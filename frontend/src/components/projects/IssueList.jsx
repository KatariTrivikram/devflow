import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import issueService from '../../services/issueService';
import sprintService from '../../services/sprintService';
import projectMemberService from '../../services/projectMemberService';
import userService from '../../services/userService';
import IssueStatusBadge from './IssueStatusBadge';
import IssuePriorityBadge from './IssuePriorityBadge';
import IssueTypeBadge from './IssueTypeBadge';
import IssueModal from './IssueModal';
import ConfirmModal from '../common/ConfirmModal';
import LoadingState from '../common/LoadingState';
import ErrorState from '../common/ErrorState';
import EmptyState from '../common/EmptyState';
import {
  ISSUE_STATUS,
  ISSUE_STATUS_LABELS,
  ISSUE_PRIORITY,
  ISSUE_PRIORITY_LABELS,
  ISSUE_TYPE,
  ISSUE_TYPE_LABELS,
} from '../../constants/issueEnums';
import { USER_ROLES } from '../../constants/roles';
import { formatDate } from '../../utils/date';
import { useAuth } from '../../hooks/useAuth';

const ISSUE_PAGE_SIZE = 10;

export const IssueList = ({
  projectId,
  project = null,
  canManageProject = false,
  sprints = null,
  members = null,
  initialSprintFilter = '',
  initialStatusFilter = '',
  initialNoSprint = false,
  onFilterChange = null,
}) => {
  const { user } = useAuth();

  const [issues, setIssues] = useState([]);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: ISSUE_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  // Filters & Search & Sort
  const [statusFilter, setStatusFilter] = useState(initialStatusFilter);
  const [priorityFilter, setPriorityFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [sprintFilter, setSprintFilter] = useState(initialSprintFilter);
  const [noSprint, setNoSprint] = useState(initialNoSprint);
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('createdAt,desc');
  const [refreshIndex, setRefreshIndex] = useState(0);

  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Project context (sprints & members for selection & lookups)
  const [fetchedSprints, setFetchedSprints] = useState([]);
  const [fetchedMembers, setFetchedMembers] = useState([]);

  const projectSprints = sprints || fetchedSprints;
  const projectMembers = members || fetchedMembers;

  // Create / Edit modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIssue, setEditingIssue] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Delete modal state
  const [issueToDelete, setIssueToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Feedback alert state
  const [feedback, setFeedback] = useState(null);

  // Authorization determinations
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
      if (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById) return true;
      if (user.role === USER_ROLES.DEVELOPER && issue.assignedToId && issue.assignedToId === user.id) {
        return true;
      }
      return false;
    },
    [user, project]
  );

  const canDeleteIssue = useCallback(() => {
    if (!user) return false;
    if (user.role === USER_ROLES.ADMIN) return true;
    if (user.role === USER_ROLES.PROJECT_MANAGER && user.id === project?.createdById) return true;
    return false;
  }, [user, project]);

  // Load project sprints & members once for dropdowns & lookup if not provided as props
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

        // Enrich members with user details
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

        // Include project creator if not already present
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
            // Ignore if creator details cannot be fetched
          }
        }

        if (!ignore) {
          setFetchedMembers(enriched);
        }
      } catch {
        // Fallback silently if context load fails
      }
    }

    loadProjectContext();

    return () => {
      ignore = true;
    };
  }, [projectId, project?.createdById, sprints, members]);

  // Load paginated issues
  useEffect(() => {
    let ignore = false;

    async function loadIssues() {
      try {
        const queryParams = {
          projectId,
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
          type: typeFilter || undefined,
          sprintId: sprintFilter || undefined,
          assignedToId: assigneeFilter || undefined,
          search: searchTerm || undefined,
          page,
          size: ISSUE_PAGE_SIZE,
          sort: sortBy,
        };
        if (noSprint) {
          queryParams.noSprint = true;
        }

        const data = await issueService.getIssues(queryParams);

        if (ignore) return;
        setIssues(data.content || []);
        setPagination({
          page: data.page ?? page,
          size: data.size ?? ISSUE_PAGE_SIZE,
          totalElements: data.totalElements ?? 0,
          totalPages: data.totalPages ?? 0,
          first: data.first ?? true,
          last: data.last ?? true,
        });
      } catch (err) {
        if (!ignore) {
          setFetchError(
            err.normalized || {
              status: 500,
              message: 'Failed to load project issues.',
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
  }, [
    projectId,
    statusFilter,
    priorityFilter,
    typeFilter,
    sprintFilter,
    noSprint,
    assigneeFilter,
    searchTerm,
    sortBy,
    page,
    refreshIndex,
  ]);

  const handleFilterChange = (setter) => (e) => {
    setIsLoading(true);
    setter(e.target.value);
    setPage(0);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setIsLoading(true);
    setPage(0);
    setRefreshIndex((idx) => idx + 1);
  };

  const handleClearFilters = () => {
    setIsLoading(true);
    setStatusFilter('');
    setPriorityFilter('');
    setTypeFilter('');
    setSprintFilter('');
    setNoSprint(false);
    setAssigneeFilter('');
    setSearchTerm('');
    setSortBy('createdAt,desc');
    setPage(0);
    if (onFilterChange) {
      onFilterChange({ status: '', sprintId: '', noSprint: false });
    }
  };

  const handleRetry = () => {
    setIsLoading(true);
    setFetchError(null);
    setRefreshIndex((idx) => idx + 1);
  };

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
    if (!isSubmitting) {
      setIsModalOpen(false);
      setEditingIssue(null);
      setModalError(null);
    }
  };

  const handleSaveIssue = async (formData) => {
    setIsSubmitting(true);
    setModalError(null);

    try {
      if (editingIssue) {
        await issueService.updateIssue(editingIssue.id, formData);
        setIsModalOpen(false);
        setEditingIssue(null);
        setFeedback({ type: 'success', message: 'Issue updated successfully.' });
        setIsLoading(true);
        setRefreshIndex((idx) => idx + 1);
      } else {
        await issueService.createIssue(formData);
        setIsModalOpen(false);
        setFeedback({ type: 'success', message: 'Issue created successfully.' });
        setIsLoading(true);
        setPage(0);
        setRefreshIndex((idx) => idx + 1);
      }
    } catch (err) {
      setModalError(
        err.normalized || {
          status: 500,
          message: 'An unexpected error occurred while saving the issue.',
        }
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!issueToDelete) return;

    setIsDeleting(true);
    try {
      await issueService.deleteIssue(issueToDelete.id);
      const deletedTitle = issueToDelete.title;
      setIssueToDelete(null);
      setFeedback({
        type: 'success',
        message: `Issue "${deletedTitle}" was deleted.`,
      });
      setIsLoading(true);
      setRefreshIndex((idx) => idx + 1);
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to delete issue.',
      });
      setIssueToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const handlePrevPage = () => {
    if (!pagination.first && page > 0) {
      setIsLoading(true);
      setPage((p) => p - 1);
    }
  };

  const handleNextPage = () => {
    if (!pagination.last && page < pagination.totalPages - 1) {
      setIsLoading(true);
      setPage((p) => p + 1);
    }
  };

  const isFiltering = Boolean(
    statusFilter || priorityFilter || typeFilter || sprintFilter || noSprint || assigneeFilter || searchTerm
  );

  const activeSprint = projectSprints.find((s) => s.status === 'ACTIVE');

  // Helper lookups
  const getSprintName = (sprintId) => {
    if (!sprintId) return null;
    const s = projectSprints.find((sp) => sp.id === sprintId);
    return s ? s.name : null;
  };

  const getAssigneeName = (assignedToId) => {
    if (!assignedToId) return 'Unassigned';
    const m = projectMembers.find((mem) => mem.userId === assignedToId || mem.user?.id === assignedToId);
    if (m) {
      return m.user?.name || m.userName || `User ${assignedToId.substring(0, 8)}`;
    }
    return `User ${assignedToId.substring(0, 8)}`;
  };

  return (
    <div className="df-section-card">
      {/* Header */}
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Issues & Backlog
          </h2>
          <p className="df-section-subtitle">
            Manage tasks, user stories, improvements, and defects for this project.
          </p>
        </div>
        {canCreateIssue && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleOpenCreateModal}
            id="btn-create-issue"
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

      {/* Quick Filter Pills */}
      <div className="df-quick-filter-pills" data-testid="issue-quick-filters" role="group" aria-label="Quick filters">
        <span className="df-quick-filter-label">Quick:</span>
        <button
          type="button"
          className={`df-quick-pill ${!statusFilter && !sprintFilter && !noSprint ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            setStatusFilter('');
            setSprintFilter('');
            setNoSprint(false);
            setPage(0);
            if (onFilterChange) onFilterChange({ status: '', sprintId: '', noSprint: false });
          }}
          data-testid="pill-filter-all"
        >
          All
        </button>

        {activeSprint && (
          <button
            type="button"
            className={`df-quick-pill ${sprintFilter === activeSprint.id && !noSprint ? 'df-quick-pill--active' : ''}`}
            onClick={() => {
              setIsLoading(true);
              setSprintFilter(activeSprint.id);
              setNoSprint(false);
              setPage(0);
              if (onFilterChange) onFilterChange({ sprintId: activeSprint.id, noSprint: false });
            }}
            data-testid="pill-filter-current-sprint"
          >
            Current Sprint
          </button>
        )}

        <button
          type="button"
          className={`df-quick-pill ${noSprint ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            setSprintFilter('');
            setNoSprint(true);
            setPage(0);
            if (onFilterChange) onFilterChange({ sprintId: '', noSprint: true });
          }}
          data-testid="pill-filter-no-sprint"
        >
          No Sprint
        </button>

        <span className="df-quick-pill-divider" aria-hidden="true" />

        <button
          type="button"
          className={`df-quick-pill ${statusFilter === ISSUE_STATUS.TODO ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            const next = statusFilter === ISSUE_STATUS.TODO ? '' : ISSUE_STATUS.TODO;
            setStatusFilter(next);
            setPage(0);
            if (onFilterChange) onFilterChange({ status: next });
          }}
          data-testid="pill-filter-todo"
        >
          To Do
        </button>

        <button
          type="button"
          className={`df-quick-pill ${statusFilter === ISSUE_STATUS.IN_PROGRESS ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            const next = statusFilter === ISSUE_STATUS.IN_PROGRESS ? '' : ISSUE_STATUS.IN_PROGRESS;
            setStatusFilter(next);
            setPage(0);
            if (onFilterChange) onFilterChange({ status: next });
          }}
          data-testid="pill-filter-in-progress"
        >
          In Progress
        </button>

        <button
          type="button"
          className={`df-quick-pill ${statusFilter === ISSUE_STATUS.CODE_REVIEW ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            const next = statusFilter === ISSUE_STATUS.CODE_REVIEW ? '' : ISSUE_STATUS.CODE_REVIEW;
            setStatusFilter(next);
            setPage(0);
            if (onFilterChange) onFilterChange({ status: next });
          }}
          data-testid="pill-filter-code-review"
        >
          Code Review
        </button>

        <button
          type="button"
          className={`df-quick-pill ${statusFilter === ISSUE_STATUS.DONE ? 'df-quick-pill--active' : ''}`}
          onClick={() => {
            setIsLoading(true);
            const next = statusFilter === ISSUE_STATUS.DONE ? '' : ISSUE_STATUS.DONE;
            setStatusFilter(next);
            setPage(0);
            if (onFilterChange) onFilterChange({ status: next });
          }}
          data-testid="pill-filter-done"
        >
          Done
        </button>
      </div>

      {/* Filter and Search Toolbar */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '0.75rem',
          alignItems: 'center',
          marginBottom: '1rem',
        }}
      >
        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.5rem', flex: '1 1 200px', maxWidth: '320px' }}>
          <input
            type="text"
            className="df-input df-search-input"
            placeholder="Search issues..."
            value={searchTerm}
            onChange={handleSearchChange}
            aria-label="Search issues"
          />
        </form>

        {/* Status Filter */}
        <select
          className="df-select"
          value={statusFilter}
          onChange={handleFilterChange(setStatusFilter)}
          aria-label="Filter issues by status"
          style={{ maxWidth: '140px' }}
        >
          <option value="">All Statuses</option>
          <option value={ISSUE_STATUS.TODO}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO]}</option>
          <option value={ISSUE_STATUS.IN_PROGRESS}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.IN_PROGRESS]}</option>
          <option value={ISSUE_STATUS.CODE_REVIEW}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.CODE_REVIEW]}</option>
          <option value={ISSUE_STATUS.DONE}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.DONE]}</option>
        </select>

        {/* Priority Filter */}
        <select
          className="df-select"
          value={priorityFilter}
          onChange={handleFilterChange(setPriorityFilter)}
          aria-label="Filter issues by priority"
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
          onChange={handleFilterChange(setTypeFilter)}
          aria-label="Filter issues by type"
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
          onChange={handleFilterChange(setSprintFilter)}
          aria-label="Filter issues by sprint"
          style={{ maxWidth: '150px' }}
        >
          <option value="">All Sprints</option>
          {projectSprints.map((sprint) => (
            <option key={sprint.id} value={sprint.id}>
              {sprint.name}
            </option>
          ))}
        </select>

        {/* Assignee Filter */}
        <select
          className="df-select"
          value={assigneeFilter}
          onChange={handleFilterChange(setAssigneeFilter)}
          aria-label="Filter issues by assignee"
          style={{ maxWidth: '150px' }}
        >
          <option value="">All Assignees</option>
          {projectMembers.map((member) => {
            const uid = member.userId || member.user?.id;
            const name = member.user?.name || member.userName || `User ${uid?.substring(0, 8)}`;
            return (
              <option key={uid} value={uid}>
                {name}
              </option>
            );
          })}
        </select>

        {/* Sort Selection */}
        <select
          className="df-select"
          value={sortBy}
          onChange={handleFilterChange(setSortBy)}
          aria-label="Sort issues"
          style={{ maxWidth: '160px' }}
        >
          <option value="createdAt,desc">Newest First</option>
          <option value="createdAt,asc">Oldest First</option>
          <option value="title,asc">Title (A-Z)</option>
          <option value="priority,desc">Priority</option>
          <option value="status,asc">Status</option>
          <option value="updatedAt,desc">Recently Updated</option>
        </select>

        {/* Clear Filters */}
        {isFiltering && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleClearFilters}
          >
            Clear filters
          </button>
        )}
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <LoadingState message="Loading issues..." />
      ) : fetchError ? (
        <ErrorState
          title={fetchError.status === 403 ? 'Access Denied' : 'Unable to load issues'}
          message={fetchError.message}
          onRetry={fetchError.status !== 403 ? handleRetry : null}
        />
      ) : issues.length === 0 ? (
        isFiltering ? (
          <EmptyState
            title="No matching issues"
            description="No issues found matching the selected search criteria or filters."
            actionLabel="Clear Filters"
            onAction={handleClearFilters}
          />
        ) : (
          <EmptyState
            title="No issues created yet"
            description="No issues have been created for this project yet. Start tracking work items by creating your first issue."
            actionLabel={canCreateIssue ? 'Create Issue' : null}
            onAction={canCreateIssue ? handleOpenCreateModal : null}
          />
        )
      ) : (
        <>
          <div className="df-table-container">
            <table className="df-table" aria-label="Project issues">
              <thead>
                <tr>
                  <th className="df-th" scope="col">Type</th>
                  <th className="df-th" scope="col">Title</th>
                  <th className="df-th" scope="col">Status</th>
                  <th className="df-th" scope="col">Priority</th>
                  <th className="df-th" scope="col">Assignee</th>
                  <th className="df-th" scope="col">Sprint</th>
                  <th className="df-th" scope="col">Created</th>
                  <th className="df-th" scope="col" style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((issue) => {
                  const sprintName = getSprintName(issue.sprintId);
                  const assigneeName = getAssigneeName(issue.assignedToId);
                  const userCanEdit = canEditIssue(issue);
                  const userCanDelete = canDeleteIssue();

                  return (
                    <tr key={issue.id} className="df-tr">
                      <td className="df-td">
                        <IssueTypeBadge type={issue.type} />
                      </td>
                      <td className="df-td" style={{ fontWeight: 600, minWidth: '200px' }}>
                        <div>
                          <Link
                            to={`/issues/${issue.id}`}
                            className="df-table-link"
                            style={{ textDecoration: 'none', color: 'inherit' }}
                          >
                            {issue.title}
                          </Link>
                        </div>
                        {issue.description && (
                          <div
                            style={{
                              fontSize: '0.75rem',
                              color: 'var(--df-text-muted)',
                              fontWeight: 400,
                              marginTop: '2px',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                              maxWidth: '300px',
                            }}
                          >
                            {issue.description}
                          </div>
                        )}
                      </td>
                      <td className="df-td">
                        <IssueStatusBadge status={issue.status} />
                      </td>
                      <td className="df-td">
                        <IssuePriorityBadge priority={issue.priority} />
                      </td>
                      <td className="df-td" style={{ fontSize: '0.8125rem', color: 'var(--df-text-muted)' }}>
                        {assigneeName}
                      </td>
                      <td className="df-td" style={{ fontSize: '0.8125rem', color: 'var(--df-text-muted)' }}>
                        {sprintName || <span style={{ color: 'var(--df-text-subtle)' }}>—</span>}
                      </td>
                      <td className="df-td" style={{ color: 'var(--df-text-muted)', fontSize: '0.8125rem' }}>
                        {formatDate(issue.createdAt)}
                      </td>
                      <td className="df-td" style={{ textAlign: 'right' }}>
                        <div className="df-td-actions">
                          {userCanEdit && (
                            <button
                              type="button"
                              className="df-btn df-btn-secondary df-btn-sm"
                              onClick={() => handleOpenEditModal(issue)}
                              aria-label={`Edit ${issue.title}`}
                            >
                              Edit
                            </button>
                          )}
                          {userCanDelete && (
                            <button
                              type="button"
                              className="df-btn df-btn-danger-outline df-btn-sm"
                              onClick={() => setIssueToDelete(issue)}
                              aria-label={`Delete ${issue.title}`}
                            >
                              Delete
                            </button>
                          )}
                          {!userCanEdit && !userCanDelete && (
                            <span style={{ color: 'var(--df-text-subtle)', fontSize: '0.8125rem' }}>—</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {pagination.totalPages > 1 && (
            <div className="df-pagination">
              <div className="df-pagination-info">
                Showing {pagination.page * pagination.size + 1} to{' '}
                {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                {pagination.totalElements} issues
              </div>
              <div className="df-pagination-controls">
                <button
                  type="button"
                  className="df-btn df-btn-secondary df-btn-sm"
                  onClick={handlePrevPage}
                  disabled={pagination.first || page === 0}
                  aria-label="Previous page"
                >
                  Previous
                </button>
                <span style={{ fontSize: '0.8125rem', color: 'var(--df-text-muted)', padding: '0 0.5rem' }}>
                  Page {pagination.page + 1} of {Math.max(1, pagination.totalPages)}
                </span>
                <button
                  type="button"
                  className="df-btn df-btn-secondary df-btn-sm"
                  onClick={handleNextPage}
                  disabled={pagination.last || page >= pagination.totalPages - 1}
                  aria-label="Next page"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Create / Edit Issue Modal */}
      <IssueModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSaveIssue}
        projectId={projectId}
        project={project}
        initialData={editingIssue}
        sprints={projectSprints}
        members={projectMembers}
        isSubmitting={isSubmitting}
        serverError={modalError}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={Boolean(issueToDelete)}
        title="Delete Issue"
        message={`Are you sure you want to permanently delete "${issueToDelete?.title}"? All associated comments and activities will also be permanently removed. This action cannot be undone.`}
        confirmLabel="Delete Issue"
        isDanger
        isConfirming={isDeleting}
        onConfirm={handleConfirmDelete}
        onClose={() => setIssueToDelete(null)}
      />
    </div>
  );
};

export default IssueList;
