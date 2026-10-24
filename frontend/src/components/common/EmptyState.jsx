import React from 'react';

/**
 * Reusable EmptyState primitive.
 */
export const EmptyState = ({
  title = 'No items found',
  description = 'There are currently no items to display.',
  actionLabel = null,
  onAction = null,
  icon = null,
}) => {
  return (
    <div className="df-empty-container">
      <div className="df-empty-icon" aria-hidden="true">
        {icon || (
          <svg
            className="df-icon-lg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
            />
          </svg>
        )}
      </div>
      <h3 className="df-empty-title">{title}</h3>
      {description && <p className="df-empty-description">{description}</p>}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="df-btn df-btn-primary"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};

export default EmptyState;
