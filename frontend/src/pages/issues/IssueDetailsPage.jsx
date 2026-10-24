import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import issueService from '../../services/issueService';
import projectService from '../../services/projectService';
import sprintService from '../../services/sprintService';
import projectMemberService from '../../services/projectMemberService';
import userService from '../../services/userService';
import IssueTypeBadge from '../../components/projects/IssueTypeBadge';
import IssuePriorityBadge from '../../components/projects/IssuePriorityBadge';
import IssueStatusBadge from '../../components/projects/IssueStatusBadge';
import IssueModal from '../../components/projects/IssueModal';
import { ISSUE_STATUS, ISSUE_STATUS_LABELS } from '../../constants/issueEnums';
import IssueComments from '../../components/issues/IssueComments';
import IssueActivity from '../../components/issues/IssueActivity';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import { formatDateTime } from '../../utils/date';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLES } from '../../constants/roles';

/**
 * IssueDetailsPage Component
 * Full issue overview containing metadata, status, description, comments collaboration,
 * and activity audit trail.
 */
export const IssueDetailsPage = () => {
  const { issueId } = useParams();
  const { user } = useAuth();

  const [issue, setIssue] = useState(null);
  const [project, setProject] = useState(null);
  const [sprints, setSprints] = useState([]);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Edit issue modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [editError, setEditError] = useState(null);
  const [actionFeedback, setActionFeedback] = useState(null);
  const [isTransitioning, setIsTransitioning] = useState(false);

  // Reload trigger for refresh/retry
  const [reloadTrigger, setReloadTrigger] = useState(0);

  const WORKFLOW_STEPS = [
    { status: ISSUE_STATUS.TODO, label: 'To Do' },
    { status: ISSUE_STATUS.IN_PROGRESS, label: 'In Progress' },
    { status: ISSUE_STATUS.CODE_REVIEW, label: 'Code Review' },
    { status: ISSUE_STATUS.DONE, label: 'Done' },
  ];

  const handleStatusTransition = async (newStatus) => {
    if (!issue || issue.status === newStatus || isTransitioning) return;
    setIsTransitioning(true);
    try {
      const updated = await issueService.updateIssue(issueId, { status: newStatus });
      setIssue(updated);
      setActionFeedback({
        type: 'success',
        message: `Status moved to ${ISSUE_STATUS_LABELS[newStatus] || newStatus}.`,
      });
    } catch (err) {
      setActionFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to update issue status.',
      });
    } finally {
      setIsTransitioning(false);
    }
  };

  const loadIssue = useCallback(() => {
    setIsLoading(true);
    setError(null);
    setReloadTrigger((v) => v + 1);
  }, []);

  useEffect(() => {
    let ignore = false;

    async function load() {
      try {
        const data = await issueService.getIssueById(issueId);
        if (ignore) return;
        setIssue(data);

        // Load parent project and contextual sprints & members
        if (data.projectId) {
          try {
            const [projectData, sprintData, memberRecords] = await Promise.all([
              projectService.getProjectById(data.projectId).catch(() => null),
              sprintService.getProjectSprints(data.projectId, { size: 100 }).catch(() => ({ content: [] })),
              projectMemberService.getProjectMembers(data.projectId).catch(() => []),
            ]);

            if (ignore) return;
            setProject(projectData);
            setSprints(sprintData.content || []);

            const enriched = await Promise.all(
              memberRecords.map(async (record) => {
                try {
                  const u = await userService.getUserById(record.userId);
                  return { ...record, user: u };
                } catch {
                  return {
                    ...record,
                    user: { id: record.userId, name: `User ${record.userId.substring(0, 8)}`, email: '' },
                  };
                }
              })
            );

            if (
              projectData?.createdById &&
              !enriched.some((m) => m.userId === projectData.createdById || m.user?.id === projectData.createdById)
            ) {
              try {
                const creatorUser = await userService.getUserById(projectData.createdById);
                enriched.unshift({
                  id: `creator-${projectData.createdById}`,
                  projectId: data.projectId,
                  userId: projectData.createdById,
                  user: creatorUser,
                });
              } catch {
                // Ignore
              }
            }

            if (!ignore) {
              setMembers(enriched);
            }
          } catch {
            // Contextual load failures are non-fatal
          }
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err.normalized || {
              status: 500,
              message: 'Failed to load issue details.',
            }
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    load();

    return () => {
      ignore = true;
    };
  }, [issueId, reloadTrigger]);

  // Authorization check for editing the issue
  const canEditIssue = Boolean(
    user && (
      user.role === USER_ROLES.ADMIN ||
      (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById) ||
      (user.role === USER_ROLES.DEVELOPER && issue?.assignedToId && issue.assignedToId === user.id)
    )
  );

  // Edit issue submission
  const handleUpdateIssue = async (payload) => {
    setIsUpdating(true);
    setEditError(null);

    try {
      const updated = await issueService.updateIssue(issueId, payload);
      setIssue(updated);
      setIsEditModalOpen(false);
      setActionFeedback({ type: 'success', message: 'Issue updated successfully.' });
    } catch (err) {
      setEditError(
        err.normalized || {
          status: 500,
          message: 'Failed to update issue. Please check inputs.',
        }
      );
    } finally {
      setIsUpdating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="df-page">
        <div className="df-section-card">
          <LoadingState message="Loading issue details..." />
        </div>
      </div>
    );
  }

  if (error) {
    const isNotFound = error.status === 404;
    const isForbidden = error.status === 403;

    return (
      <div className="df-page">
        <Link to="/projects" className="df-back-link">
          ← Back to Projects
        </Link>
        <div className="df-section-card">
          <ErrorState
            title={
              isNotFound
                ? 'Issue Not Found'
                : isForbidden
                ? 'Access Denied'
                : 'Unable to Load Issue'
            }
            message={
              isNotFound
                ? 'The requested issue could not be found or has been removed.'
                : isForbidden
                ? 'You do not have permission to view this issue.'
                : error.message
            }
            onRetry={!isNotFound && !isForbidden ? loadIssue : null}
          />
        </div>
      </div>
    );
  }

  // Lookup sprint name & assignee name
  const sprint = sprints.find((s) => s.id === issue?.sprintId);
  const sprintName = sprint ? sprint.name : issue?.sprintId ? `Sprint #${issue.sprintId.substring(0, 8)}` : 'None';

  const memberRecord = members.find(
    (m) => m.userId === issue?.assignedToId || m.user?.id === issue?.assignedToId
  );
  const assigneeName = memberRecord
    ? memberRecord.user?.name || memberRecord.user?.email || `User #${issue.assignedToId.substring(0, 8)}`
    : issue?.assignedToId
    ? `User #${issue.assignedToId.substring(0, 8)}`
    : 'Unassigned';

  const backLinkUrl = issue?.projectId ? `/projects/${issue.projectId}` : '/projects';

  return (
    <div className="df-page">
      {/* Back Navigation */}
      <Link to={backLinkUrl} className="df-back-link">
        ← Back to Project
      </Link>

      {/* Action feedback banner */}
      {actionFeedback && (
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
            backgroundColor: actionFeedback.type === 'error' ? 'var(--df-error-bg)' : '#ecfdf5',
            color: actionFeedback.type === 'error' ? 'var(--df-error-text)' : '#065f46',
            border: `1px solid ${actionFeedback.type === 'error' ? 'var(--df-error-border)' : '#a7f3d0'}`,
          }}
        >
          <span>{actionFeedback.message}</span>
          <button
            type="button"
            onClick={() => setActionFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
            aria-label="Dismiss feedback"
          >
            ✕
          </button>
        </div>
      )}

      {/* Header with Title, Badges, and Edit Button */}
      <div className="df-details-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <h1 className="df-page-title" style={{ margin: 0 }}>
              {issue?.title}
            </h1>
            <div style={{ display: 'flex', gap: '0.375rem', alignItems: 'center' }}>
              <IssueTypeBadge type={issue?.type} />
              <IssuePriorityBadge priority={issue?.priority} />
              <IssueStatusBadge status={issue?.status} />
            </div>
          </div>
          <p className="df-page-subtitle" style={{ marginTop: '0.375rem' }}>
            Issue ID: <code style={{ fontSize: '0.8125rem' }}>{issue?.id}</code>
          </p>
        </div>

        {canEditIssue && (
          <div className="df-details-actions">
            <button
              type="button"
              className="df-btn df-btn-secondary"
              onClick={() => setIsEditModalOpen(true)}
              id="btn-edit-issue"
            >
              Edit Issue
            </button>
          </div>
        )}
      </div>

      {/* Workflow Stepper Card */}
      <div className="df-section-card" style={{ marginBottom: '1.5rem' }} data-testid="workflow-stepper">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <h2 className="df-section-title" style={{ fontSize: '1rem', margin: 0 }}>
              Workflow Progression
            </h2>
            <p className="df-section-subtitle" style={{ margin: '0.25rem 0 0 0' }}>
              Track and advance issue lifecycle state
            </p>
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--df-text-muted)' }}>
            Current Stage: {ISSUE_STATUS_LABELS[issue?.status] || issue?.status}
          </div>
        </div>

        <div className="df-stepper-container">
          {WORKFLOW_STEPS.map((step, idx) => {
            const currentIdx = WORKFLOW_STEPS.findIndex((s) => s.status === issue?.status);
            const isCurrent = step.status === issue?.status;
            const isCompleted = currentIdx > idx;

            return (
              <div
                key={step.status}
                className={`df-stepper-step ${
                  isCurrent
                    ? 'df-stepper-step--active'
                    : isCompleted
                    ? 'df-stepper-step--completed'
                    : 'df-stepper-step--pending'
                }`}
                data-testid={`workflow-step-${step.status}`}
              >
                <div className="df-stepper-circle">
                  {isCompleted ? (
                    <svg style={{ width: '14px', height: '14px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    <span>{idx + 1}</span>
                  )}
                </div>
                <div className="df-stepper-text">
                  <span className="df-stepper-label">{idx + 1}. {step.label}</span>
                  {canEditIssue && !isCurrent && (
                    <button
                      type="button"
                      className="df-stepper-action-btn"
                      onClick={() => handleStatusTransition(step.status)}
                      disabled={isTransitioning}
                      aria-label={`Move to ${step.label}`}
                    >
                      Move to {step.label}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Issue Overview & Specifications Card */}
      <div className="df-section-card" style={{ marginBottom: '1.5rem' }}>
        <h2 className="df-section-title" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>
          Description
        </h2>
        <div className="df-desc-box">
          {issue?.description ? (
            issue.description
          ) : (
            <span style={{ color: 'var(--df-text-subtle)', fontStyle: 'italic' }}>
              No description provided for this issue.
            </span>
          )}
        </div>

        {/* Metadata Grid */}
        <div className="df-meta-grid">
          <div className="df-meta-item">
            <span className="df-meta-label">Project</span>
            <span className="df-meta-value">
              {issue?.projectId ? (
                <Link to={`/projects/${issue.projectId}`} className="df-meta-link">
                  {project?.name || issue.projectId}
                </Link>
              ) : (
                '—'
              )}
            </span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Sprint</span>
            <span className="df-meta-value">
              {issue?.sprintId ? (
                <Link
                  to={`/projects/${issue.projectId}?view=list&sprint=${issue.sprintId}`}
                  className="df-meta-link"
                >
                  {sprintName}
                </Link>
              ) : (
                <span style={{ color: 'var(--df-text-subtle)' }}>None (Backlog)</span>
              )}
            </span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Assignee</span>
            <span className="df-meta-value">{assigneeName}</span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Created</span>
            <span className="df-meta-value">{formatDateTime(issue?.createdAt)}</span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Last Updated</span>
            <span className="df-meta-value">{formatDateTime(issue?.updatedAt)}</span>
          </div>
        </div>
      </div>

      {/* Comments Section */}
      <IssueComments issueId={issueId} project={project} members={members} />

      {/* Activity Timeline Section */}
      <div style={{ marginTop: '1.5rem' }}>
        <IssueActivity issueId={issueId} members={members} />
      </div>

      {/* Edit Issue Modal */}
      {canEditIssue && (
        <IssueModal
          isOpen={isEditModalOpen}
          onClose={() => {
            setIsEditModalOpen(false);
            setEditError(null);
          }}
          onSubmit={handleUpdateIssue}
          projectId={issue?.projectId}
          project={project}
          initialData={issue}
          sprints={sprints}
          members={members}
          isSubmitting={isUpdating}
          serverError={editError}
        />
      )}
    </div>
  );
};

export default IssueDetailsPage;
