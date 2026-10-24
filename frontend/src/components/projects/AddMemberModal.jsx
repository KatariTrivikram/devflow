import React, { useState } from 'react';
import userService from '../../services/userService';
import projectMemberService from '../../services/projectMemberService';
import ErrorState from '../common/ErrorState';
import { USER_ROLE_LABELS, USER_ROLES } from '../../constants/roles';

function getRoleBadgeClass(role) {
  switch (role) {
    case USER_ROLES.ADMIN:
      return 'df-role-badge-admin';
    case USER_ROLES.PROJECT_MANAGER:
      return 'df-role-badge-pm';
    case USER_ROLES.DEVELOPER:
    default:
      return 'df-role-badge-dev';
  }
}

function getInitials(name) {
  if (!name) return 'U';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const AddMemberModalForm = ({
  projectId,
  existingMemberUserIds = [],
  onClose,
  onMemberAdded,
}) => {
  const [emailInput, setEmailInput] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [lookupError, setLookupError] = useState(null);
  const [searchedUser, setSearchedUser] = useState(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const handleSearch = async (e) => {
    e.preventDefault();
    const email = emailInput.trim();
    if (!email) {
      setLookupError({ message: 'Please enter a valid user email address.' });
      return;
    }

    setIsSearching(true);
    setLookupError(null);
    setSearchedUser(null);
    setSubmitError(null);

    try {
      const user = await userService.getUserByEmail(email);
      setSearchedUser(user);
    } catch (err) {
      if (err.normalized?.status === 404) {
        setLookupError({ message: `No registered user found with email "${email}".` });
      } else {
        setLookupError(err.normalized || {
          message: 'Failed to look up user. Please verify connection.',
        });
      }
    } finally {
      setIsSearching(false);
    }
  };

  const isAlreadyMember = Boolean(
    searchedUser && existingMemberUserIds.includes(searchedUser.id)
  );

  const handleAddMember = async () => {
    if (!searchedUser || isAlreadyMember || isSubmitting) return;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const memberRecord = await projectMemberService.addMember({
        projectId,
        userId: searchedUser.id,
      });

      onMemberAdded({
        ...memberRecord,
        user: searchedUser,
      });
      onClose();
    } catch (err) {
      setSubmitError(err.normalized || {
        status: 500,
        message: 'Failed to add user to project team.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !isSubmitting) {
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
        role="dialog"
        aria-modal="true"
        aria-labelledby="df-add-member-title"
      >
        <div className="df-modal-header">
          <h2 id="df-add-member-title" className="df-modal-title">
            Add Team Member
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

        <div className="df-modal-body">
          {submitError && (
            <ErrorState
              compact
              title={
                submitError.status === 409
                  ? 'Duplicate Membership'
                  : submitError.status === 403
                  ? 'Access Denied'
                  : 'Unable to Add Member'
              }
              message={submitError.message}
            />
          )}

          <form onSubmit={handleSearch}>
            <div className="df-form-group">
              <label htmlFor="member-email-search" className="df-label">
                User Email Address <span aria-hidden="true">*</span>
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  id="member-email-search"
                  type="email"
                  className="df-input"
                  placeholder="e.g., developer@example.com"
                  value={emailInput}
                  onChange={(e) => setEmailInput(e.target.value)}
                  disabled={isSearching || isSubmitting}
                  autoFocus
                />
                <button
                  type="submit"
                  className="df-btn df-btn-secondary"
                  disabled={isSearching || isSubmitting || !emailInput.trim()}
                  style={{ whiteSpace: 'nowrap' }}
                >
                  {isSearching ? 'Looking up...' : 'Find User'}
                </button>
              </div>
              <p className="df-lookup-hint">
                Look up an existing DevFlow user account by their registered email address.
              </p>
            </div>
          </form>

          {lookupError && (
            <div
              role="alert"
              style={{
                marginTop: '0.75rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--df-error-bg)',
                border: '1px solid var(--df-error-border)',
                color: 'var(--df-error-text)',
                borderRadius: 'var(--df-radius)',
                fontSize: '0.8125rem',
              }}
            >
              {lookupError.message}
            </div>
          )}

          {searchedUser && (
            <div className="df-lookup-card">
              <div className="df-lookup-card-info">
                <div className="df-avatar-sm" aria-hidden="true">
                  {getInitials(searchedUser.name)}
                </div>
                <div className="df-member-info">
                  <span className="df-member-name">{searchedUser.name}</span>
                  <span className="df-member-email">{searchedUser.email}</span>
                </div>
              </div>
              <span
                className={`df-status-pill ${getRoleBadgeClass(searchedUser.role)}`}
                style={{ fontSize: '0.6875rem' }}
              >
                {USER_ROLE_LABELS[searchedUser.role] || searchedUser.role}
              </span>
            </div>
          )}

          {isAlreadyMember && (
            <div className="df-lookup-warning" role="status">
              This user is already a member of this project team.
            </div>
          )}
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
            type="button"
            className="df-btn df-btn-primary"
            onClick={handleAddMember}
            disabled={!searchedUser || isAlreadyMember || isSubmitting}
            id="btn-confirm-add-member"
          >
            {isSubmitting ? 'Adding...' : 'Add to Project'}
          </button>
        </div>
      </div>
    </div>
  );
};

/**
 * Modal dialog for searching and adding team members to a project.
 */
export const AddMemberModal = ({
  isOpen,
  onClose,
  onMemberAdded,
  projectId,
  existingMemberUserIds = [],
}) => {
  if (!isOpen) return null;

  return (
    <AddMemberModalForm
      key={`add-member-${projectId}`}
      projectId={projectId}
      existingMemberUserIds={existingMemberUserIds}
      onClose={onClose}
      onMemberAdded={onMemberAdded}
    />
  );
};

export default AddMemberModal;
