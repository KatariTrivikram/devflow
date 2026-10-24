import React from 'react';

/**
 * Reusable LoadingState primitive.
 */
export const LoadingState = ({ message = 'Loading...', fullPage = false }) => {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`df-loading-container ${fullPage ? 'df-loading-fullpage' : ''}`}
    >
      <div className="df-spinner" aria-hidden="true" />
      <span className="df-loading-text">{message}</span>
    </div>
  );
};

export default LoadingState;
