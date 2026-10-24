import React from 'react';
import { render as rtlRender, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import KanbanIssueCard from '../components/projects/kanban/KanbanIssueCard';

const render = (ui, options) => rtlRender(ui, { wrapper: MemoryRouter, ...options });
import {
  ISSUE_STATUS,
  ISSUE_PRIORITY,
  ISSUE_TYPE,
} from '../constants/issueEnums';

describe('KanbanIssueCard Component', () => {
  const mockIssue = {
    id: 'issue-1111-2222',
    title: 'Fix drag and drop event handling',
    description: 'Ensure text/plain contains json payload',
    type: ISSUE_TYPE.BUG,
    priority: ISSUE_PRIORITY.HIGH,
    status: ISSUE_STATUS.TODO,
    sprintId: 'sprint-1',
    assignedToId: 'user-1',
  };

  const mockSprints = [
    { id: 'sprint-1', name: 'Sprint 1 - Foundation' },
  ];

  const mockMembers = [
    { userId: 'user-1', user: { id: 'user-1', name: 'Alice Developer', email: 'alice@example.com' } },
  ];

  it('renders issue card with ID, badges, title, description, sprint, and assignee', () => {
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={true}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    expect(screen.getByText('#issue-11')).toBeInTheDocument();
    expect(screen.getByText('Fix drag and drop event handling')).toBeInTheDocument();
    expect(screen.getByText('Ensure text/plain contains json payload')).toBeInTheDocument();
    expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    expect(screen.getByText('Alice Developer')).toBeInTheDocument();
    expect(screen.getByText('Bug')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
  });

  it('calls onStatusChange when status is changed via accessible selector', () => {
    const handleStatusChange = vi.fn();
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={true}
        onStatusChange={handleStatusChange}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    const selector = screen.getByLabelText(/move issue fix drag and drop event handling to status/i);
    expect(selector).toBeInTheDocument();
    expect(selector.value).toBe(ISSUE_STATUS.TODO);

    fireEvent.change(selector, { target: { value: ISSUE_STATUS.IN_PROGRESS } });
    expect(handleStatusChange).toHaveBeenCalledWith(mockIssue, ISSUE_STATUS.IN_PROGRESS);
  });

  it('calls onEdit when edit button is clicked', () => {
    const handleEdit = vi.fn();
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={true}
        onEdit={handleEdit}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    const editBtn = screen.getByRole('button', { name: /edit issue fix drag and drop event handling/i });
    fireEvent.click(editBtn);
    expect(handleEdit).toHaveBeenCalledWith(mockIssue);
  });

  it('disables dragging, disables status selector, and hides edit button when canEdit is false', () => {
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={false}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    const card = screen.getByTestId('kanban-card-issue-1111-2222');
    expect(card).not.toHaveAttribute('draggable', 'true');
    expect(card).toHaveClass('df-kanban-card-disabled');

    const selector = screen.getByLabelText(/move issue fix drag and drop event handling to status/i);
    expect(selector).toBeDisabled();

    expect(screen.queryByRole('button', { name: /edit issue/i })).not.toBeInTheDocument();
  });

  it('disables controls when isUpdating is true', () => {
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={true}
        isUpdating={true}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    const card = screen.getByTestId('kanban-card-issue-1111-2222');
    expect(card).toHaveClass('df-kanban-card-updating');
    expect(card).not.toHaveAttribute('draggable', 'true');

    const selector = screen.getByLabelText(/move issue fix drag and drop event handling to status/i);
    expect(selector).toBeDisabled();
  });

  it('sets dataTransfer payload on drag start', () => {
    render(
      <KanbanIssueCard
        issue={mockIssue}
        canEdit={true}
        sprints={mockSprints}
        members={mockMembers}
      />
    );

    const card = screen.getByTestId('kanban-card-issue-1111-2222');
    const setDataMock = vi.fn();

    fireEvent.dragStart(card, {
      dataTransfer: {
        setData: setDataMock,
        effectAllowed: '',
      },
    });

    expect(setDataMock).toHaveBeenCalledWith(
      'text/plain',
      JSON.stringify({ issueId: mockIssue.id, currentStatus: mockIssue.status })
    );

    fireEvent.dragEnd(card);
    expect(card).not.toHaveClass('df-kanban-card-dragging');
  });
});
