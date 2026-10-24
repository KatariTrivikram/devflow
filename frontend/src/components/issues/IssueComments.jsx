import React, { useState, useEffect, useCallback } from 'react';
import CommentForm from './CommentForm';
import CommentItem from './CommentItem';
import ConfirmModal from '../common/ConfirmModal';
import LoadingState from '../common/LoadingState';
import ErrorState from '../common/ErrorState';
import commentService from '../../services/commentService';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLES } from '../../constants/roles';

const COMMENTS_PAGE_SIZE = 20;

/**
 * IssueComments Component
 * Manages the comments section for an issue, including creation, inline editing,
 * deletion with confirmation, and pagination.
 */
const IssueComments = ({ issueId, project, members = [] }) => {
  const { user } = useAuth();

  const [comments, setComments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Pagination
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: COMMENTS_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  // Create form state
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Edit comment state
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [isUpdating, setIsUpdating] = useState(false);

  // Delete modal state
  const [commentToDelete, setCommentToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Refresh trigger
  const [refreshKey, setRefreshKey] = useState(0);
  const handleRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  // Load paginated comments
  useEffect(() => {
    let ignore = false;

    async function loadComments() {
      setIsLoading(true);
      setFetchError(null);

      try {
        const data = await commentService.getCommentsByIssue(issueId, {
          page,
          size: COMMENTS_PAGE_SIZE,
          sort: 'createdAt,asc',
        });

        if (!ignore) {
          setComments(data.content || []);
          setPagination({
            page: data.page ?? page,
            size: data.size ?? COMMENTS_PAGE_SIZE,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
            first: data.first ?? true,
            last: data.last ?? true,
          });
        }
      } catch (err) {
        if (!ignore) {
          setFetchError(
            err.normalized || {
              status: 500,
              message: 'Failed to load comments for this issue.',
            }
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadComments();

    return () => {
      ignore = true;
    };
  }, [issueId, page, refreshKey]);

  // Authorization check for comment modification
  const canModifyComment = useCallback(
    (comment) => {
      if (!user) return false;
      if (user.role === USER_ROLES.ADMIN) return true;
      if (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById) return true;
      if (comment.userId && comment.userId === user.id) return true;
      return false;
    },
    [user, project]
  );

  // Lookup author info from project members
  const getAuthor = (userId) => {
    const member = members.find((m) => m.userId === userId || m.user?.id === userId);
    return member?.user || { id: userId };
  };

  // Create comment handler
  const handleCreateComment = async (content) => {
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const created = await commentService.createComment({
        issueId,
        userId: user?.id,
        content,
      });

      setComments((prev) => [...prev, created]);
      setPagination((prev) => ({
        ...prev,
        totalElements: prev.totalElements + 1,
      }));
      setFeedback({ type: 'success', message: 'Comment added successfully.' });
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to post comment. Please try again.',
      });
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  // Edit comment handler
  const handleUpdateComment = async (commentId, newContent) => {
    setIsUpdating(true);
    setFeedback(null);

    try {
      const updated = await commentService.updateComment(commentId, { content: newContent });
      setComments((prev) =>
        prev.map((c) => (c.id === commentId ? { ...c, ...updated } : c))
      );
      setEditingCommentId(null);
      setFeedback({ type: 'success', message: 'Comment updated successfully.' });
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to update comment.',
      });
      throw err;
    } finally {
      setIsUpdating(false);
    }
  };

  // Delete comment handler
  const handleDeleteComment = async () => {
    if (!commentToDelete) return;
    setIsDeleting(true);
    setFeedback(null);

    try {
      await commentService.deleteComment(commentToDelete.id);
      setComments((prev) => prev.filter((c) => c.id !== commentToDelete.id));
      setPagination((prev) => ({
        ...prev,
        totalElements: Math.max(0, prev.totalElements - 1),
      }));
      setCommentToDelete(null);
      setFeedback({ type: 'success', message: 'Comment deleted successfully.' });
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to delete comment.',
      });
      setCommentToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <section className="df-section-card" aria-label="Issue comments section">
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Comments ({pagination.totalElements})
          </h2>
          <p className="df-section-subtitle">
            Collaborate, share updates, and discuss implementation details.
          </p>
        </div>
      </div>

      {/* Feedback banner */}
      {feedback && (
        <div
          role="status"
          style={{
            padding: '0.75rem 1rem',
            marginBottom: '1rem',
            borderRadius: 'var(--df-radius)',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: feedback.type === 'error' ? 'var(--df-error-bg)' : '#ecfdf5',
            color: feedback.type === 'error' ? 'var(--df-error-text)' : '#065f46',
            border: `1px solid ${feedback.type === 'error' ? 'var(--df-error-border)' : '#a7f3d0'}`,
          }}
        >
          <span>{feedback.message}</span>
          <button
            type="button"
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
            aria-label="Dismiss feedback"
          >
            ✕
          </button>
        </div>
      )}

      {/* Add new comment form */}
      <CommentForm
        onSubmit={handleCreateComment}
        isSubmitting={isSubmitting}
        placeholder="Leave a comment on this issue..."
      />

      {/* Comments list or loading / error / empty states */}
      {isLoading ? (
        <LoadingState message="Loading comments..." />
      ) : fetchError ? (
        <ErrorState
          title="Unable to load comments"
          message={fetchError.message}
          onRetry={handleRefresh}
        />
      ) : comments.length === 0 ? (
        <div
          className="df-empty-container"
          style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--df-text-subtle)' }}
        >
          <p style={{ fontStyle: 'italic', fontSize: '0.875rem' }}>
            No comments yet. Be the first to start the discussion!
          </p>
        </div>
      ) : (
        <div className="df-comment-list">
          {comments.map((comment) =>
            editingCommentId === comment.id ? (
              <div
                key={comment.id}
                className="df-comment-card"
                style={{ borderColor: 'var(--df-border-focus)' }}
              >
                <CommentForm
                  initialContent={comment.content}
                  isEditing
                  isSubmitting={isUpdating}
                  onSubmit={(newContent) => handleUpdateComment(comment.id, newContent)}
                  onCancel={() => setEditingCommentId(null)}
                />
              </div>
            ) : (
              <CommentItem
                key={comment.id}
                comment={comment}
                author={getAuthor(comment.userId)}
                canModify={canModifyComment(comment)}
                onEdit={() => setEditingCommentId(comment.id)}
                onDelete={() => setCommentToDelete(comment)}
              />
            )
          )}
        </div>
      )}

      {/* Comments Pagination */}
      {pagination.totalPages > 1 && (
        <div className="df-pagination" style={{ marginTop: '1.25rem' }}>
          <button
            type="button"
            className="df-pagination-btn"
            disabled={pagination.first || isLoading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            aria-label="Previous page of comments"
          >
            Previous
          </button>
          <span className="df-pagination-info">
            Page {pagination.page + 1} of {pagination.totalPages}
          </span>
          <button
            type="button"
            className="df-pagination-btn"
            disabled={pagination.last || isLoading}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Next page of comments"
          >
            Next
          </button>
        </div>
      )}

      {/* Delete Comment Confirm Modal */}
      <ConfirmModal
        isOpen={Boolean(commentToDelete)}
        title="Delete Comment"
        message="Are you sure you want to delete this comment? This action cannot be undone."
        confirmLabel="Delete Comment"
        isDanger
        isConfirming={isDeleting}
        onConfirm={handleDeleteComment}
        onClose={() => setCommentToDelete(null)}
      />
    </section>
  );
};

export default IssueComments;
