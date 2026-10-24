import React, { useState } from 'react';
import { PROJECT_STATUS } from '../../constants/projectEnums';
import ErrorState from '../common/ErrorState';

const ProjectModalForm = ({
  initialData,
  onClose,
  onSubmit,
  isSubmitting,
  serverError,
}) => {
  const isEditMode = Boolean(initialData && initialData.id);

  const [formData, setFormData] = useState(() => ({
    name: initialData?.name || '',
    description: initialData?.description || '',
    status: initialData?.status || PROJECT_STATUS.ACTIVE,
  }));

  const [clientErrors, setClientErrors] = useState({});

  const validate = () => {
    const errors = {};
    const trimmedName = formData.name.trim();

    if (!trimmedName) {
      errors.name = 'Project name is required';
    } else if (trimmedName.length > 100) {
      errors.name = 'Project name cannot exceed 100 characters';
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

    onSubmit({
      name: formData.name.trim(),
      description: formData.description.trim() || null,
      status: formData.status,
    });
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !isSubmitting) {
      onClose();
    }
  };

  const title = isEditMode ? 'Edit Project' : 'Create New Project';
  const submitLabel = isSubmitting
    ? isEditMode
      ? 'Saving...'
      : 'Creating...'
    : isEditMode
    ? 'Save Changes'
    : 'Create Project';

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
        aria-labelledby="df-project-modal-title"
      >
        <div className="df-modal-header">
          <h2 id="df-project-modal-title" className="df-modal-title">
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
                title={serverError.status === 409 ? 'Project Conflict' : 'Unable to save project'}
                message={serverError.message}
                errors={serverError.errors}
              />
            )}

            <div className="df-form-group">
              <label htmlFor="project-name" className="df-label">
                Project Name <span aria-hidden="true">*</span>
              </label>
              <input
                id="project-name"
                name="name"
                type="text"
                className={`df-input ${
                  clientErrors.name || serverError?.errors?.name ? 'df-input-error' : ''
                }`}
                placeholder="e.g., Core Engine, Payment Gateway"
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

            <div className="df-form-group">
              <label htmlFor="project-description" className="df-label">
                Description <span className="df-label-hint">(optional)</span>
              </label>
              <textarea
                id="project-description"
                name="description"
                rows={3}
                className="df-input"
                style={{ resize: 'vertical' }}
                placeholder="Briefly describe the goals and scope of this project..."
                value={formData.description}
                onChange={handleChange}
                disabled={isSubmitting}
              />
            </div>

            <div className="df-form-group">
              <label htmlFor="project-status" className="df-label">
                Status
              </label>
              <select
                id="project-status"
                name="status"
                className="df-select"
                value={formData.status}
                onChange={handleChange}
                disabled={isSubmitting}
              >
                <option value={PROJECT_STATUS.ACTIVE}>Active</option>
                <option value={PROJECT_STATUS.ARCHIVED}>Archived</option>
                <option value={PROJECT_STATUS.COMPLETED}>Completed</option>
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
 * Modal dialog for creating and editing DevFlow projects.
 */
export const ProjectModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData = null,
  isSubmitting = false,
  serverError = null,
}) => {
  if (!isOpen) return null;

  const modalKey = initialData?.id || 'new-project';

  return (
    <ProjectModalForm
      key={modalKey}
      initialData={initialData}
      onClose={onClose}
      onSubmit={onSubmit}
      isSubmitting={isSubmitting}
      serverError={serverError}
    />
  );
};

export default ProjectModal;
