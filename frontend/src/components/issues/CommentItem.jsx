import React from 'react';
import { formatDateTime } from '../../utils/date';

/**
 * CommentItem Component
 * Displays a single comment with author info, timestamps, and edit/delete actions.
 */
const CommentItem = ({
  comment,
  author,
  canModify = false,
  onEdit,
  onDelete,
  isDeleting = false,
}) => {
  const authorName = author?.name || author?.email || `User ${comment.userId ? comment.userId.substring(0, 8) : 'Unknown'}`;
  const authorInitials = authorName
    .split(' ')
    .map((part) => part[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase() || 'U';

  const isEdited = Boolean(
    comment.updatedAt &&
    comment.createdAt &&
    new Date(comment.updatedAt).getTime() > new Date(comment.createdAt).getTime()
  );

  return (
    <article
      className="df-comment-card"
      data-testid={`comment-item-${comment.id}`}
      aria-label={`Comment by ${authorName}`}
    >
      <header className="df-comment-header">
        <div className="df-comment-author">
          <div className="df-avatar-sm" aria-hidden="true">
            {authorInitials}
          </div>
          <div>
            <div className="df-comment-author-name">{authorName}</div>
            <div className="df-comment-date">
              <time dateTime={comment.createdAt}>
                {formatDateTime(comment.createdAt)}
              </time>
              {isEdited && (
                <span className="df-comment-tag-edited" title={`Edited on ${formatDateTime(comment.updatedAt)}`}>
                  (edited)
                </span>
              )}
            </div>
          </div>
        </div>

        {canModify && (
          <div className="df-comment-actions">
            {onEdit && (
              <button
                type="button"
                className="df-btn df-btn-secondary df-btn-sm"
                onClick={() => onEdit(comment)}
                disabled={isDeleting}
                aria-label={`Edit comment by ${authorName}`}
                style={{ padding: '0.1875rem 0.5rem', fontSize: '0.75rem', minHeight: '26px' }}
              >
                Edit
              </button>
            )}

            {onDelete && (
              <button
                type="button"
                className="df-btn df-btn-danger-outline df-btn-sm"
                onClick={() => onDelete(comment)}
                disabled={isDeleting}
                aria-label={`Delete comment by ${authorName}`}
                style={{ padding: '0.1875rem 0.5rem', fontSize: '0.75rem', minHeight: '26px' }}
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </button>
            )}
          </div>
        )}
      </header>

      <div className="df-comment-body">
        {comment.content}
      </div>
    </article>
  );
};

export default CommentItem;
