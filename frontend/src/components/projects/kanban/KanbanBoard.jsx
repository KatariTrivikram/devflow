import React, { useState, useEffect, useCallback } from 'react';
import KanbanColumn from './KanbanColumn';
import IssueModal from '../IssueModal';
import LoadingState from '../../common/LoadingState';
import ErrorState from '../../common/ErrorState';
import EmptyState from '../../common/EmptyState';
import issueService from '../../../services/issueService';
import sprintService from '../../../services/sprintService';
import projectMemberService from '../../../services/projectMemberService';
import userService from '../../../services/userService';
import { useAuth } from '../../../hooks/useAuth';
import { USER_ROLES } from '../../../constants/roles';
import {
  ISSUE_STATUS,
  ISSUE_STATUS_LABELS,
  ISSUE_PRIORITY,
  ISSUE_PRIORITY_LABELS,
  ISSUE_TYPE,
  ISSUE_TYPE_LABELS,
} from '../../../constants/issueEnums';

const KANBAN_COLUMNS = [
  { status: ISSUE_STATUS.TODO, title: ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO] },
  { status: ISSUE_STATUS.IN_PROGRESS, title: ISSUE_STATUS_LABELS[ISSUE_STATUS.IN_PROGRESS] },
  { status: ISSUE_STATUS.CODE_REVIEW, title: ISSUE_STATUS_LABELS[ISSUE_STATUS.CODE_REVIEW] },
  { status: ISSUE_STATUS.DONE, title: ISSUE_STATUS_LABELS[ISSUE_STATUS.DONE] },
];

/**
 * KanbanBoard Component
 * Visual workflow board organizing project issues into status columns.
 * Supports native drag-and-drop, accessible keyboard moves, search, and filtering.
 */
const KanbanBoard = ({
  projectId,
  project,
  canManageProject = false,
  sprints = null,
  members = null,
  initialSprintFilter = '',
  initialNoSprint = false,
  onFilterChange = null,
}) => {
  const { user } = useAuth();

  // Issues data & status
  const [issues, setIssues] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [updatingIssueId, setUpdatingIssueId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Filters
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [sprintFilter, setSprintFilter] = useState(initialSprintFilter);
  const [noSprint, setNoSprint] = useState(initialNoSprint);
  const [assigneeFilter, setAssigneeFilter] = useState('');

  // Project context (sprints & members)
  const [fetchedSprints, setFetchedSprints] = useState([]);
  const [fetchedMembers, setFetchedMembers] = useState([]);

  const projectSprints = sprints || fetchedSprints;
  const projectMembers = members || fetchedMembers;

  // Create / Edit modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIssue, setEditingIssue] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Determine permissions
  const canCreateIssue = Boolean(
    canManageProject ||
    (user && (
      user.role === USER_ROLES.ADMIN ||
      (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById)
    ))
  );

  const canEditIssue = useCallback(
    (issue) => {
      if (!user) return false;
      if (user.role === USER_ROLES.ADMIN) return true;
      if (canManageProject) return true;
      if (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById) return true;
      if (user.role === USER_ROLES.DEVELOPER && issue.assignedToId && issue.assignedToId === user.id) {
        return true;
      }
      return false;
    },
    [user, project, canManageProject]
  );

  // Load project sprints & members once on mount if not provided as props
  useEffect(() => {
    if (sprints && members) {
      return;
    }

    let ignore = false;

    async function loadProjectContext() {
      try {
        const [sprintData, memberRecords] = await Promise.all([
          sprintService.getProjectSprints(projectId, { size: 100 }).catch(() => ({ content: [] })),
          projectMemberService.getProjectMembers(projectId).catch(() => []),
        ]);

        if (ignore) return;
        setFetchedSprints(sprintData.content || []);

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
          project?.createdById &&
          !enriched.some((m) => m.userId === project.createdById || m.user?.id === project.createdById)
        ) {
          try {
            const creatorUser = await userService.getUserById(project.createdById);
            enriched.unshift({
              id: `creator-${project.createdById}`,
              projectId,
              userId: project.createdById,
              user: creatorUser,
            });
          } catch {
            // Ignore
          }
        }

        if (!ignore) {
          setFetchedMembers(enriched);
        }
      } catch {
        // Ignore fallback
      }
    }

    loadProjectContext();

    return () => {
      ignore = true;
    };
  }, [projectId, project, sprints, members]);

  // Refresh trigger
  const [refreshKey, setRefreshKey] = useState(0);
  const handleRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  // Load issues from backend with active filters
  useEffect(() => {
    let ignore = false;

    async function loadIssues() {
      setIsLoading(true);
      setFetchError(null);

      try {
        const queryParams = {
          projectId,
          sprintId: sprintFilter || undefined,
          assignedToId: assigneeFilter || undefined,
          priority: priorityFilter || undefined,
          type: typeFilter || undefined,
          search: searchQuery || undefined,
          page: 0,
          size: 100,
          sort: 'createdAt,desc',
        };
        if (noSprint) {
          queryParams.noSprint = true;
        }

        const data = await issueService.getIssues(queryParams);

        if (!ignore) {
          setIssues(data.content || []);
        }
      } catch (err) {
        if (!ignore) {
          setFetchError(
            err.normalized || {
              status: 500,
              message: 'Failed to load project issues for Kanban board.',
            }
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadIssues();

    return () => {
      ignore = true;
    };
  }, [projectId, sprintFilter, noSprint, assigneeFilter, priorityFilter, typeFilter, searchQuery, refreshKey]);

  // Handle status update (drag-and-drop or select) with optimistic update & rollback
  const handleStatusChange = useCallback(
    async (issue, newStatus) => {
      if (issue.status === newStatus) return;

      if (!canEditIssue(issue)) {
        setFeedback({
          type: 'error',
          message: 'You do not have permission to update this issue.',
        });
        return;
      }

      if (updatingIssueId === issue.id) return;

      const previousIssues = issues;
      const originalStatus = issue.status;

      // Optimistic update
      setIssues((prev) =>
        prev.map((item) => (item.id === issue.id ? { ...item, status: newStatus } : item))
      );
      setUpdatingIssueId(issue.id);
      setFeedback(null);

      try {
        const updated = await issueService.updateIssue(issue.id, { status: newStatus });
        setIssues((prev) =>
          prev.map((item) => (item.id === issue.id ? { ...item, ...updated } : item))
        );
        setFeedback({
          type: 'success',
          message: `Moved "${issue.title}" to ${ISSUE_STATUS_LABELS[newStatus]}.`,
        });
      } catch (err) {
        // Rollback optimistic update
        setIssues(previousIssues);
        setFeedback({
          type: 'error',
          message:
            err.normalized?.message ||
            `Failed to move issue to ${ISSUE_STATUS_LABELS[newStatus]}. Reverted to ${ISSUE_STATUS_LABELS[originalStatus]}.`,
        });
      } finally {
        setUpdatingIssueId(null);
      }
    },
    [issues, canEditIssue, updatingIssueId]
  );

  // Drop issue handler from KanbanColumn
  const handleDropIssue = useCallback(
    (issueId, targetStatus) => {
      const issue = issues.find((i) => i.id === issueId);
      if (issue && issue.status !== targetStatus) {
        handleStatusChange(issue, targetStatus);
      }
    },
    [issues, handleStatusChange]
  );

  // Search submit
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setSearchQuery(searchInput.trim());
  };

  // Clear filters
  const handleClearFilters = () => {
    setSearchInput('');
    setSearchQuery('');
    setPriorityFilter('');
    setTypeFilter('');
    setSprintFilter('');
    setNoSprint(false);
    setAssigneeFilter('');
    if (onFilterChange) onFilterChange({ sprintId: '', noSprint: false });
  };

  const isFiltering = Boolean(
    searchQuery || priorityFilter || typeFilter || sprintFilter || noSprint || assigneeFilter
  );

  const activeSprint = projectSprints.find((s) => s.status === 'ACTIVE');

  // Create & Edit modal handlers
  const handleOpenCreateModal = () => {
    setEditingIssue(null);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (issue) => {
    setEditingIssue(issue);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingIssue(null);
    setModalError(null);
  };

  const handleModalSubmit = async (payload) => {
    setIsSubmitting(true);
    setModalError(null);

    try {
      if (editingIssue) {
        const updated = await issueService.updateIssue(editingIssue.id, payload);
        setIssues((prev) =>
          prev.map((item) => (item.id === editingIssue.id ? updated : item))
        );
        setFeedback({
          type: 'success',
          message: `Issue "${updated.title}" was updated successfully.`,
        });
      } else {
        const created = await issueService.createIssue(payload);
        setIssues((prev) => [created, ...prev]);
        setFeedback({
          type: 'success',
          message: `Issue "${created.title}" was created successfully.`,
        });
      }
      handleCloseModal();
    } catch (err) {
      setModalError(
        err.normalized || {
          status: 500,
          message: 'Failed to save issue. Please check fields.',
        }
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="df-section-card" data-testid="kanban-board-container">
      {/* Header & Actions Toolbar */}
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Kanban Board
          </h2>
          <p className="df-section-subtitle">
            Visual workflow tracking for tasks, stories, defects, and iterations.
          </p>
        </div>
        {canCreateIssue && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleOpenCreateModal}
            id="btn-create-kanban-issue"
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
            Create Issue
          </button>
        )}
      </div>

      {/* Action / Error Feedback Alert */}
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

      {/* Quick Filter Pills */}
      <div className="df-quick-filter-pills" data-testid="kanban-quick-filters" role="group" aria-label="Kanban quick filters">
        <span className="df-quick-filter-label">Quick:</span>
        <button
          type="button"
          className={`df-quick-pill ${!sprintFilter && !noSprint ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setSprintFilter('');
            setNoSprint(false);
            if (onFilterChange) onFilterChange({ sprintId: '', noSprint: false });
          }}
          data-testid="kanban-pill-filter-all"
        >
          All Sprints
        </button>

        {activeSprint && (
          <button
            type="button"
            className={`df-quick-pill ${sprintFilter === activeSprint.id && !noSprint ? 'df-quick-pill--active' : ''}`}
            onClick={() => {
              setSprintFilter(activeSprint.id);
              setNoSprint(false);
              if (onFilterChange) onFilterChange({ sprintId: activeSprint.id, noSprint: false });
            }}
            data-testid="kanban-pill-filter-current-sprint"
          >
            Current Sprint
          </button>
        )}

        <button
          type="button"
          className={`df-quick-pill ${noSprint ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setSprintFilter('');
            setNoSprint(true);
            if (onFilterChange) onFilterChange({ sprintId: '', noSprint: true });
          }}
          data-testid="kanban-pill-filter-no-sprint"
        >
          No Sprint
        </button>
      </div>

      {/* Compact Filter Toolbar */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '0.75rem',
          alignItems: 'center',
          marginBottom: '1rem',
        }}
      >
        {/* Search */}
        <form
          onSubmit={handleSearchSubmit}
          style={{ display: 'flex', gap: '0.5rem', flex: '1 1 200px', maxWidth: '300px' }}
        >
          <input
            type="text"
            className="df-input df-search-input"
            placeholder="Search board issues..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            aria-label="Search board issues"
          />
        </form>

        {/* Priority Filter */}
        <select
          className="df-select"
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
          aria-label="Filter board by priority"
          style={{ maxWidth: '140px' }}
        >
          <option value="">All Priorities</option>
          <option value={ISSUE_PRIORITY.LOW}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.LOW]}</option>
          <option value={ISSUE_PRIORITY.MEDIUM}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.MEDIUM]}</option>
          <option value={ISSUE_PRIORITY.HIGH}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.HIGH]}</option>
          <option value={ISSUE_PRIORITY.CRITICAL}>{ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.CRITICAL]}</option>
        </select>

        {/* Type Filter */}
        <select
          className="df-select"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          aria-label="Filter board by type"
          style={{ maxWidth: '140px' }}
        >
          <option value="">All Types</option>
          <option value={ISSUE_TYPE.TASK}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.TASK]}</option>
          <option value={ISSUE_TYPE.FEATURE}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.FEATURE]}</option>
          <option value={ISSUE_TYPE.BUG}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.BUG]}</option>
          <option value={ISSUE_TYPE.IMPROVEMENT}>{ISSUE_TYPE_LABELS[ISSUE_TYPE.IMPROVEMENT]}</option>
        </select>

        {/* Sprint Filter */}
        <select
          className="df-select"
          value={sprintFilter}
          onChange={(e) => setSprintFilter(e.target.value)}
          aria-label="Filter board by sprint"
          style={{ maxWidth: '150px' }}
        >
          <option value="">All Sprints</option>
          {projectSprints.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>

        {/* Assignee Filter */}
        <select
          className="df-select"
          value={assigneeFilter}
          onChange={(e) => setAssigneeFilter(e.target.value)}
          aria-label="Filter board by assignee"
          style={{ maxWidth: '150px' }}
        >
          <option value="">All Assignees</option>
          {projectMembers.map((m) => {
            const uid = m.userId || m.user?.id;
            const uname = m.user?.name || m.user?.email || `User ${uid.substring(0, 8)}`;
            return (
              <option key={m.id || uid} value={uid}>
                {uname}
              </option>
            );
          })}
        </select>

        {/* Clear Filters */}
        {isFiltering && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleClearFilters}
            aria-label="Clear board filters"
          >
            Clear filters
          </button>
        )}
      </div>

      {/* Main Content: Loading, Error, or Columns */}
      {isLoading ? (
        <LoadingState message="Loading board issues..." />
      ) : fetchError ? (
        <ErrorState
          title="Unable to load board"
          message={fetchError.message}
          onRetry={handleRefresh}
        />
      ) : issues.length === 0 && !isFiltering ? (
        <EmptyState
          title="No issues created yet"
          description="No issues have been created for this project yet. Start tracking work items by creating your first issue."
          actionLabel={canCreateIssue ? 'Create Issue' : null}
          onAction={canCreateIssue ? handleOpenCreateModal : null}
        />
      ) : (
        <div className="df-kanban-board" role="region" aria-label="Kanban columns">
          {KANBAN_COLUMNS.map((col) => {
            const columnIssues = issues.filter((i) => i.status === col.status);
            return (
              <KanbanColumn
                key={col.status}
                status={col.status}
                title={col.title}
                issues={columnIssues}
                onDropIssue={handleDropIssue}
                onStatusChange={handleStatusChange}
                onEditIssue={handleOpenEditModal}
                canEditIssue={canEditIssue}
                updatingIssueId={updatingIssueId}
                sprints={projectSprints}
                members={projectMembers}
              />
            );
          })}
        </div>
      )}

      {/* Create / Edit Issue Modal */}
      <IssueModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSubmit={handleModalSubmit}
        projectId={projectId}
        project={project}
        initialData={editingIssue}
        sprints={projectSprints}
        members={projectMembers}
        isSubmitting={isSubmitting}
        serverError={modalError}
      />
    </div>
  );
};

export default KanbanBoard;
