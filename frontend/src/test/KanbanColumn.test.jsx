import React from 'react';
import { render as rtlRender, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import KanbanColumn from '../components/projects/kanban/KanbanColumn';
import { ISSUE_STATUS, ISSUE_PRIORITY, ISSUE_TYPE } from '../constants/issueEnums';

const render = (ui, options) => rtlRender(ui, { wrapper: MemoryRouter, ...options });

describe('KanbanColumn Component', () => {
  const mockIssues = [
    {
      id: 'issue-1',
      title: 'First task',
      description: 'First task description',
      type: ISSUE_TYPE.TASK,
      priority: ISSUE_PRIORITY.LOW,
      status: ISSUE_STATUS.TODO,
    },
    {
      id: 'issue-2',
      title: 'Second task',
      description: 'Second task description',
      type: ISSUE_TYPE.FEATURE,
      priority: ISSUE_PRIORITY.MEDIUM,
      status: ISSUE_STATUS.TODO,
    },
  ];

  it('renders column title and count badge', () => {
    render(
      <KanbanColumn
        status={ISSUE_STATUS.TODO}
        title="To Do"
        issues={mockIssues}
      />
    );

    expect(screen.getByRole('region', { name: 'To Do column' })).toBeInTheDocument();
    expect(screen.getAllByText('To Do').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByLabelText('2 issues in To Do')).toHaveTextContent('2');
    expect(screen.getByText('First task')).toBeInTheDocument();
    expect(screen.getByText('Second task')).toBeInTheDocument();
  });

  it('renders compact empty placeholder when column has no issues', () => {
    render(
      <KanbanColumn
        status={ISSUE_STATUS.DONE}
        title="Done"
        issues={[]}
      />
    );

    expect(screen.getByText('Done')).toBeInTheDocument();
    expect(screen.getByLabelText('0 issues in Done')).toHaveTextContent('0');
    expect(screen.getByText('No issues')).toBeInTheDocument();
  });

  it('handles drag over, drag leave, and drop events', () => {
    const handleDropIssue = vi.fn();
    render(
      <KanbanColumn
        status={ISSUE_STATUS.IN_PROGRESS}
        title="In Progress"
        issues={[]}
        onDropIssue={handleDropIssue}
      />
    );

    const column = screen.getByTestId('kanban-column-IN_PROGRESS');

    // Drag over
    fireEvent.dragOver(column, {
      dataTransfer: { dropEffect: '' },
    });
    expect(column).toHaveClass('df-kanban-column-dropzone');

    // Drag leave
    fireEvent.dragLeave(column, {
      relatedTarget: document.body,
    });
    expect(column).not.toHaveClass('df-kanban-column-dropzone');

    // Drop
    fireEvent.drop(column, {
      dataTransfer: {
        getData: (type) =>
          type === 'text/plain'
            ? JSON.stringify({ issueId: 'issue-1', currentStatus: ISSUE_STATUS.TODO })
            : '',
      },
    });

    expect(handleDropIssue).toHaveBeenCalledWith('issue-1', ISSUE_STATUS.IN_PROGRESS);
  });
});
