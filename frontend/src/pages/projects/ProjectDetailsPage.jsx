import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom';
import projectService from '../../services/projectService';
import sprintService from '../../services/sprintService';
import projectMemberService from '../../services/projectMemberService';
import userService from '../../services/userService';
import activityService from '../../services/activityService';
import ProjectStatusBadge from '../../components/projects/ProjectStatusBadge';
import ProjectModal from '../../components/projects/ProjectModal';
import ConfirmModal from '../../components/common/ConfirmModal';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import { formatDateTime } from '../../utils/date';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLES } from '../../constants/roles';
import ProjectMembers from '../../components/projects/ProjectMembers';
import SprintList from '../../components/projects/SprintList';
import IssueList from '../../components/projects/IssueList';
import KanbanBoard from '../../components/projects/kanban/KanbanBoard';
import ProjectSummaryCards from '../../components/projects/ProjectSummaryCards';
import ActiveSprintCard from '../../components/projects/ActiveSprintCard';
import RecentActivity from '../../components/dashboard/RecentActivity';

export const ProjectDetailsPage = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();

  const [project, setProject] = useState(null);
  const [summary, setSummary] = useState(null);
  const [recentActivities, setRecentActivities] = useState([]);
  const [projectSprints, setProjectSprints] = useState([]);
  const [projectMembers, setProjectMembers] = useState([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Edit modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [editError, setEditError] = useState(null);

  // Delete modal state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [actionFeedback, setActionFeedback] = useState(null);

  // Issues view mode: 'list' | 'board' (synced with URL)
  const viewParam = searchParams.get('view');
  const [issueView, setIssueView] = useState(viewParam === 'board' ? 'board' : 'list');

  // Filters from URL
  const [selectedSprintFilter, setSelectedSprintFilter] = useState(() => searchParams.get('sprint') || '');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState(() => searchParams.get('status') || '');

  const canManageProject = Boolean(
    !user ||
    user.role === USER_ROLES.ADMIN ||
    (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById)
  );

  const [reloadIndex, setReloadIndex] = useState(0);

  useEffect(() => {
    let ignore = false;

    async function loadProjectData() {
      try {
        const [projectData, summaryData, activityData, sprintData, memberRecords] = await Promise.all([
          projectService.getProjectById(projectId),
          projectService.getProjectSummary(projectId).catch(() => null),
          activityService.getActivitiesByProject(projectId, 10).catch(() => []),
          sprintService.getProjectSprints(projectId, { size: 100 }).catch(() => ({ content: [] })),
          projectMemberService.getProjectMembers(projectId).catch(() => []),
        ]);

        if (ignore) return;
        setProject(projectData);
        setSummary(summaryData);
        setRecentActivities(activityData || []);
        setProjectSprints(sprintData?.content || []);

        const enrichedMembers = await Promise.all(
          (memberRecords || []).map(async (record) => {
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
          !enrichedMembers.some((m) => m.userId === projectData.createdById || m.user?.id === projectData.createdById)
        ) {
          try {
            const creatorUser = await userService.getUserById(projectData.createdById);
            enrichedMembers.unshift({
              id: `creator-${projectData.createdById}`,
              projectId,
              userId: projectData.createdById,
              user: creatorUser,
            });
          } catch {
            // Ignore
          }
        }

        if (!ignore) {
          setProjectMembers(enrichedMembers);
        }
      } catch (err) {
        if (!ignore) {
          setError(
            err.normalized || {
              status: 500,
              message: 'Failed to retrieve project details.',
            }
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadProjectData();

    return () => {
      ignore = true;
    };
  }, [projectId, reloadIndex]);

  const handleRetry = () => {
    setIsLoading(true);
    setError(null);
    setReloadIndex((prev) => prev + 1);
  };

  const handleViewChange = (newView) => {
    setIssueView(newView);
    const nextParams = new URLSearchParams(searchParams);
    if (newView === 'board') {
      nextParams.set('view', 'board');
    } else {
      nextParams.delete('view');
    }
    setSearchParams(nextParams, { replace: true });
  };

  const handleFilterSprint = (sprintId) => {
    setSelectedSprintFilter(sprintId);
    const nextParams = new URLSearchParams(searchParams);
    if (sprintId) {
      nextParams.set('sprint', sprintId);
    } else {
      nextParams.delete('sprint');
    }
    setSearchParams(nextParams, { replace: true });
  };

  const handleSelectStatus = (status) => {
    setSelectedStatusFilter(status);
    const nextParams = new URLSearchParams(searchParams);
    if (status) {
      nextParams.set('status', status);
    } else {
      nextParams.delete('status');
    }
    setSearchParams(nextParams, { replace: true });
  };

  const activeSprintObj =
    summary?.activeSprint ||
    projectSprints.find((s) => s.status === 'ACTIVE') ||
    null;

  // Edit project handler
  const handleUpdateProject = async (formData) => {
    setIsUpdating(true);
    setEditError(null);

    try {
      const updated = await projectService.updateProject(projectId, formData);
      setProject(updated);
      setIsEditModalOpen(false);
      setActionFeedback({ type: 'success', message: 'Project updated successfully.' });
    } catch (err) {
      setEditError(err.normalized || {
        status: 500,
        message: 'Failed to update project. Please verify inputs.',
      });
    } finally {
      setIsUpdating(false);
    }
  };

  // Delete project handler
  const handleDeleteProject = async () => {
    setIsDeleting(true);
    try {
      await projectService.deleteProject(projectId);
      setIsDeleteModalOpen(false);
      navigate('/projects', {
        replace: true,
        state: { message: `Project "${project?.name}" was successfully deleted.` },
      });
    } catch (err) {
      setActionFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to delete project.',
      });
      setIsDeleteModalOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="df-page">
        <div className="df-section-card">
          <LoadingState message="Loading project details..." />
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
                ? 'Project Not Found'
                : isForbidden
                ? 'Access Denied'
                : 'Unable to Load Project'
            }
            message={
              isNotFound
                ? 'The requested project could not be found. It may have been removed or you may have followed an outdated link.'
                : isForbidden
                ? 'You do not have sufficient permissions to view this project.'
                : error.message
            }
            onRetry={!isNotFound && !isForbidden ? handleRetry : null}
            retryLabel="Reload"
          />
          <div style={{ marginTop: '1rem' }}>
            <Link to="/projects" className="df-btn df-btn-secondary df-btn-sm">
              Return to Projects List
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="df-page">
      {/* Navigation breadcrumb / back link */}
      <Link to="/projects" className="df-back-link">
        ← Back to Projects
      </Link>

      {/* Action feedback alert */}
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

      {/* Details Header */}
      <div className="df-details-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <h1 className="df-page-title" style={{ margin: 0 }}>
              {project?.name}
            </h1>
            <ProjectStatusBadge status={project?.status} />
          </div>
          <p className="df-page-subtitle" style={{ marginTop: '0.375rem' }}>
            Project Overview & Specifications
          </p>
        </div>

        {canManageProject && (
          <div className="df-details-actions">
            <button
              type="button"
              onClick={() => setIsEditModalOpen(true)}
              className="df-btn df-btn-secondary"
              id="btn-edit-project"
            >
              Edit Project
            </button>
            <button
              type="button"
              onClick={() => setIsDeleteModalOpen(true)}
              className="df-btn df-btn-danger-outline"
              id="btn-delete-project"
            >
              Delete Project
            </button>
          </div>
        )}
      </div>

      {/* Project Information Card */}
      <div className="df-section-card" style={{ marginBottom: '1.5rem' }}>
        <h2 className="df-section-title" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>
          About this Project
        </h2>
        <div className="df-desc-box">
          {project?.description ? (
            project.description
          ) : (
            <span style={{ color: 'var(--df-text-subtle)', fontStyle: 'italic' }}>
              No description provided for this project.
            </span>
          )}
        </div>

        {/* Metadata Grid */}
        <div className="df-meta-grid">
          <div className="df-meta-item">
            <span className="df-meta-label">Project ID</span>
            <span className="df-meta-value" style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>
              {project?.id}
            </span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Status</span>
            <div style={{ marginTop: '2px' }}>
              <ProjectStatusBadge status={project?.status} />
            </div>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Created</span>
            <span className="df-meta-value">
              {formatDateTime(project?.createdAt)}
            </span>
          </div>

          <div className="df-meta-item">
            <span className="df-meta-label">Last Updated</span>
            <span className="df-meta-value">
              {formatDateTime(project?.updatedAt)}
            </span>
          </div>
        </div>
      </div>

      {/* Project Summary Cards */}
      {summary && (
        <ProjectSummaryCards
          summary={summary}
          activeSprintName={activeSprintObj?.name}
          onSelectStatus={handleSelectStatus}
        />
      )}

      {/* Active Sprint Banner */}
      <ActiveSprintCard
        activeSprint={activeSprintObj}
        canManageSprints={canManageProject}
        onFilterSprint={handleFilterSprint}
        isFiltered={selectedSprintFilter === activeSprintObj?.id}
      />

      {/* Issues Section with View Mode Toggle */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '0.75rem',
        }}
      >
        <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--df-text-muted)' }}>
          Issue View Mode
        </span>
        <div className="df-view-toggle" role="tablist" aria-label="Issue views">
          <button
            type="button"
            className={`df-view-toggle-btn ${issueView === 'list' ? 'df-view-toggle-btn-active' : ''}`}
            onClick={() => handleViewChange('list')}
            role="tab"
            aria-selected={issueView === 'list'}
            id="tab-issue-list"
          >
            <svg
              style={{ width: '14px', height: '14px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
            List View
          </button>
          <button
            type="button"
            className={`df-view-toggle-btn ${issueView === 'board' ? 'df-view-toggle-btn-active' : ''}`}
            onClick={() => handleViewChange('board')}
            role="tab"
            aria-selected={issueView === 'board'}
            id="tab-issue-board"
          >
            <svg
              style={{ width: '14px', height: '14px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2"
              />
            </svg>
            Board View
          </button>
        </div>
      </div>

      {issueView === 'list' ? (
        <IssueList
          projectId={projectId}
          project={project}
          canManageProject={canManageProject}
          sprints={projectSprints}
          members={projectMembers}
          initialSprintFilter={selectedSprintFilter}
          initialStatusFilter={selectedStatusFilter}
          onFilterChange={({ status, sprintId }) => {
            if (status !== undefined) handleSelectStatus(status);
            if (sprintId !== undefined) handleFilterSprint(sprintId);
          }}
        />
      ) : (
        <KanbanBoard
          projectId={projectId}
          project={project}
          canManageProject={canManageProject}
          sprints={projectSprints}
          members={projectMembers}
          initialSprintFilter={selectedSprintFilter}
          onFilterChange={({ sprintId }) => {
            if (sprintId !== undefined) handleFilterSprint(sprintId);
          }}
        />
      )}

      {/* Sprints Section */}
      <SprintList
        projectId={projectId}
        canManageSprints={canManageProject}
      />

      {/* Team Members Section */}
      <ProjectMembers
        projectId={projectId}
        canManageMembers={canManageProject}
      />

      {/* Recent Activity Audit Trail */}
      <div style={{ marginTop: '1.5rem' }}>
        <RecentActivity activities={recentActivities} />
      </div>

      {/* Edit Project Modal */}
      <ProjectModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditError(null);
        }}
        onSubmit={handleUpdateProject}
        initialData={project}
        isSubmitting={isUpdating}
        serverError={editError}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={isDeleteModalOpen}
        title="Delete Project"
        message={`Are you sure you want to permanently delete "${project?.name}"? All associated sprints, issues, and member records will be permanently removed. This action cannot be undone.`}
        confirmLabel="Delete Project"
        isDanger
        isConfirming={isDeleting}
        onConfirm={handleDeleteProject}
        onClose={() => setIsDeleteModalOpen(false)}
      />
    </div>
  );
};

export default ProjectDetailsPage;
