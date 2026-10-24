import React, { useState } from 'react';

const MAX_COMMENT_LENGTH = 2000;

/**
 * CommentForm Component
 * Reusable form for composing new comments or editing existing comments.
 */
const CommentForm = ({
  initialContent = '',
  onSubmit,
  onCancel,
  isSubmitting = false,
  isEditing = false,
  placeholder = 'Add a comment...',
}) => {
  const [content, setContent] = useState(initialContent);
  const [clientError, setClientError] = useState(null);
  const [prevInitialContent, setPrevInitialContent] = useState(initialContent);

  if (initialContent !== prevInitialContent) {
    setPrevInitialContent(initialContent);
    setContent(initialContent);
    setClientError(null);
  }

  const handleChange = (e) => {
    setContent(e.target.value);
    if (clientError) {
      setClientError(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    const trimmed = content.trim();
    if (!trimmed) {
      setClientError('Comment cannot be empty.');
      return;
    }

    if (trimmed.length > MAX_COMMENT_LENGTH) {
      setClientError(`Comment cannot exceed ${MAX_COMMENT_LENGTH} characters.`);
      return;
    }

    try {
      await onSubmit(trimmed);
      if (!isEditing) {
        setContent('');
      }
    } catch {
      // Error is handled by parent / error state
    }
  };

  const isOverLimit = content.length > MAX_COMMENT_LENGTH;

  return (
    <form className="df-comment-form" onSubmit={handleSubmit} noValidate>
      <div style={{ position: 'relative' }}>
        <textarea
          className="df-comment-textarea"
          rows={3}
          value={content}
          onChange={handleChange}
          placeholder={placeholder}
          disabled={isSubmitting}
          aria-label={isEditing ? 'Edit comment' : 'Add comment'}
          aria-invalid={Boolean(clientError)}
        />
      </div>

      {clientError && (
        <p
          className="df-error-message"
          role="alert"
          style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}
        >
          {clientError}
        </p>
      )}

      <div className="df-comment-form-footer">
        <span
          className="df-comment-char-count"
          style={{ color: isOverLimit ? 'var(--df-error-text)' : undefined }}
        >
          {content.length} / {MAX_COMMENT_LENGTH}
        </span>

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {isEditing && onCancel && (
            <button
              type="button"
              className="df-btn df-btn-secondary df-btn-sm"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              Cancel
            </button>
          )}

          <button
            type="submit"
            className="df-btn df-btn-primary df-btn-sm"
            disabled={isSubmitting || !content.trim() || isOverLimit}
          >
            {isSubmitting
              ? isEditing
                ? 'Saving...'
                : 'Posting...'
              : isEditing
              ? 'Save Changes'
              : 'Add Comment'}
          </button>
        </div>
      </div>
    </form>
  );
};

export default CommentForm;
