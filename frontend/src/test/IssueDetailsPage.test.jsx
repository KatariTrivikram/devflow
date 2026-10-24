import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import IssueDetailsPage from '../pages/issues/IssueDetailsPage';
import issueService from '../services/issueService';
import projectService from '../services/projectService';
import sprintService from '../services/sprintService';
import projectMemberService from '../services/projectMemberService';
import userService from '../services/userService';
import commentService from '../services/commentService';
import activityService from '../services/activityService';
import { useAuth } from '../hooks/useAuth';
import { USER_ROLES } from '../constants/roles';
import { ISSUE_TYPE, ISSUE_PRIORITY, ISSUE_STATUS } from '../constants/issueEnums';

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('IssueDetailsPage Component', () => {
  const issueId = 'issue-1234';
  const projectId = 'proj-5678';

  const mockIssue = {
    id: issueId,
    projectId,
    title: 'Resolve connection pool exhaustion',
    description: 'Configure HikariCP maximumPoolSize to match available DB connections.',
    type: ISSUE_TYPE.BUG,
    priority: ISSUE_PRIORITY.HIGH,
    status: ISSUE_STATUS.IN_PROGRESS,
    sprintId: 'sprint-1',
    assignedToId: 'user-1',
    createdAt: '2026-10-15T09:00:00Z',
    updatedAt: '2026-10-15T11:00:00Z',
  };

  const mockProject = {
    id: projectId,
    name: 'DevFlow Infrastructure',
    createdById: 'pm-user-1',
  };

  const mockSprints = [
    { id: 'sprint-1', name: 'Sprint 1 - Core Services' },
  ];

  const mockMembers = [
    {
      userId: 'user-1',
      user: { id: 'user-1', name: 'Alice Developer', email: 'alice@example.com' },
    },
    {
      userId: 'pm-user-1',
      user: { id: 'pm-user-1', name: 'Bob Manager', email: 'bob@example.com' },
    },
  ];

  const emptyCommentsPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };

  const emptyActivityPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: {
        id: 'pm-user-1',
        role: USER_ROLES.PROJECT_MANAGER,
      },
    });

    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValue(emptyCommentsPage);
    vi.spyOn(activityService, 'getActivitiesByIssue').mockResolvedValue(emptyActivityPage);
    vi.spyOn(projectService, 'getProjectById').mockResolvedValue(mockProject);
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValue({ content: mockSprints });
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValue(mockMembers);
    vi.spyOn(userService, 'getUserById').mockImplementation((uid) =>
      Promise.resolve({
        id: uid,
        name: uid === 'user-1' ? 'Alice Developer' : 'Bob Manager',
        email: uid === 'user-1' ? 'alice@example.com' : 'bob@example.com',
      })
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = (id = issueId) => {
    return render(
      <MemoryRouter initialEntries={[`/issues/${id}`]}>
        <Routes>
          <Route path="/issues/:issueId" element={<IssueDetailsPage />} />
          <Route path="/projects/:projectId" element={<div>Project Details Page Mock</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders loading state initially', () => {
    vi.spyOn(issueService, 'getIssueById').mockReturnValue(new Promise(() => {}));

    renderComponent();

    expect(screen.getByText('Loading issue details...')).toBeInTheDocument();
  });

  it('renders issue details, metadata, badges, and project back link', async () => {
    vi.spyOn(issueService, 'getIssueById').mockResolvedValueOnce(mockIssue);

    renderComponent();

    await waitFor(() => {
      expect(issueService.getIssueById).toHaveBeenCalledWith(issueId);
      expect(projectService.getProjectById).toHaveBeenCalledWith(projectId);
      expect(screen.getByText('Resolve connection pool exhaustion')).toBeInTheDocument();
      expect(screen.getByText('Configure HikariCP maximumPoolSize to match available DB connections.')).toBeInTheDocument();
      expect(screen.getByText('Bug')).toBeInTheDocument();
      expect(screen.getByText('High')).toBeInTheDocument();
      expect(screen.getByText('In Progress')).toBeInTheDocument();
      expect(screen.getByText('Sprint 1 - Core Services')).toBeInTheDocument();
      expect(screen.getByText('Alice Developer')).toBeInTheDocument();
      expect(screen.getByText(/DevFlow Infrastructure/i)).toBeInTheDocument();
    });

    // Check back link
    const backLink = screen.getByRole('link', { name: /back to/i });
    expect(backLink).toHaveAttribute('href', `/projects/${projectId}`);
  });

  it('renders error state on 404 Not Found', async () => {
    vi.spyOn(issueService, 'getIssueById').mockRejectedValueOnce({
      normalized: { status: 404, message: 'Issue not found.' },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Issue Not Found')).toBeInTheDocument();
      expect(screen.getByText('The requested issue could not be found or has been removed.')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: '← Back to Projects' })).toBeInTheDocument();
    });
  });

  it('allows authorized user to open edit modal and update issue', async () => {
    vi.spyOn(issueService, 'getIssueById').mockResolvedValue(mockIssue);

    const updatedIssue = {
      ...mockIssue,
      title: 'Resolve connection pool exhaustion in HikariCP',
    };
    vi.spyOn(issueService, 'updateIssue').mockResolvedValueOnce(updatedIssue);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Resolve connection pool exhaustion')).toBeInTheDocument();
    });

    const editBtn = screen.getByRole('button', { name: 'Edit Issue' });
    fireEvent.click(editBtn);

    // IssueModal opens
    expect(screen.getByRole('heading', { name: 'Edit Issue' })).toBeInTheDocument();

    const titleInput = screen.getByLabelText(/Title/i);
    fireEvent.change(titleInput, { target: { value: 'Resolve connection pool exhaustion in HikariCP' } });

    const saveBtn = screen.getByRole('button', { name: 'Save Changes' });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(issueService.updateIssue).toHaveBeenCalledWith(issueId, expect.objectContaining({
        title: 'Resolve connection pool exhaustion in HikariCP',
      }));
      expect(screen.getByText('Issue updated successfully.')).toBeInTheDocument();
      expect(screen.getByText('Resolve connection pool exhaustion in HikariCP')).toBeInTheDocument();
    });
  });

  it('renders workflow progression stepper and transitions issue status when clicked', async () => {
    vi.spyOn(issueService, 'getIssueById').mockResolvedValue(mockIssue);
    const updateSpy = vi.spyOn(issueService, 'updateIssue').mockResolvedValueOnce({
      ...mockIssue,
      status: ISSUE_STATUS.CODE_REVIEW,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('workflow-stepper')).toBeInTheDocument();
      expect(screen.getByTestId('workflow-step-TODO')).toBeInTheDocument();
      expect(screen.getByTestId('workflow-step-IN_PROGRESS')).toBeInTheDocument();
      expect(screen.getByTestId('workflow-step-CODE_REVIEW')).toBeInTheDocument();
      expect(screen.getByTestId('workflow-step-DONE')).toBeInTheDocument();
    });

    const moveBtn = screen.getByRole('button', { name: 'Move to Code Review' });
    fireEvent.click(moveBtn);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(issueId, { status: ISSUE_STATUS.CODE_REVIEW });
      expect(screen.getByText('Status moved to Code Review.')).toBeInTheDocument();
    });
  });

  it('hides Edit Issue button when user does not have permission', async () => {
    useAuth.mockReturnValue({
      user: {
        id: 'unrelated-user',
        role: USER_ROLES.DEVELOPER,
      },
    });

    vi.spyOn(issueService, 'getIssueById').mockResolvedValueOnce(mockIssue);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Resolve connection pool exhaustion')).toBeInTheDocument();
    });

    expect(screen.queryByRole('button', { name: 'Edit Issue' })).not.toBeInTheDocument();
  });
});
