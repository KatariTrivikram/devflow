import React, { useState } from 'react';
import ErrorState from '../common/ErrorState';
import {
  ISSUE_TYPE,
  ISSUE_TYPE_LABELS,
  ISSUE_PRIORITY,
  ISSUE_PRIORITY_LABELS,
  ISSUE_STATUS,
  ISSUE_STATUS_LABELS,
} from '../../constants/issueEnums';

const IssueModalForm = ({
  projectId,
  projectName = null,
  project = null,
  initialData,
  sprints,
  members,
  onClose,
  onSubmit,
  isSubmitting,
  serverError,
}) => {
  const isEditMode = Boolean(initialData && initialData.id);

  const [formData, setFormData] = useState(() => ({
    title: initialData?.title || '',
    description: initialData?.description || '',
    type: initialData?.type || ISSUE_TYPE.TASK,
    priority: initialData?.priority || ISSUE_PRIORITY.MEDIUM,
    status: initialData?.status || ISSUE_STATUS.TODO,
    sprintId: initialData?.sprintId || '',
    assignedToId: initialData?.assignedToId || '',
  }));

  const [clientErrors, setClientErrors] = useState({});

  const validate = () => {
    const errors = {};
    const trimmedTitle = formData.title.trim();

    if (!trimmedTitle) {
      errors.title = 'Issue title is required';
    } else if (trimmedTitle.length > 200) {
      errors.title = 'Issue title cannot exceed 200 characters';
    }

    if (!formData.type) {
      errors.type = 'Issue type is required';
    }

    if (!formData.priority) {
      errors.priority = 'Issue priority is required';
    }

    if (!formData.status) {
      errors.status = 'Issue status is required';
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
      title: formData.title.trim(),
      description: formData.description?.trim() || null,
      type: formData.type,
      priority: formData.priority,
      status: formData.status,
      sprintId: formData.sprintId ? formData.sprintId : null,
      assignedToId: formData.assignedToId ? formData.assignedToId : null,
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

  const title = isEditMode ? 'Edit Issue' : 'Create New Issue';
  const submitLabel = isSubmitting
    ? isEditMode
      ? 'Saving...'
      : 'Creating...'
    : isEditMode
    ? 'Save Changes'
    : 'Create Issue';

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
        aria-labelledby="df-issue-modal-title"
        style={{ maxWidth: '620px' }}
      >
        <div className="df-modal-header">
          <h2 id="df-issue-modal-title" className="df-modal-title">
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
            {(projectName || project?.name) && (
              <div
                className="df-modal-project-context"
                data-testid="modal-project-context"
                style={{
                  padding: '0.5rem 0.75rem',
                  marginBottom: '1rem',
                  backgroundColor: 'var(--df-bg-subtle)',
                  borderRadius: 'var(--df-radius)',
                  border: '1px solid var(--df-border)',
                  fontSize: '0.8125rem',
                  color: 'var(--df-text-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <span style={{ fontWeight: 600, color: 'var(--df-text-main)' }}>Project:</span>
                <span>{projectName || project?.name}</span>
              </div>
            )}

            {serverError && (
              <ErrorState
                compact
                title={serverError.status === 400 ? 'Validation Error' : 'Unable to save issue'}
                message={serverError.message}
                errors={serverError.errors}
              />
            )}

            {/* Issue Title */}
            <div className="df-form-group">
              <label htmlFor="issue-title" className="df-label">
                Title <span aria-hidden="true">*</span>
              </label>
              <input
                id="issue-title"
                name="title"
                type="text"
                className={`df-input ${
                  clientErrors.title || serverError?.errors?.title ? 'df-input-error' : ''
                }`}
                placeholder="e.g., Implement authentication middleware"
                value={formData.title}
                onChange={handleChange}
                disabled={isSubmitting}
                maxLength={200}
                autoFocus
              />
              {(clientErrors.title || serverError?.errors?.title) && (
                <span className="df-field-error" role="alert">
                  {clientErrors.title || serverError?.errors?.title}
                </span>
              )}
            </div>

            {/* Type & Priority Row */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="df-form-group">
                <label htmlFor="issue-type" className="df-label">
                  Issue Type <span aria-hidden="true">*</span>
                </label>
                <select
                  id="issue-type"
                  name="type"
                  className={`df-select ${clientErrors.type ? 'df-select-error' : ''}`}
                  value={formData.type}
                  onChange={handleChange}
                  disabled={isSubmitting}
                >
                  <option value={ISSUE_TYPE.TASK}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.TASK]}</option>
                  <option value={ISSUE_TYPE.FEATURE}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.FEATURE]}</option>
                  <option value={ISSUE_TYPE.BUG}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.BUG]}</option>
                  <option value={ISSUE_TYPE.IMPROVEMENT}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.IMPROVEMENT]}</option>
                </select>
                {clientErrors.type && (
                  <span className="df-field-error" role="alert">
                    {clientErrors.type}
                  </span>
                )}
              </div>

              <div className="df-form-group">
                <label htmlFor="issue-priority" className="df-label">
                  Priority <span aria-hidden="true">*</span>
                </label>
                <select
                  id="issue-priority"
                  name="priority"
                  className={`df-select ${clientErrors.priority ? 'df-select-error' : ''}`}
                  value={formData.priority}
                  onChange={handleChange}
                  disabled={isSubmitting}
                >
                  <option value={ISSUE_PRIORITY.LOW}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.LOW]}</option>
                  <option value={ISSUE_PRIORITY.MEDIUM}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.MEDIUM]}</option>
                  <option value={ISSUE_PRIORITY.HIGH}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.HIGH]}</option>
                  <option value={ISSUE_PRIORITY.CRITICAL}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.CRITICAL]}</option>
                </select>
                {clientErrors.priority && (
                  <span className="df-field-error" role="alert">
                    {clientErrors.priority}
                  </span>
                )}
              </div>
            </div>

            {/* Status & Sprint Row */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="df-form-group">
                <label htmlFor="issue-status" className="df-label">
                  Status <span aria-hidden="true">*</span>
                </label>
                <select
                  id="issue-status"
                  name="status"
                  className={`df-select ${clientErrors.status ? 'df-select-error' : ''}`}
                  value={formData.status}
                  onChange={handleChange}
                  disabled={isSubmitting}
                >
                  <option value={ISSUE_STATUS.TODO}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO]}</option>
                  <option value={ISSUE_STATUS.IN_PROGRESS}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.IN_PROGRESS]}</option>
                  <option value={ISSUE_STATUS.CODE_REVIEW}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.CODE_REVIEW]}</option>
                  <option value={ISSUE_STATUS.DONE}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.DONE]}</option>
                </select>
                {clientErrors.status && (
                  <span className="df-field-error" role="alert">
                    {clientErrors.status}
                  </span>
                )}
              </div>

              <div className="df-form-group">
                <label htmlFor="issue-sprint" className="df-label">
                  Sprint <span className="df-label-hint">(Optional)</span>
                </label>
                <select
                  id="issue-sprint"
                  name="sprintId"
                  className="df-select"
                  value={formData.sprintId}
                  onChange={handleChange}
                  disabled={isSubmitting}
                >
                  <option value="">None (Backlog)</option>
                  {sprints.map((sprint) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name} ({sprint.status})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Assignee Row */}
            <div className="df-form-group">
              <label htmlFor="issue-assignee" className="df-label">
                Assignee <span className="df-label-hint">(Optional)</span>
              </label>
              <select
                id="issue-assignee"
                name="assignedToId"
                className="df-select"
                value={formData.assignedToId}
                onChange={handleChange}
                disabled={isSubmitting}
              >
                <option value="">Unassigned</option>
                {members.map((member) => {
                  const uid = member.userId || member.user?.id;
                  const name = member.user?.name || member.userName || `User ${uid?.substring(0, 8)}`;
                  const email = member.user?.email || member.userEmail || '';
                  return (
                    <option key={uid} value={uid}>
                      {name} {email ? `(${email})` : ''}
                    </option>
                  );
                })}
              </select>
            </div>

            {/* Description */}
            <div className="df-form-group">
              <label htmlFor="issue-description" className="df-label">
                Description <span className="df-label-hint">(Optional)</span>
              </label>
              <textarea
                id="issue-description"
                name="description"
                rows={4}
                className="df-input"
                style={{ resize: 'vertical' }}
                placeholder="Detailed context, requirements, or acceptance criteria..."
                value={formData.description}
                onChange={handleChange}
                disabled={isSubmitting}
              />
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
 * Modal dialog for creating and updating issues.
 *
 * @param {Object} props
 * @param {boolean} props.isOpen - Controls modal visibility
 * @param {Function} props.onClose - Callback to close modal
 * @param {Function} props.onSubmit - Callback receiving issue form payload
 * @param {string} props.projectId - Project UUID (cannot be changed by user)
 * @param {Object} [props.initialData] - Existing issue object when editing, null when creating
 * @param {Array} [props.sprints] - Array of project sprints for selection
 * @param {Array} [props.members] - Array of project members / assignees for selection
 * @param {boolean} [props.isSubmitting=false] - Submission pending state
 * @param {Object} [props.serverError] - Normalized backend error response
 */
export const IssueModal = ({
  isOpen,
  onClose,
  onSubmit,
  projectId,
  projectName = null,
  project = null,
  initialData = null,
  sprints = [],
  members = [],
  isSubmitting = false,
  serverError = null,
}) => {
  if (!isOpen) return null;

  const modalKey = initialData?.id || `new-issue-${projectId}`;

  return (
    <IssueModalForm
      key={modalKey}
      projectId={projectId}
      projectName={projectName || project?.name}
      project={project}
      initialData={initialData}
      sprints={sprints}
      members={members}
      onClose={onClose}
      onSubmit={onSubmit}
      isSubmitting={isSubmitting}
      serverError={serverError}
    />
  );
};

export default IssueModal;
