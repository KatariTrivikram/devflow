import React from 'react';

/**
 * Reusable modal for confirming sensitive or destructive actions.
 *
 * @param {Object} props
 * @param {boolean} props.isOpen
 * @param {string} props.title
 * @param {string} props.message
 * @param {string} [props.confirmLabel='Confirm']
 * @param {string} [props.cancelLabel='Cancel']
 * @param {boolean} [props.isConfirming=false]
 * @param {boolean} [props.isDanger=true]
 * @param {Function} props.onConfirm
 * @param {Function} props.onClose
 */
export const ConfirmModal = ({
  isOpen,
  title = 'Confirm Action',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isConfirming = false,
  isDanger = true,
  onConfirm,
  onClose,
}) => {
  if (!isOpen) return null;

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !isConfirming) {
      onClose();
    }
  };

  return (
    <div
      className="df-modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="df-modal"
        style={{ maxWidth: '440px' }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="df-confirm-modal-title"
      >
        <div className="df-modal-header">
          <h2 id="df-confirm-modal-title" className="df-modal-title">
            {title}
          </h2>
          <button
            type="button"
            className="df-modal-close"
            onClick={onClose}
            disabled={isConfirming}
            aria-label="Close dialog"
          >
            <svg
              style={{ width: '20px', height: '20px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div className="df-modal-body">
          <p style={{ margin: 0, color: 'var(--df-text-muted)', fontSize: '0.875rem', lineHeight: 1.5 }}>
            {message}
          </p>
        </div>

        <div className="df-modal-footer">
          <button
            type="button"
            className="df-btn df-btn-secondary"
            onClick={onClose}
            disabled={isConfirming}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`df-btn ${isDanger ? 'df-btn-danger' : 'df-btn-primary'}`}
            onClick={onConfirm}
            disabled={isConfirming}
          >
            {isConfirming && (
              <span
                className="df-spinner"
                style={{ width: '14px', height: '14px', marginRight: '6px' }}
                aria-hidden="true"
              />
            )}
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
