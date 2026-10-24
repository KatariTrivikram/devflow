import React from 'react';

/**
 * Reusable ErrorState primitive displaying sanitized error alerts with optional field validation lists and retry action.
 */
export const ErrorState = ({
  title = 'Something went wrong',
  message,
  errors = null,
  onRetry = null,
  retryLabel = 'Try again',
  compact = false,
}) => {
  return (
    <div
      role="alert"
      className={`df-error-container ${compact ? 'df-error-compact' : ''}`}
    >
      <div className="df-error-header">
        <svg
          className="df-error-icon"
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            fillRule="evenodd"
            d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"
            clipRule="evenodd"
          />
        </svg>
        <div className="df-error-content">
          <h4 className="df-error-title">{title}</h4>
          {message && <p className="df-error-message">{message}</p>}
          {errors && typeof errors === 'object' && Object.keys(errors).length > 0 && (
            <ul className="df-error-list">
              {Object.entries(errors).map(([field, errorMsg]) => (
                <li key={field}>
                  <strong>{field}:</strong> {errorMsg}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="df-btn df-btn-secondary df-btn-sm"
        >
          {retryLabel}
        </button>
      )}
    </div>
  );
};

export default ErrorState;
