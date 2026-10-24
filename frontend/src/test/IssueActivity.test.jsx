import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import IssueActivity from '../components/issues/IssueActivity';
import activityService from '../services/activityService';

describe('IssueActivity Component', () => {
  const issueId = 'issue-uuid-100';

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

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.spyOn(activityService, 'getActivitiesByIssue').mockReturnValue(new Promise(() => {}));

    render(<IssueActivity issueId={issueId} members={mockMembers} />);

    expect(screen.getByText('Loading activity history...')).toBeInTheDocument();
  });

  it('renders error state on fetch failure', async () => {
    vi.spyOn(activityService, 'getActivitiesByIssue').mockRejectedValueOnce({
      normalized: { status: 500, message: 'Server unreachable.' },
    });

    render(<IssueActivity issueId={issueId} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Unable to load activity')).toBeInTheDocument();
      expect(screen.getByText('Server unreachable.')).toBeInTheDocument();
    });
  });

  it('renders empty state when no activities exist', async () => {
    vi.spyOn(activityService, 'getActivitiesByIssue').mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    render(<IssueActivity issueId={issueId} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText(/No activity recorded yet for this issue/i)).toBeInTheDocument();
    });
  });

  it('renders various activity actions with actor names, action labels, and diffs', async () => {
    const mockActivities = [
      {
        id: 'act-1',
        issueId,
        userId: 'pm-user-1',
        action: 'ISSUE_CREATED',
        createdAt: '2026-10-15T09:00:00Z',
      },
      {
        id: 'act-2',
        issueId,
        userId: 'pm-user-1',
        action: 'ASSIGNED',
        newValue: 'user-1',
        createdAt: '2026-10-15T09:05:00Z',
      },
      {
        id: 'act-3',
        issueId,
        userId: 'user-1',
        action: 'STATUS_CHANGED',
        oldValue: 'TODO',
        newValue: 'IN_PROGRESS',
        createdAt: '2026-10-15T10:00:00Z',
      },
      {
        id: 'act-4',
        issueId,
        userId: 'user-1',
        action: 'PRIORITY_CHANGED',
        oldValue: 'MEDIUM',
        newValue: 'HIGH',
        createdAt: '2026-10-15T10:30:00Z',
      },
      {
        id: 'act-5',
        issueId,
        userId: 'user-1',
        action: 'COMMENT_ADDED',
        createdAt: '2026-10-15T11:00:00Z',
      },
      {
        id: 'act-6',
        issueId,
        userId: 'pm-user-1',
        action: 'UNASSIGNED',
        oldValue: 'user-1',
        createdAt: '2026-10-15T11:30:00Z',
      },
    ];

    vi.spyOn(activityService, 'getActivitiesByIssue').mockResolvedValueOnce({
      content: mockActivities,
      page: 0,
      size: 20,
      totalElements: 6,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(<IssueActivity issueId={issueId} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText(/Activity History \(6\)/i)).toBeInTheDocument();
      // Actor names
      expect(screen.getAllByText('Bob Manager').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Alice Developer').length).toBeGreaterThan(0);
      // Actions
      expect(screen.getByText('created this issue')).toBeInTheDocument();
      expect(screen.getByText(/assigned to/i)).toBeInTheDocument();
      expect(screen.getByText('changed status')).toBeInTheDocument();
      expect(screen.getByText('To Do')).toBeInTheDocument();
      expect(screen.getByText('In Progress')).toBeInTheDocument();
      expect(screen.getByText('changed priority')).toBeInTheDocument();
      expect(screen.getByText('Medium')).toBeInTheDocument();
      expect(screen.getByText('High')).toBeInTheDocument();
      expect(screen.getByText('added a comment')).toBeInTheDocument();
      expect(screen.getByText(/unassigned from/i)).toBeInTheDocument();
    });
  });

  it('renders pagination and loads next page on click', async () => {
    vi.spyOn(activityService, 'getActivitiesByIssue')
      .mockResolvedValueOnce({
        content: [
          {
            id: 'act-1',
            issueId,
            userId: 'user-1',
            action: 'ISSUE_CREATED',
            createdAt: '2026-10-15T09:00:00Z',
          },
        ],
        page: 0,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: true,
        last: false,
      })
      .mockResolvedValueOnce({
        content: [
          {
            id: 'act-2',
            issueId,
            userId: 'user-1',
            action: 'COMMENT_ADDED',
            createdAt: '2026-10-15T10:00:00Z',
          },
        ],
        page: 1,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true,
      });

    render(<IssueActivity issueId={issueId} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
      expect(screen.getByText('created this issue')).toBeInTheDocument();
    });

    const nextBtn = screen.getByRole('button', { name: 'Next page of activity' });
    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(activityService.getActivitiesByIssue).toHaveBeenCalledWith(issueId, {
        page: 1,
        size: 20,
        sort: 'createdAt,desc',
      });
      expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
      expect(screen.getByText('added a comment')).toBeInTheDocument();
    });
  });
});
