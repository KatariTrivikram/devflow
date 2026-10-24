import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import ProjectDetailsPage from '../pages/projects/ProjectDetailsPage';
import projectService from '../services/projectService';
import projectMemberService from '../services/projectMemberService';
import sprintService from '../services/sprintService';
import issueService from '../services/issueService';
import { PROJECT_STATUS } from '../constants/projectEnums';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ProjectDetailsPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValue([]);
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });
    vi.spyOn(issueService, 'getIssues').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockProject = {
    id: 'proj-1234',
    name: 'DevFlow Infrastructure',
    description: 'DevFlow Core Infrastructure and Orchestration',
    status: PROJECT_STATUS.ACTIVE,
    createdById: 'user-creator-id',
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-12T15:30:00Z',
  };

  const renderComponent = (projectId = 'proj-1234') => {
    return render(
      <MemoryRouter initialEntries={[`/projects/${projectId}`]}>
        <AuthProvider>
          <Routes>
            <Route path="/projects/:projectId" element={<ProjectDetailsPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );
  };

  it('renders project details, metadata, and status on successful fetch', async () => {
    vi.spyOn(projectService, 'getProjectById').mockResolvedValueOnce(mockProject);
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValue({
      content: [
        {
          id: 'sprint-1',
          name: 'Sprint 1 - Foundation',
          status: 'ACTIVE',
          startDate: '2026-10-10T00:00:00Z',
          endDate: '2026-10-24T00:00:00Z',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderComponent();

    expect(screen.getByText(/loading project details/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'DevFlow Infrastructure' })).toBeInTheDocument();
      expect(screen.getByText('DevFlow Core Infrastructure and Orchestration')).toBeInTheDocument();
      expect(screen.getByText('proj-1234')).toBeInTheDocument();
      expect(screen.getAllByText('Sprint 1 - Foundation').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByRole('heading', { name: 'Issues & Backlog' })).toBeInTheDocument();
    });
  });

  it('handles 404 Project Not Found error state', async () => {
    vi.spyOn(projectService, 'getProjectById').mockRejectedValueOnce({
      normalized: {
        status: 404,
        message: 'Project not found with id: proj-1234',
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /project not found/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /return to projects list/i })).toBeInTheDocument();
    });
  });

  it('handles 403 Forbidden Access Denied error state', async () => {
    vi.spyOn(projectService, 'getProjectById').mockRejectedValueOnce({
      normalized: {
        status: 403,
        message: 'Access denied: You do not have permission to view this project',
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /access denied/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /return to projects list/i })).toBeInTheDocument();
    });
  });

  it('opens edit modal and updates project details successfully', async () => {
    vi.spyOn(projectService, 'getProjectById').mockResolvedValueOnce(mockProject);

    const updateSpy = vi.spyOn(projectService, 'updateProject').mockResolvedValueOnce({
      ...mockProject,
      name: 'DevFlow Infrastructure v2',
      description: 'Updated infrastructure scope',
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'DevFlow Infrastructure' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Edit Project' }));

    expect(screen.getByRole('heading', { name: 'Edit Project' })).toBeInTheDocument();
    expect(screen.getByLabelText(/project name/i)).toHaveValue('DevFlow Infrastructure');

    fireEvent.change(screen.getByLabelText(/project name/i), {
      target: { value: 'DevFlow Infrastructure v2' },
    });
    fireEvent.change(screen.getByLabelText(/^description/i), {
      target: { value: 'Updated infrastructure scope' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('proj-1234', {
        name: 'DevFlow Infrastructure v2',
        description: 'Updated infrastructure scope',
        status: PROJECT_STATUS.ACTIVE,
      });
      expect(screen.getByText('Project updated successfully.')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'DevFlow Infrastructure v2' })).toBeInTheDocument();
    });
  });

  it('prompts delete confirmation modal and navigates away on deletion', async () => {
    vi.spyOn(projectService, 'getProjectById').mockResolvedValueOnce(mockProject);

    const deleteSpy = vi.spyOn(projectService, 'deleteProject').mockResolvedValueOnce(null);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'DevFlow Infrastructure' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Delete Project' }));

    expect(screen.getByRole('heading', { name: 'Delete Project' })).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to permanently delete "devflow infrastructure"\?/i)).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole('button', { name: 'Delete Project' });
    fireEvent.click(deleteButtons[deleteButtons.length - 1]);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('proj-1234');
      expect(mockNavigate).toHaveBeenCalledWith('/projects', {
        replace: true,
        state: { message: 'Project "DevFlow Infrastructure" was successfully deleted.' },
      });
    });
  });

  it('toggles between List View and Kanban Board View', async () => {
    vi.spyOn(projectService, 'getProjectById').mockResolvedValueOnce(mockProject);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Issues & Backlog' })).toBeInTheDocument();
    });

    const boardTab = screen.getByRole('tab', { name: /board view/i });
    fireEvent.click(boardTab);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Kanban Board' })).toBeInTheDocument();
    });

    const listTab = screen.getByRole('tab', { name: /list view/i });
    fireEvent.click(listTab);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Issues & Backlog' })).toBeInTheDocument();
    });
  });

  it('renders ProjectSummaryCards, ActiveSprintCard, and RecentActivity audit trail', async () => {
    vi.spyOn(projectService, 'getProjectById').mockResolvedValueOnce(mockProject);
    vi.spyOn(projectService, 'getProjectSummary').mockResolvedValueOnce({
      totalIssues: 12,
      openIssues: 5,
      inProgressIssues: 4,
      codeReviewIssues: 1,
      doneIssues: 2,
      memberCount: 4,
      activeSprint: {
        id: 'sprint-active-1',
        name: 'Sprint 1 - Foundation',
        status: 'ACTIVE',
        startDate: '2026-10-10T00:00:00Z',
        endDate: '2026-10-24T00:00:00Z',
        totalIssues: 10,
        completedIssues: 2,
        inProgressIssues: 4,
        todoIssues: 4,
        progressPercentage: 20,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('project-summary-cards')).toBeInTheDocument();
      expect(screen.getByTestId('summary-card-total-issues')).toBeInTheDocument();
      expect(screen.getByTestId('active-sprint-card')).toBeInTheDocument();
      expect(screen.getByTestId('sprint-progress-pct')).toHaveTextContent('20%');
      expect(screen.getByTestId('recent-activity-panel')).toBeInTheDocument();
    });
  });
});
