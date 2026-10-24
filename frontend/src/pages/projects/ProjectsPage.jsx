import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import projectService from '../../services/projectService';
import ProjectStatusBadge from '../../components/projects/ProjectStatusBadge';
import ProjectModal from '../../components/projects/ProjectModal';
import ConfirmModal from '../../components/common/ConfirmModal';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import EmptyState from '../../components/common/EmptyState';
import { PROJECT_STATUS } from '../../constants/projectEnums';
import { formatDate } from '../../utils/date';

const PAGE_SIZE = 10;

export const ProjectsPage = () => {
  const navigate = useNavigate();

  // Project list & pagination state
  const [projects, setProjects] = useState([]);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  // Filter & Search state
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [sortBy, setSortBy] = useState('createdAt,desc');
  const [refreshIndex, setRefreshIndex] = useState(0);

  // Loading & Error states
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Delete confirmation state
  const [deletingProject, setDeletingProject] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [actionFeedback, setActionFeedback] = useState(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(0);
    }, 300);

    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Fetch projects from backend
  useEffect(() => {
    let ignore = false;

    async function loadProjects() {
      try {
        const data = await projectService.getProjects({
          status: statusFilter || undefined,
          search: debouncedSearch || undefined,
          page,
          size: PAGE_SIZE,
          sort: sortBy,
        });

        if (ignore) return;
        setProjects(data.content || []);
        setPagination({
          page: data.page ?? page,
          size: data.size ?? PAGE_SIZE,
          totalElements: data.totalElements ?? 0,
          totalPages: data.totalPages ?? 0,
          first: data.first ?? true,
          last: data.last ?? true,
        });
      } catch (err) {
        if (!ignore) {
          setFetchError(err.normalized || {
            message: 'Failed to load projects. Please try again.',
          });
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadProjects();

    return () => {
      ignore = true;
    };
  }, [statusFilter, debouncedSearch, sortBy, page, refreshIndex]);

  // Filter change handlers
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

  const handleClearFilters = () => {
    setIsLoading(true);
    setSearchQuery('');
    setDebouncedSearch('');
    setStatusFilter('');
    setSortBy('createdAt,desc');
    setPage(0);
  };

  const handleRetry = () => {
    setIsLoading(true);
    setFetchError(null);
    setRefreshIndex((idx) => idx + 1);
  };

  // Modal open handlers
  const handleOpenCreateModal = () => {
    setEditingProject(null);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (project) => {
    setEditingProject(project);
    setModalError(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    if (!isSubmitting) {
      setIsModalOpen(false);
      setEditingProject(null);
      setModalError(null);
    }
  };

  // Create or Update submit handler
  const handleSaveProject = async (formData) => {
    setIsSubmitting(true);
    setModalError(null);

    try {
      if (editingProject) {
        await projectService.updateProject(editingProject.id, formData);
        setIsModalOpen(false);
        setEditingProject(null);
        setActionFeedback({ type: 'success', message: 'Project updated successfully.' });
        setIsLoading(true);
        setRefreshIndex((idx) => idx + 1);
      } else {
        const created = await projectService.createProject(formData);
        setIsModalOpen(false);
        setActionFeedback({ type: 'success', message: 'Project created successfully.' });
        navigate(`/projects/${created.id}`);
      }
    } catch (err) {
      setModalError(err.normalized || {
        status: 500,
        message: 'An unexpected error occurred while saving the project.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Delete project handlers
  const handlePromptDelete = (project) => {
    setDeletingProject(project);
  };

  const handleCancelDelete = () => {
    if (!isDeleting) {
      setDeletingProject(null);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingProject) return;

    setIsDeleting(true);
    try {
      await projectService.deleteProject(deletingProject.id);
      setDeletingProject(null);
      setActionFeedback({ type: 'success', message: 'Project deleted successfully.' });
      setIsLoading(true);
      setPage(0);
      setRefreshIndex((idx) => idx + 1);
    } catch (err) {
      setActionFeedback({
        type: 'error',
        message: err.normalized?.message || 'Failed to delete project.',
      });
      setDeletingProject(null);
    } finally {
      setIsDeleting(false);
    }
  };

  // Pagination navigation
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

  const isFiltering = Boolean(statusFilter || debouncedSearch);

  return (
    <div className="df-page">
      {/* Page Header */}
      <div className="df-page-header">
        <div>
          <h1 className="df-page-title">Projects</h1>
          <p className="df-page-subtitle">
            Manage your workspace projects, track statuses, and coordinate development cycles.
          </p>
        </div>
        <button
          type="button"
          onClick={handleOpenCreateModal}
          className="df-btn df-btn-primary"
          id="btn-create-project"
        >
          <svg
            style={{ width: '16px', height: '16px', marginRight: '6px' }}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Project
        </button>
      </div>

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
            aria-label="Dismiss message"
          >
            ✕
          </button>
        </div>
      )}

      {/* Toolbar: Search, Status Filter, Sort */}
      <div className="df-toolbar">
        <div className="df-toolbar-filters">
          <div className="df-search-wrapper">
            <svg
              className="df-search-icon"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            <input
              type="text"
              className="df-input df-search-input"
              placeholder="Search by name or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search projects"
            />
          </div>

          <select
            className="df-select"
            value={statusFilter}
            onChange={handleStatusChange}
            aria-label="Filter by status"
            style={{ maxWidth: '170px' }}
          >
            <option value="">All Statuses</option>
            <option value={PROJECT_STATUS.ACTIVE}>Active</option>
            <option value={PROJECT_STATUS.ARCHIVED}>Archived</option>
            <option value={PROJECT_STATUS.COMPLETED}>Completed</option>
          </select>

          <select
            className="df-select"
            value={sortBy}
            onChange={handleSortChange}
            aria-label="Sort projects"
            style={{ maxWidth: '190px' }}
          >
            <option value="createdAt,desc">Newest First</option>
            <option value="createdAt,asc">Oldest First</option>
            <option value="name,asc">Name (A-Z)</option>
            <option value="name,desc">Name (Z-A)</option>
            <option value="status,asc">Status</option>
          </select>

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
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <div className="df-section-card">
          <LoadingState message="Loading projects..." />
        </div>
      ) : fetchError ? (
        <div className="df-section-card">
          <ErrorState
            title="Unable to load projects"
            message={fetchError.message}
            onRetry={handleRetry}
          />
        </div>
      ) : projects.length === 0 ? (
        <div className="df-section-card">
          {isFiltering ? (
            <EmptyState
              title="No matching projects"
              description="No projects match your current search or filter criteria. Try broadening your parameters."
              actionLabel="Clear Filters"
              onAction={handleClearFilters}
            />
          ) : (
            <EmptyState
              title="No projects yet"
              description="DevFlow has no projects registered for your organization. Create your first project to begin scheduling sprints and assigning issues."
              actionLabel="Create Project"
              onAction={handleOpenCreateModal}
            />
          )}
        </div>
      ) : (
        <>
          <div className="df-table-container">
            <table className="df-table" aria-label="Projects list">
              <thead>
                <tr>
                  <th className="df-th" scope="col">Project Name</th>
                  <th className="df-th" scope="col">Status</th>
                  <th className="df-th" scope="col">Created Date</th>
                  <th className="df-th" scope="col" style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {projects.map((project) => (
                  <tr key={project.id} className="df-tr">
                    <td className="df-td">
                      <Link
                        to={`/projects/${project.id}`}
                        className="df-project-link"
                      >
                        {project.name}
                      </Link>
                      {project.description && (
                        <div className="df-project-desc-clamp">
                          {project.description}
                        </div>
                      )}
                    </td>
                    <td className="df-td">
                      <ProjectStatusBadge status={project.status} />
                    </td>
                    <td className="df-td" style={{ color: 'var(--df-text-muted)', fontSize: '0.8125rem' }}>
                      {formatDate(project.createdAt)}
                    </td>
                    <td className="df-td">
                      <div className="df-td-actions">
                        <Link
                          to={`/projects/${project.id}`}
                          className="df-btn df-btn-secondary df-btn-sm"
                          aria-label={`View details for ${project.name}`}
                        >
                          View
                        </Link>
                        <button
                          type="button"
                          onClick={() => handleOpenEditModal(project)}
                          className="df-btn df-btn-secondary df-btn-sm"
                          aria-label={`Edit ${project.name}`}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => handlePromptDelete(project)}
                          className="df-btn df-btn-danger-outline df-btn-sm"
                          aria-label={`Delete ${project.name}`}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Real Pagination Controls */}
          <div className="df-pagination">
            <div className="df-pagination-info">
              Showing {pagination.page * pagination.size + 1} to{' '}
              {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
              {pagination.totalElements} projects
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
        </>
      )}

      {/* Create / Edit Project Modal */}
      <ProjectModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSaveProject}
        initialData={editingProject}
        isSubmitting={isSubmitting}
        serverError={modalError}
      />

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={Boolean(deletingProject)}
        title="Delete Project"
        message={`Are you sure you want to delete "${deletingProject?.name}"? All associated sprints, issues, and member records will be permanently removed. This action cannot be undone.`}
        confirmLabel="Delete Project"
        isDanger
        isConfirming={isDeleting}
        onConfirm={handleConfirmDelete}
        onClose={handleCancelDelete}
      />
    </div>
  );
};

export default ProjectsPage;
