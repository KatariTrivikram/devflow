import React from 'react';
import { render as rtlRender, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import IssueList from '../components/projects/IssueList';

const render = (ui, options) => rtlRender(ui, { wrapper: MemoryRouter, ...options });
import issueService from '../services/issueService';
import sprintService from '../services/sprintService';
import projectMemberService from '../services/projectMemberService';
import userService from '../services/userService';
import { useAuth } from '../hooks/useAuth';
import {
  ISSUE_STATUS,
  ISSUE_PRIORITY,
  ISSUE_TYPE,
} from '../constants/issueEnums';
import { USER_ROLES } from '../constants/roles';

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('IssueList Component', () => {
  const projectId = 'proj-123';
  const project = {
    id: projectId,
    name: 'DevFlow Platform',
    createdById: 'pm-user-id',
  };

  const mockSprint1 = {
    id: 'sprint-1',
    name: 'Sprint 1',
    status: 'ACTIVE',
  };

  const mockMember1 = {
    userId: 'dev-user-id',
    userName: 'Dave Developer',
    userEmail: 'dave@example.com',
  };

  const mockIssue1 = {
    id: 'issue-1',
    projectId,
    sprintId: 'sprint-1',
    assignedToId: 'dev-user-id',
    title: 'Fix issue filtering bug',
    description: 'Ensure query params are parsed safely',
    type: ISSUE_TYPE.BUG,
    priority: ISSUE_PRIORITY.HIGH,
    status: ISSUE_STATUS.IN_PROGRESS,
    createdAt: '2026-10-14T10:00:00Z',
    updatedAt: '2026-10-14T11:00:00Z',
  };

  const mockIssue2 = {
    id: 'issue-2',
    projectId,
    sprintId: null,
    assignedToId: 'other-user-id',
    title: 'Design database index strategy',
    description: 'Add composite indexes for query speed',
    type: ISSUE_TYPE.TASK,
    priority: ISSUE_PRIORITY.MEDIUM,
    status: ISSUE_STATUS.TODO,
    createdAt: '2026-10-14T12:00:00Z',
    updatedAt: '2026-10-14T12:00:00Z',
  };

  const emptyPage = {
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };

  const populatedPage = {
    content: [mockIssue1, mockIssue2],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    first: true,
    last: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: {
        id: 'pm-user-id',
        role: USER_ROLES.PROJECT_MANAGER,
      },
    });

    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValue({
      content: [mockSprint1],
    });
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValue([mockMember1]);
    vi.spyOn(userService, 'getUserById').mockImplementation((id) =>
      Promise.resolve({ id, name: id === 'dev-user-id' ? 'Dave Developer' : 'User ' + id })
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially', async () => {
    vi.spyOn(issueService, 'getIssues').mockImplementationOnce(
      () => new Promise(() => {}) // pending
    );

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    expect(screen.getByText('Loading issues...')).toBeInTheDocument();
  });

  it('renders empty state when no issues exist', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(emptyPage);

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('No issues created yet')).toBeInTheDocument();
      expect(document.getElementById('btn-create-issue')).toBeInTheDocument();
    });
  });

  it('renders issues table with details, badges, and action buttons for managing PM', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedPage);

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
      expect(screen.getByText('Design database index strategy')).toBeInTheDocument();
      expect(screen.getByLabelText('Edit Fix issue filtering bug')).toBeInTheDocument();
      expect(screen.getByLabelText('Delete Fix issue filtering bug')).toBeInTheDocument();
      expect(screen.getByLabelText('Edit Design database index strategy')).toBeInTheDocument();
      expect(screen.getByLabelText('Delete Design database index strategy')).toBeInTheDocument();
    });
  });

  it('restricts actions for a developer: can edit only assigned issue and cannot delete', async () => {
    useAuth.mockReturnValue({
      user: {
        id: 'dev-user-id',
        role: USER_ROLES.DEVELOPER,
      },
    });

    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedPage);

    render(<IssueList projectId={projectId} project={project} canManageProject={false} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
    });

    // Developer cannot create issues
    expect(document.getElementById('btn-create-issue')).not.toBeInTheDocument();

    // Developer can edit issue assigned to them
    expect(screen.getByLabelText('Edit Fix issue filtering bug')).toBeInTheDocument();
    expect(screen.queryByLabelText('Delete Fix issue filtering bug')).not.toBeInTheDocument();

    // Developer cannot edit or delete issue assigned to another user
    expect(screen.queryByLabelText('Edit Design database index strategy')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Delete Design database index strategy')).not.toBeInTheDocument();
  });

  it('filters issues when filter selects change', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce({
        content: [mockIssue1],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('Filter issues by status'), {
      target: { value: ISSUE_STATUS.IN_PROGRESS },
    });

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith({
        projectId,
        status: ISSUE_STATUS.IN_PROGRESS,
        priority: undefined,
        type: undefined,
        sprintId: undefined,
        assignedToId: undefined,
        search: undefined,
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      });
    });
  });

  it('submits search form and clears filters', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce({ content: [mockIssue1], totalElements: 1, totalPages: 1 })
      .mockResolvedValueOnce(populatedPage);

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText('Search issues');
    fireEvent.change(searchInput, { target: { value: 'filtering' } });
    fireEvent.submit(searchInput.closest('form'));

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ search: 'filtering' })
      );
    });

    // Clear filters
    const clearBtn = screen.getByRole('button', { name: 'Clear filters' });
    fireEvent.click(clearBtn);

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ search: undefined, status: undefined })
      );
    });
  });

  it('opens create modal, creates an issue, and refreshes the list', async () => {
    vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(emptyPage)
      .mockResolvedValueOnce(populatedPage);

    const createSpy = vi.spyOn(issueService, 'createIssue').mockResolvedValueOnce(mockIssue1);

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('No issues created yet')).toBeInTheDocument();
    });

    fireEvent.click(document.getElementById('btn-create-issue'));

    expect(screen.getByRole('heading', { name: 'Create New Issue' })).toBeInTheDocument();

    fireEvent.change(document.getElementById('issue-title'), {
      target: { value: 'Fix issue filtering bug' },
    });
    fireEvent.change(document.getElementById('issue-type'), {
      target: { value: ISSUE_TYPE.BUG },
    });
    fireEvent.change(document.getElementById('issue-priority'), {
      target: { value: ISSUE_PRIORITY.HIGH },
    });
    fireEvent.change(document.getElementById('issue-status'), {
      target: { value: ISSUE_STATUS.IN_PROGRESS },
    });

    // Modal submit button is labeled 'Create Issue'
    const modalSubmitBtn = screen.getAllByRole('button', { name: 'Create Issue' }).pop();
    fireEvent.click(modalSubmitBtn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        projectId,
        title: 'Fix issue filtering bug',
        description: null,
        type: ISSUE_TYPE.BUG,
        priority: ISSUE_PRIORITY.HIGH,
        status: ISSUE_STATUS.IN_PROGRESS,
        sprintId: null,
        assignedToId: null,
      });
      expect(screen.getByText('Issue created successfully.')).toBeInTheDocument();
    });
  });

  it('opens edit modal, updates an issue, and refreshes the list', async () => {
    vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce(populatedPage);

    const updateSpy = vi.spyOn(issueService, 'updateIssue').mockResolvedValueOnce({
      ...mockIssue1,
      title: 'Fix issue filtering bug updated',
    });

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Edit Fix issue filtering bug'));

    expect(screen.getByRole('heading', { name: 'Edit Issue' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/title/i), {
      target: { value: 'Fix issue filtering bug updated' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('issue-1', {
        title: 'Fix issue filtering bug updated',
        description: 'Ensure query params are parsed safely',
        type: ISSUE_TYPE.BUG,
        priority: ISSUE_PRIORITY.HIGH,
        status: ISSUE_STATUS.IN_PROGRESS,
        sprintId: 'sprint-1',
        assignedToId: 'dev-user-id',
      });
      expect(screen.getByText('Issue updated successfully.')).toBeInTheDocument();
    });
  });

  it('opens delete confirmation and deletes issue', async () => {
    vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce(emptyPage);

    const deleteSpy = vi.spyOn(issueService, 'deleteIssue').mockResolvedValueOnce(null);

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Fix issue filtering bug')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Delete Fix issue filtering bug'));

    expect(screen.getByRole('heading', { name: 'Delete Issue' })).toBeInTheDocument();
    expect(
      screen.getByText(/are you sure you want to permanently delete "fix issue filtering bug"\?/i)
    ).toBeInTheDocument();

    // Confirm inside modal
    const confirmDeleteBtn = screen.getAllByRole('button', { name: 'Delete Issue' }).pop();
    fireEvent.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('issue-1');
      expect(screen.getByText('Issue "Fix issue filtering bug" was deleted.')).toBeInTheDocument();
    });
  });

  it('filters issues using Quick Filter Pills', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce({ content: [mockIssue1], totalElements: 1, totalPages: 1 })
      .mockResolvedValueOnce({ content: [mockIssue2], totalElements: 1, totalPages: 1 });

    render(
      <IssueList
        projectId={projectId}
        project={project}
        canManageProject={true}
        sprints={[mockSprint1]}
        members={[mockMember1]}
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('issue-quick-filters')).toBeInTheDocument();
      expect(screen.getByTestId('pill-filter-all')).toBeInTheDocument();
      expect(screen.getByTestId('pill-filter-current-sprint')).toBeInTheDocument();
      expect(screen.getByTestId('pill-filter-no-sprint')).toBeInTheDocument();
    });

    // Click Current Sprint pill
    fireEvent.click(screen.getByTestId('pill-filter-current-sprint'));

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ sprintId: 'sprint-1' })
      );
    });

    // Click No Sprint pill
    fireEvent.click(screen.getByTestId('pill-filter-no-sprint'));

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ noSprint: true })
      );
    });
  });

  it('displays error state when loading issues fails', async () => {
    vi.spyOn(issueService, 'getIssues').mockRejectedValueOnce({
      normalized: { status: 500, message: 'Server error loading issues.' },
    });

    render(<IssueList projectId={projectId} project={project} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText('Server error loading issues.')).toBeInTheDocument();
    });
  });
});
