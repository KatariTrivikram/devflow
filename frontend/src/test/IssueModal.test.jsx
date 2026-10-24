import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import IssueModal from '../components/projects/IssueModal';
import { ISSUE_TYPE, ISSUE_PRIORITY, ISSUE_STATUS } from '../constants/issueEnums';

describe('IssueModal Component', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    projectId: 'proj-uuid-1',
    initialData: null,
    sprints: [
      { id: 'sprint-1', name: 'Sprint 1', status: 'ACTIVE' },
      { id: 'sprint-2', name: 'Sprint 2', status: 'PLANNED' },
    ],
    members: [
      { userId: 'user-1', userName: 'Alice Admin', userEmail: 'alice@example.com' },
      { userId: 'user-2', userName: 'Bob Dev', userEmail: 'bob@example.com' },
    ],
    isSubmitting: false,
    serverError: null,
  };

  it('renders nothing when isOpen is false', () => {
    const { container } = render(<IssueModal {...defaultProps} isOpen={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders in create mode with default fields', () => {
    render(<IssueModal {...defaultProps} />);

    expect(screen.getByRole('heading', { name: 'Create New Issue' })).toBeInTheDocument();
    expect(screen.getByLabelText(/title/i)).toHaveValue('');
    expect(screen.getByLabelText(/issue type/i)).toHaveValue(ISSUE_TYPE.TASK);
    expect(screen.getByLabelText(/priority/i)).toHaveValue(ISSUE_PRIORITY.MEDIUM);
    expect(screen.getByLabelText(/status/i)).toHaveValue(ISSUE_STATUS.TODO);
    expect(screen.getByLabelText(/sprint/i)).toHaveValue('');
    expect(screen.getByLabelText(/assignee/i)).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Create Issue' })).toBeInTheDocument();
  });

  it('renders in edit mode pre-populated with existing issue details', () => {
    const initialData = {
      id: 'issue-1',
      title: 'Fix memory leak in websocket listener',
      description: 'Occurs after prolonged dashboard usage',
      type: ISSUE_TYPE.BUG,
      priority: ISSUE_PRIORITY.CRITICAL,
      status: ISSUE_STATUS.IN_PROGRESS,
      sprintId: 'sprint-1',
      assignedToId: 'user-2',
    };

    render(<IssueModal {...defaultProps} initialData={initialData} />);

    expect(screen.getByRole('heading', { name: 'Edit Issue' })).toBeInTheDocument();
    expect(screen.getByLabelText(/title/i)).toHaveValue('Fix memory leak in websocket listener');
    expect(screen.getByLabelText(/description/i)).toHaveValue('Occurs after prolonged dashboard usage');
    expect(screen.getByLabelText(/issue type/i)).toHaveValue(ISSUE_TYPE.BUG);
    expect(screen.getByLabelText(/priority/i)).toHaveValue(ISSUE_PRIORITY.CRITICAL);
    expect(screen.getByLabelText(/status/i)).toHaveValue(ISSUE_STATUS.IN_PROGRESS);
    expect(screen.getByLabelText(/sprint/i)).toHaveValue('sprint-1');
    expect(screen.getByLabelText(/assignee/i)).toHaveValue('user-2');
    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeInTheDocument();
  });

  it('validates required title on submit', () => {
    const handleSubmit = vi.fn();
    render(<IssueModal {...defaultProps} onSubmit={handleSubmit} />);

    fireEvent.click(screen.getByRole('button', { name: 'Create Issue' }));

    expect(screen.getByText('Issue title is required')).toBeInTheDocument();
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('validates that title does not exceed 200 characters', () => {
    const handleSubmit = vi.fn();
    render(<IssueModal {...defaultProps} onSubmit={handleSubmit} />);

    const longTitle = 'a'.repeat(201);
    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: longTitle } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Issue' }));

    expect(screen.getByText('Issue title cannot exceed 200 characters')).toBeInTheDocument();
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('submits valid form data in create mode with projectId injected', () => {
    const handleSubmit = vi.fn();
    render(<IssueModal {...defaultProps} onSubmit={handleSubmit} />);

    fireEvent.change(screen.getByLabelText(/title/i), {
      target: { value: 'Implement OAuth authentication' },
    });
    fireEvent.change(screen.getByLabelText(/description/i), {
      target: { value: 'Add Google and GitHub OAuth providers' },
    });
    fireEvent.change(screen.getByLabelText(/issue type/i), {
      target: { value: ISSUE_TYPE.FEATURE },
    });
    fireEvent.change(screen.getByLabelText(/priority/i), {
      target: { value: ISSUE_PRIORITY.HIGH },
    });
    fireEvent.change(screen.getByLabelText(/status/i), {
      target: { value: ISSUE_STATUS.IN_PROGRESS },
    });
    fireEvent.change(screen.getByLabelText(/sprint/i), {
      target: { value: 'sprint-1' },
    });
    fireEvent.change(screen.getByLabelText(/assignee/i), {
      target: { value: 'user-2' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Issue' }));

    expect(handleSubmit).toHaveBeenCalledWith({
      projectId: 'proj-uuid-1',
      title: 'Implement OAuth authentication',
      description: 'Add Google and GitHub OAuth providers',
      type: ISSUE_TYPE.FEATURE,
      priority: ISSUE_PRIORITY.HIGH,
      status: ISSUE_STATUS.IN_PROGRESS,
      sprintId: 'sprint-1',
      assignedToId: 'user-2',
    });
  });

  it('submits valid form data in edit mode without injecting projectId', () => {
    const initialData = {
      id: 'issue-1',
      title: 'Update existing issue',
      description: 'Existing description',
      type: ISSUE_TYPE.TASK,
      priority: ISSUE_PRIORITY.LOW,
      status: ISSUE_STATUS.TODO,
      sprintId: null,
      assignedToId: null,
    };

    const handleSubmit = vi.fn();
    render(<IssueModal {...defaultProps} initialData={initialData} onSubmit={handleSubmit} />);

    fireEvent.change(screen.getByLabelText(/title/i), {
      target: { value: 'Update existing issue modified' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(handleSubmit).toHaveBeenCalledWith({
      title: 'Update existing issue modified',
      description: 'Existing description',
      type: ISSUE_TYPE.TASK,
      priority: ISSUE_PRIORITY.LOW,
      status: ISSUE_STATUS.TODO,
      sprintId: null,
      assignedToId: null,
    });
  });

  it('displays server error message in alert role', () => {
    render(
      <IssueModal
        {...defaultProps}
        serverError={{ message: 'Assigned user does not belong to project' }}
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Assigned user does not belong to project'
    );
  });

  it('disables buttons and inputs during submission', () => {
    render(<IssueModal {...defaultProps} isSubmitting={true} />);

    expect(screen.getByRole('button', { name: 'Creating...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByLabelText(/title/i)).toBeDisabled();
  });

  it('calls onClose when Cancel button or backdrop is clicked', () => {
    const handleClose = vi.fn();
    render(<IssueModal {...defaultProps} onClose={handleClose} />);

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(handleClose).toHaveBeenCalled();
  });
});
