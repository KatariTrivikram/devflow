import React, { useState } from 'react';
import { SPRINT_STATUS } from '../../constants/projectEnums';
import ErrorState from '../common/ErrorState';
import { toInputDateFormat } from '../../utils/date';

const SprintModalForm = ({
  projectId,
  initialData,
  onClose,
  onSubmit,
  isSubmitting,
  serverError,
}) => {
  const isEditMode = Boolean(initialData && initialData.id);

  const [formData, setFormData] = useState(() => ({
    name: initialData?.name || '',
    startDate: toInputDateFormat(initialData?.startDate) || '',
    endDate: toInputDateFormat(initialData?.endDate) || '',
    status: initialData?.status || SPRINT_STATUS.PLANNED,
  }));

  const [clientErrors, setClientErrors] = useState({});

  const validate = () => {
    const errors = {};
    const trimmedName = formData.name.trim();

    if (!trimmedName) {
      errors.name = 'Sprint name is required';
    } else if (trimmedName.length > 100) {
      errors.name = 'Sprint name cannot exceed 100 characters';
    }

    if (!formData.startDate) {
      errors.startDate = 'Start date is required';
    }

    if (!formData.endDate) {
      errors.endDate = 'End date is required';
    } else if (formData.startDate && formData.endDate < formData.startDate) {
      errors.endDate = 'Sprint end date cannot be before start date';
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    if (clientErrors[name]) {
      setClientErrors((prev) => ({
        ...prev,
        [name]: null,
      }));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    if (!validate()) return;

    const payload = {
      name: formData.name.trim(),
      startDate: `${formData.startDate}T00:00:00Z`,
      endDate: `${formData.endDate}T23:59:59Z`,
      status: formData.status,
    };

    if (!isEditMode) {
      payload.projectId = projectId;
    }

    onSubmit(payload);
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !isSubmitting) {
      onClose();
    }
  };

  const title = isEditMode ? 'Edit Sprint' : 'Create New Sprint';
  const submitLabel = isSubmitting
    ? isEditMode
      ? 'Saving...'
      : 'Creating...'
    : isEditMode
    ? 'Save Changes'
    : 'Create Sprint';

  return (
    <div
      className="df-modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="df-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="df-sprint-modal-title"
      >
        <div className="df-modal-header">
          <h2 id="df-sprint-modal-title" className="df-modal-title">
            {title}
          </h2>
          <button
            type="button"
            className="df-modal-close"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Close modal"
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

        <form onSubmit={handleSubmit} noValidate>
          <div className="df-modal-body">
            {serverError && (
              <ErrorState
                compact
                title={serverError.status === 400 ? 'Validation Error' : 'Unable to save sprint'}
                message={serverError.message}
                errors={serverError.errors}
              />
            )}

            <div className="df-form-group">
              <label htmlFor="sprint-name" className="df-label">
                Sprint Name <span aria-hidden="true">*</span>
              </label>
              <input
                id="sprint-name"
                name="name"
                type="text"
                className={`df-input ${
                  clientErrors.name || serverError?.errors?.name ? 'df-input-error' : ''
                }`}
                placeholder="e.g., Sprint 1, Core Platform Hardening"
                value={formData.name}
                onChange={handleChange}
                disabled={isSubmitting}
                maxLength={100}
                autoFocus
              />
              {(clientErrors.name || serverError?.errors?.name) && (
                <span className="df-field-error" role="alert">
                  {clientErrors.name || serverError?.errors?.name}
                </span>
              )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="df-form-group">
                <label htmlFor="sprint-start-date" className="df-label">
                  Start Date <span aria-hidden="true">*</span>
                </label>
                <input
                  id="sprint-start-date"
                  name="startDate"
                  type="date"
                  className={`df-input ${
                    clientErrors.startDate || serverError?.errors?.startDate ? 'df-input-error' : ''
                  }`}
                  value={formData.startDate}
                  onChange={handleChange}
                  disabled={isSubmitting}
                />
                {(clientErrors.startDate || serverError?.errors?.startDate) && (
                  <span className="df-field-error" role="alert">
                    {clientErrors.startDate || serverError?.errors?.startDate}
                  </span>
                )}
              </div>

              <div className="df-form-group">
                <label htmlFor="sprint-end-date" className="df-label">
                  End Date <span aria-hidden="true">*</span>
                </label>
                <input
                  id="sprint-end-date"
                  name="endDate"
                  type="date"
                  className={`df-input ${
                    clientErrors.endDate || serverError?.errors?.endDate ? 'df-input-error' : ''
                  }`}
                  value={formData.endDate}
                  onChange={handleChange}
                  disabled={isSubmitting}
                />
                {(clientErrors.endDate || serverError?.errors?.endDate) && (
                  <span className="df-field-error" role="alert">
                    {clientErrors.endDate || serverError?.errors?.endDate}
                  </span>
                )}
              </div>
            </div>

            <div className="df-form-group">
              <label htmlFor="sprint-status" className="df-label">
                Status
              </label>
              <select
                id="sprint-status"
                name="status"
                className="df-select"
                value={formData.status}
                onChange={handleChange}
                disabled={isSubmitting}
              >
                <option value={SPRINT_STATUS.PLANNED}>Planned</option>
                <option value={SPRINT_STATUS.ACTIVE}>Active</option>
                <option value={SPRINT_STATUS.COMPLETED}>Completed</option>
              </select>
            </div>
          </div>

          <div className="df-modal-footer">
            <button
              type="button"
              className="df-btn df-btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="df-btn df-btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting && (
                <span
                  className="df-spinner"
                  style={{ width: '14px', height: '14px', marginRight: '6px' }}
                  aria-hidden="true"
                />
              )}
              {submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

/**
 * Modal dialog for creating and editing DevFlow sprints.
 */
export const SprintModal = ({
  isOpen,
  onClose,
  onSubmit,
  projectId,
  initialData = null,
  isSubmitting = false,
  serverError = null,
}) => {
  if (!isOpen) return null;

  const modalKey = initialData?.id || `new-sprint-${projectId}`;

  return (
    <SprintModalForm
      key={modalKey}
      projectId={projectId}
      initialData={initialData}
      onClose={onClose}
      onSubmit={onSubmit}
      isSubmitting={isSubmitting}
      serverError={serverError}
    />
  );
};

export default SprintModal;
