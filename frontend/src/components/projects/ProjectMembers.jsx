import React, { useState, useEffect } from 'react';
import projectMemberService from '../../services/projectMemberService';
import userService from '../../services/userService';
import AddMemberModal from './AddMemberModal';
import ConfirmModal from '../common/ConfirmModal';
import LoadingState from '../common/LoadingState';
import ErrorState from '../common/ErrorState';
import EmptyState from '../common/EmptyState';
import { USER_ROLE_LABELS, USER_ROLES } from '../../constants/roles';
import { formatDate } from '../../utils/date';

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

export const ProjectMembers = ({ projectId, canManageMembers = false }) => {
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Add member modal state
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  // Remove member confirmation state
  const [memberToRemove, setMemberToRemove] = useState(null);
  const [isRemoving, setIsRemoving] = useState(false);

  // Feedback banner state
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    let ignore = false;

    async function loadMembers() {
      try {
        const memberRecords = await projectMemberService.getProjectMembers(projectId);
        if (ignore) return;

        // Populate user details for each member record
        const enrichedMembers = await Promise.all(
          memberRecords.map(async (record) => {
            try {
              const user = await userService.getUserById(record.userId);
              return { ...record, user };
            } catch {
              return {
                ...record,
                user: {
                  id: record.userId,
                  name: `User ${record.userId.substring(0, 8)}`,
                  email: 'Unavailable',
                  role: USER_ROLES.DEVELOPER,
                },
              };
            }
          })
        );

        if (!ignore) {
          setMembers(enrichedMembers);
        }
      } catch (err) {
        if (!ignore) {
          setLoadError(err.normalized || {
            status: 500,
            message: 'Failed to retrieve project members.',
          });
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadMembers();

    return () => {
      ignore = true;
    };
  }, [projectId]);

  const handleRetry = () => {
    setIsLoading(true);
    setLoadError(null);
    projectMemberService.getProjectMembers(projectId)
      .then(async (memberRecords) => {
        const enriched = await Promise.all(
          memberRecords.map(async (record) => {
            try {
              const user = await userService.getUserById(record.userId);
              return { ...record, user };
            } catch {
              return {
                ...record,
                user: {
                  id: record.userId,
                  name: `User ${record.userId.substring(0, 8)}`,
                  email: 'Unavailable',
                  role: USER_ROLES.DEVELOPER,
                },
              };
            }
          })
        );
        setMembers(enriched);
      })
      .catch((err) => {
        setLoadError(err.normalized || {
          status: 500,
          message: 'Failed to retrieve project members.',
        });
      })
      .finally(() => setIsLoading(false));
  };

  const handleMemberAdded = (newMember) => {
    setMembers((prev) => [...prev, newMember]);
    setFeedback({
      type: 'success',
      message: `${newMember.user?.name || 'Member'} was successfully added to the project team.`,
    });
  };

  const handleConfirmRemove = async () => {
    if (!memberToRemove) return;

    setIsRemoving(true);
    try {
      await projectMemberService.removeMember(projectId, memberToRemove.userId);
      const removedName = memberToRemove.user?.name || 'Member';
      setMembers((prev) => prev.filter((m) => m.userId !== memberToRemove.userId));
      setMemberToRemove(null);
      setFeedback({
        type: 'success',
        message: `${removedName} was removed from the project team.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to remove member from project.',
      });
      setMemberToRemove(null);
    } finally {
      setIsRemoving(false);
    }
  };

  const existingMemberUserIds = members.map((m) => m.userId);

  return (
    <div className="df-section-card" style={{ marginBottom: '1.5rem' }}>
      {/* Section Header */}
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Team Members
          </h2>
          <p className="df-section-subtitle">
            Collaborators with assigned roles and contribution access to this project.
          </p>
        </div>
        {canManageMembers && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={() => setIsAddModalOpen(true)}
            id="btn-add-member"
          >
            <svg
              style={{ width: '14px', height: '14px', marginRight: '6px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Add Member
          </button>
        )}
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          role="status"
          style={{
            padding: '0.75rem 1rem',
            marginBottom: '1rem',
            borderRadius: 'var(--df-radius)',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: feedback.type === 'error' ? 'var(--df-error-bg)' : '#ecfdf5',
            color: feedback.type === 'error' ? 'var(--df-error-text)' : '#065f46',
            border: `1px solid ${feedback.type === 'error' ? 'var(--df-error-border)' : '#a7f3d0'}`,
          }}
        >
          <span>{feedback.message}</span>
          <button
            type="button"
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
            aria-label="Dismiss feedback"
          >
            ✕
          </button>
        </div>
      )}

      {/* Main Content Area */}
      {isLoading ? (
        <LoadingState message="Loading team members..." />
      ) : loadError ? (
        <ErrorState
          title={loadError.status === 403 ? 'Access Denied' : 'Unable to load team members'}
          message={
            loadError.status === 403
              ? 'You do not have access to view members of this project.'
              : loadError.message
          }
          onRetry={loadError.status !== 403 ? handleRetry : null}
        />
      ) : members.length === 0 ? (
        <EmptyState
          title="No team members assigned"
          description="There are currently no team members assigned to this project. Add members to coordinate sprints and assign issues."
          actionLabel={canManageMembers ? 'Add Member' : null}
          onAction={canManageMembers ? () => setIsAddModalOpen(true) : null}
        />
      ) : (
        <div className="df-table-container">
          <table className="df-table" aria-label="Project team members">
            <thead>
              <tr>
                <th className="df-th" scope="col">Team Member</th>
                <th className="df-th" scope="col">Role</th>
                <th className="df-th" scope="col">Joined Date</th>
                {canManageMembers && (
                  <th className="df-th" scope="col" style={{ textAlign: 'right' }}>Actions</th>
                )}
              </tr>
            </thead>
            <tbody>
              {members.map((member) => (
                <tr key={member.id} className="df-tr">
                  <td className="df-td">
                    <div className="df-member-cell">
                      <div className="df-avatar-sm" aria-hidden="true">
                        {getInitials(member.user?.name)}
                      </div>
                      <div className="df-member-info">
                        <span className="df-member-name">{member.user?.name}</span>
                        <span className="df-member-email">{member.user?.email}</span>
                      </div>
                    </div>
                  </td>
                  <td className="df-td">
                    <span
                      className={`df-status-pill ${getRoleBadgeClass(member.user?.role)}`}
                      style={{ fontSize: '0.6875rem' }}
                    >
                      {USER_ROLE_LABELS[member.user?.role] || member.user?.role || 'Developer'}
                    </span>
                  </td>
                  <td className="df-td" style={{ color: 'var(--df-text-muted)', fontSize: '0.8125rem' }}>
                    {formatDate(member.joinedAt)}
                  </td>
                  {canManageMembers && (
                    <td className="df-td" style={{ textAlign: 'right' }}>
                      <button
                        type="button"
                        className="df-btn df-btn-danger-outline df-btn-sm"
                        onClick={() => setMemberToRemove(member)}
                        aria-label={`Remove ${member.user?.name || 'member'} from project`}
                      >
                        Remove
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add Member Modal */}
      <AddMemberModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onMemberAdded={handleMemberAdded}
        projectId={projectId}
        existingMemberUserIds={existingMemberUserIds}
      />

      {/* Remove Member Confirmation Modal */}
      <ConfirmModal
        isOpen={Boolean(memberToRemove)}
        title="Remove Team Member"
        message={`Are you sure you want to remove "${memberToRemove?.user?.name || memberToRemove?.userId}" from this project team? They will lose access to project sprints and issues.`}
        confirmLabel="Remove Member"
        isDanger
        isConfirming={isRemoving}
        onConfirm={handleConfirmRemove}
        onClose={() => setMemberToRemove(null)}
      />
    </div>
  );
};

export default ProjectMembers;
