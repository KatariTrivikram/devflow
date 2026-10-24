import React, { useState, useEffect } from 'react';
import sprintService from '../../services/sprintService';
import SprintStatusBadge from './SprintStatusBadge';
import SprintModal from './SprintModal';
import ConfirmModal from '../common/ConfirmModal';
import LoadingState from '../common/LoadingState';
import ErrorState from '../common/ErrorState';
import EmptyState from '../common/EmptyState';
import { SPRINT_STATUS } from '../../constants/projectEnums';
import { formatDate } from '../../utils/date';

const SPRINT_PAGE_SIZE = 10;

export const SprintList = ({ projectId, canManageSprints = false }) => {
  const [sprints, setSprints] = useState([]);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: SPRINT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  const [statusFilter, setStatusFilter] = useState('');
  const [sortBy, setSortBy] = useState('startDate,asc');
  const [refreshIndex, setRefreshIndex] = useState(0);

  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Create / Edit modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingSprint, setEditingSprint] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Delete modal state
  const [sprintToDelete, setSprintToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Feedback alert state
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    let ignore = false;

    async function loadSprints() {
      try {
        const data = await sprintService.getProjectSprints(projectId, {
          status: statusFilter || undefined,
          page,
          size: SPRINT_PAGE_SIZE,
          sort: sortBy,
        });

        if (ignore) return;
        setSprints(data.content || []);
        setPagination({
          page: data.page ?? page,
          size: data.size ?? SPRINT_PAGE_SIZE,
          totalElements: data.totalElements ?? 0,
          totalPages: data.totalPages ?? 0,
          first: data.first ?? true,
          last: data.last ?? true,
        });
      } catch (err) {
        if (!ignore) {
          setFetchError(err.normalized || {
            status: 500,
            message: 'Failed to load project sprints.',
          });
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadSprints();

    return () => {
      ignore = true;
    };
  }, [projectId, statusFilter, sortBy, page, refreshIndex]);

  const handleStatusChange = (e) => {
    setIsLoading(true);
    setStatusFilter(e.target.value);
    setPage(0);
  };

  const handleSortChange = (e) => {
    setIsLoading(true);
    setSortBy(e.target.value);
    setPage(0);
  };

  const handleClearFilter = () => {
    setIsLoading(true);
    setStatusFilter('');
    setPage(0);
  };

  const handleRetry = () => {
    setIsLoading(true);
    setFetchError(null);
    setRefreshIndex((idx) => idx + 1);
  };

  const handleOpenCreateModal = () => {
    setEditingSprint(null);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (sprint) => {
    setEditingSprint(sprint);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (!isSubmitting) {
      setIsModalOpen(false);
      setEditingSprint(null);
      setModalError(null);
    }
  };

  const handleSaveSprint = async (formData) => {
    setIsSubmitting(true);
    setModalError(null);

    try {
      if (editingSprint) {
        await sprintService.updateSprint(editingSprint.id, formData);
        setIsModalOpen(false);
        setEditingSprint(null);
        setFeedback({ type: 'success', message: 'Sprint updated successfully.' });
        setIsLoading(true);
        setRefreshIndex((idx) => idx + 1);
      } else {
        await sprintService.createSprint(formData);
        setIsModalOpen(false);
        setFeedback({ type: 'success', message: 'Sprint created successfully.' });
        setIsLoading(true);
        setPage(0);
        setRefreshIndex((idx) => idx + 1);
      }
    } catch (err) {
      setModalError(err.normalized || {
        status: 500,
        message: 'An unexpected error occurred while saving the sprint.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!sprintToDelete) return;

    setIsDeleting(true);
    try {
      await sprintService.deleteSprint(sprintToDelete.id);
      const deletedName = sprintToDelete.name;
      setSprintToDelete(null);
      setFeedback({ type: 'success', message: `Sprint "${deletedName}" was deleted.` });
      setIsLoading(true);
      setRefreshIndex((idx) => idx + 1);
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to delete sprint.',
      });
      setSprintToDelete(null);
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

  const isFiltering = Boolean(statusFilter);

  return (
    <div className="df-section-card">
      {/* Section Header */}
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Sprints & Iterations
          </h2>
          <p className="df-section-subtitle">
            Active and planned development sprint cycles for this project.
          </p>
        </div>
        {canManageSprints && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleOpenCreateModal}
            id="btn-create-sprint"
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
            Create Sprint
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

      {/* Filter and Sort Toolbar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center', marginBottom: '1rem' }}>
        <select
          className="df-select"
          value={statusFilter}
          onChange={handleStatusChange}
          aria-label="Filter sprints by status"
          style={{ maxWidth: '170px' }}
        >
          <option value="">All Sprints</option>
          <option value={SPRINT_STATUS.PLANNED}>Planned</option>
          <option value={SPRINT_STATUS.ACTIVE}>Active</option>
          <option value={SPRINT_STATUS.COMPLETED}>Completed</option>
        </select>

        <select
          className="df-select"
          value={sortBy}
          onChange={handleSortChange}
          aria-label="Sort sprints"
          style={{ maxWidth: '190px' }}
        >
          <option value="startDate,asc">Start Date (Earliest)</option>
          <option value="startDate,desc">Start Date (Latest)</option>
          <option value="endDate,asc">End Date (Earliest)</option>
          <option value="name,asc">Name (A-Z)</option>
          <option value="status,asc">Status</option>
        </select>

        {isFiltering && (
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={handleClearFilter}
          >
            Clear filter
          </button>
        )}
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <LoadingState message="Loading sprints..." />
      ) : fetchError ? (
        <ErrorState
          title={fetchError.status === 403 ? 'Access Denied' : 'Unable to load sprints'}
          message={fetchError.message}
          onRetry={fetchError.status !== 403 ? handleRetry : null}
        />
      ) : sprints.length === 0 ? (
        isFiltering ? (
          <EmptyState
            title="No matching sprints"
            description="No sprints found matching the selected status filter."
            actionLabel="Clear Filter"
            onAction={handleClearFilter}
          />
        ) : (
          <EmptyState
            title="No sprints created yet"
            description="No sprints have been created for this project yet. Plan your first sprint iteration to organize issues and milestones."
            actionLabel={canManageSprints ? 'Create Sprint' : null}
            onAction={canManageSprints ? handleOpenCreateModal : null}
          />
        )
      ) : (
        <>
          <div className="df-table-container">
            <table className="df-table" aria-label="Project sprints">
              <thead>
                <tr>
                  <th className="df-th" scope="col">Sprint Name</th>
                  <th className="df-th" scope="col">Status</th>
                  <th className="df-th" scope="col">Start Date</th>
                  <th className="df-th" scope="col">End Date</th>
                  {canManageSprints && (
                    <th className="df-th" scope="col" style={{ textAlign: 'right' }}>Actions</th>
                  )}
                </tr>
              </thead>
              <tbody>
                {sprints.map((sprint) => (
                  <tr key={sprint.id} className="df-tr">
                    <td className="df-td" style={{ fontWeight: 600 }}>
                      {sprint.name}
                    </td>
                    <td className="df-td">
                      <SprintStatusBadge status={sprint.status} />
                    </td>
                    <td className="df-td" style={{ color: 'var(--df-text-muted)', fontSize: '0.8125rem' }}>
                      {formatDate(sprint.startDate)}
                    </td>
                    <td className="df-td" style={{ color: 'var(--df-text-muted)', fontSize: '0.8125rem' }}>
                      {formatDate(sprint.endDate)}
                    </td>
                    {canManageSprints && (
                      <td className="df-td" style={{ textAlign: 'right' }}>
                        <div className="df-td-actions">
                          <button
                            type="button"
                            className="df-btn df-btn-secondary df-btn-sm"
                            onClick={() => handleOpenEditModal(sprint)}
                            aria-label={`Edit ${sprint.name}`}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="df-btn df-btn-danger-outline df-btn-sm"
                            onClick={() => setSprintToDelete(sprint)}
                            aria-label={`Delete ${sprint.name}`}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {pagination.totalPages > 1 && (
            <div className="df-pagination">
              <div className="df-pagination-info">
                Showing {pagination.page * pagination.size + 1} to{' '}
                {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                {pagination.totalElements} sprints
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

      {/* Create / Edit Sprint Modal */}
      <SprintModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSaveSprint}
        projectId={projectId}
        initialData={editingSprint}
        isSubmitting={isSubmitting}
        serverError={modalError}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={Boolean(sprintToDelete)}
        title="Delete Sprint"
        message={`Are you sure you want to delete "${sprintToDelete?.name}"? Any issues assigned to this sprint will be unassigned from it. This action cannot be undone.`}
        confirmLabel="Delete Sprint"
        isDanger
        isConfirming={isDeleting}
        onConfirm={handleConfirmDelete}
        onClose={() => setSprintToDelete(null)}
      />
    </div>
  );
};

export default SprintList;
