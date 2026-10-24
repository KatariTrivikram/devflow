import React from 'react';
import { render as rtlRender, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import KanbanBoard from '../components/projects/kanban/KanbanBoard';

const render = (ui, options) => rtlRender(ui, { wrapper: MemoryRouter, ...options });
import issueService from '../services/issueService';
import sprintService from '../services/sprintService';
import projectMemberService from '../services/projectMemberService';
import { useAuth } from '../hooks/useAuth';
import { USER_ROLES } from '../constants/roles';

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));
import {
  ISSUE_STATUS,
  ISSUE_PRIORITY,
  ISSUE_TYPE,
} from '../constants/issueEnums';

describe('KanbanBoard Component', () => {
  const projectId = 'proj-kanban-123';
  const pmUserId = 'pm-user-id';
  const devUserId = 'dev-user-id';

  const mockProject = {
    id: projectId,
    name: 'Kanban Core Project',
    createdById: pmUserId,
  };

  const mockIssueTodo = {
    id: 'issue-todo-1',
    title: 'Setup Kanban scaffolding',
    description: 'Create column and card components',
    type: ISSUE_TYPE.TASK,
    priority: ISSUE_PRIORITY.HIGH,
    status: ISSUE_STATUS.TODO,
    sprintId: 'sprint-1',
    assignedToId: devUserId,
  };

  const mockIssueInProgress = {
    id: 'issue-inprogress-2',
    title: 'Implement drag and drop',
    description: 'Use native HTML5 drag events',
    type: ISSUE_TYPE.FEATURE,
    priority: ISSUE_PRIORITY.CRITICAL,
    status: ISSUE_STATUS.IN_PROGRESS,
    sprintId: 'sprint-1',
    assignedToId: pmUserId,
  };

  const mockIssueCodeReview = {
    id: 'issue-codereview-3',
    title: 'Accessible keyboard controls',
    description: 'Provide select dropdown fallback',
    type: ISSUE_TYPE.IMPROVEMENT,
    priority: ISSUE_PRIORITY.MEDIUM,
    status: ISSUE_STATUS.CODE_REVIEW,
    sprintId: null,
    assignedToId: devUserId,
  };

  const mockIssueDone = {
    id: 'issue-done-4',
    title: 'Define status enums',
    description: 'All 4 statuses mapped',
    type: ISSUE_TYPE.TASK,
    priority: ISSUE_PRIORITY.LOW,
    status: ISSUE_STATUS.DONE,
    sprintId: null,
    assignedToId: null,
  };

  const allIssues = [
    mockIssueTodo,
    mockIssueInProgress,
    mockIssueCodeReview,
    mockIssueDone,
  ];

  const emptyIssuesPage = {
    content: [],
    page: 0,
    size: 100,
    totalElements: 0,
    totalPages: 0,
  };

  const populatedIssuesPage = {
    content: allIssues,
    page: 0,
    size: 100,
    totalElements: allIssues.length,
    totalPages: 1,
  };

  const mockSprintsResponse = {
    content: [
      { id: 'sprint-1', name: 'Sprint 1 - Foundation' },
    ],
  };

  const mockMembersResponse = [
    {
      id: 'pm-member',
      userId: pmUserId,
      user: { id: pmUserId, name: 'Project Manager User', email: 'pm@example.com' },
    },
    {
      id: 'dev-member',
      userId: devUserId,
      user: { id: devUserId, name: 'Developer User', email: 'dev@example.com' },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValue(mockSprintsResponse);
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValue(mockMembersResponse);
    useAuth.mockReturnValue({
      user: { id: pmUserId, email: 'pm@example.com', role: USER_ROLES.PROJECT_MANAGER },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially', () => {
    vi.spyOn(issueService, 'getIssues').mockImplementation(() => new Promise(() => {}));

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    expect(screen.getByText('Loading board issues...')).toBeInTheDocument();
  });

  it('renders error state and retries fetch', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockRejectedValueOnce({ normalized: { status: 500, message: 'Database connection error' } })
      .mockResolvedValueOnce(populatedIssuesPage);

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Unable to load board')).toBeInTheDocument();
      expect(screen.getByText('Database connection error')).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });
  });

  it('renders empty state when no issues exist and not filtering', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(emptyIssuesPage);

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('No issues created yet')).toBeInTheDocument();
    });
  });

  it('renders all 4 columns and groups issues appropriately', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedIssuesPage);

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByRole('region', { name: 'To Do column' })).toBeInTheDocument();
      expect(screen.getByRole('region', { name: 'In Progress column' })).toBeInTheDocument();
      expect(screen.getByRole('region', { name: 'Code Review column' })).toBeInTheDocument();
      expect(screen.getByRole('region', { name: 'Done column' })).toBeInTheDocument();
    });

    // Verify issue placement in columns
    expect(screen.getByLabelText('1 issues in To Do')).toBeInTheDocument();
    expect(screen.getByLabelText('1 issues in In Progress')).toBeInTheDocument();
    expect(screen.getByLabelText('1 issues in Code Review')).toBeInTheDocument();
    expect(screen.getByLabelText('1 issues in Done')).toBeInTheDocument();

    expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    expect(screen.getByText('Implement drag and drop')).toBeInTheDocument();
    expect(screen.getByText('Accessible keyboard controls')).toBeInTheDocument();
    expect(screen.getByText('Define status enums')).toBeInTheDocument();
  });

  it('filters issues when priority and type filters change', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedIssuesPage)
      .mockResolvedValueOnce({ content: [mockIssueTodo], totalElements: 1 });

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('Filter board by priority'), {
      target: { value: ISSUE_PRIORITY.HIGH },
    });

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          priority: ISSUE_PRIORITY.HIGH,
        })
      );
    });
  });

  it('submits search query and clears filters', async () => {
    const getIssuesSpy = vi.spyOn(issueService, 'getIssues')
      .mockResolvedValueOnce(populatedIssuesPage)
      .mockResolvedValueOnce({ content: [mockIssueInProgress], totalElements: 1 })
      .mockResolvedValueOnce(populatedIssuesPage);

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText('Search board issues');
    fireEvent.change(searchInput, { target: { value: 'drag' } });
    fireEvent.submit(searchInput.closest('form'));

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          search: 'drag',
        })
      );
    });

    // Clear filters
    const clearBtn = screen.getByRole('button', { name: 'Clear board filters' });
    fireEvent.click(clearBtn);

    await waitFor(() => {
      expect(getIssuesSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          search: undefined,
          priority: undefined,
        })
      );
    });
  });

  it('moves issue status successfully with optimistic update', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedIssuesPage);
    const updateSpy = vi.spyOn(issueService, 'updateIssue').mockResolvedValueOnce({
      ...mockIssueTodo,
      status: ISSUE_STATUS.IN_PROGRESS,
    });

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });

    const statusSelector = screen.getByLabelText(
      /move issue setup kanban scaffolding to status/i
    );
    fireEvent.change(statusSelector, { target: { value: ISSUE_STATUS.IN_PROGRESS } });

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('issue-todo-1', {
        status: ISSUE_STATUS.IN_PROGRESS,
      });
      expect(
        screen.getByText('Moved "Setup Kanban scaffolding" to In Progress.')
      ).toBeInTheDocument();
    });
  });

  it('reverts optimistic update and displays error when status update fails', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedIssuesPage);
    vi.spyOn(issueService, 'updateIssue').mockRejectedValueOnce({
      normalized: { status: 400, message: 'Invalid status transition' },
    });

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });

    const statusSelector = screen.getByLabelText(
      /move issue setup kanban scaffolding to status/i
    );
    fireEvent.change(statusSelector, { target: { value: ISSUE_STATUS.DONE } });

    await waitFor(() => {
      expect(screen.getByText('Invalid status transition')).toBeInTheDocument();
      expect(
        screen.getByLabelText(/move issue setup kanban scaffolding to status/i).value
      ).toBe(ISSUE_STATUS.TODO);
    });
  });

  it('enforces granular authorization: Developer can only move issues assigned to them', async () => {
    useAuth.mockReturnValue({
      user: { id: devUserId, email: 'dev@example.com', role: USER_ROLES.DEVELOPER },
    });
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(populatedIssuesPage);

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={false} />);

    await waitFor(() => {
      expect(screen.getByText('Setup Kanban scaffolding')).toBeInTheDocument();
    });

    // mockIssueTodo is assigned to devUserId -> status selector enabled
    const devAssignedSelector = screen.getByLabelText(
      /move issue setup kanban scaffolding to status/i
    );
    expect(devAssignedSelector).not.toBeDisabled();

    // mockIssueInProgress is assigned to pmUserId -> status selector disabled for devUserId
    const unassignedSelector = screen.getByLabelText(
      /move issue implement drag and drop to status/i
    );
    expect(unassignedSelector).toBeDisabled();

    // Create issue button not visible for Developer
    expect(document.getElementById('btn-create-kanban-issue')).not.toBeInTheDocument();
  });

  it('opens create modal, creates an issue, and adds it to the board', async () => {
    vi.spyOn(issueService, 'getIssues').mockResolvedValueOnce(emptyIssuesPage);
    const createSpy = vi.spyOn(issueService, 'createIssue').mockResolvedValueOnce({
      id: 'issue-new-99',
      title: 'New Kanban Issue',
      type: ISSUE_TYPE.TASK,
      priority: ISSUE_PRIORITY.MEDIUM,
      status: ISSUE_STATUS.TODO,
    });

    render(<KanbanBoard projectId={projectId} project={mockProject} canManageProject={true} />);

    await waitFor(() => {
      expect(screen.getByText('No issues created yet')).toBeInTheDocument();
    });

    fireEvent.click(document.getElementById('btn-create-kanban-issue'));
    expect(screen.getByRole('heading', { name: 'Create New Issue' })).toBeInTheDocument();

    fireEvent.change(document.getElementById('issue-title'), {
      target: { value: 'New Kanban Issue' },
    });

    const modalSubmitBtn = screen.getAllByRole('button', { name: 'Create Issue' }).pop();
    fireEvent.click(modalSubmitBtn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        projectId,
        title: 'New Kanban Issue',
        description: null,
        type: ISSUE_TYPE.TASK,
        priority: ISSUE_PRIORITY.MEDIUM,
        status: ISSUE_STATUS.TODO,
        sprintId: null,
        assignedToId: null,
      });
      expect(screen.getByText('New Kanban Issue')).toBeInTheDocument();
    });
  });
});
