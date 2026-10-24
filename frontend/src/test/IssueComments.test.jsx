import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import IssueComments from '../components/issues/IssueComments';
import commentService from '../services/commentService';
import { useAuth } from '../hooks/useAuth';
import { USER_ROLES } from '../constants/roles';

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('IssueComments Component', () => {
  const issueId = 'issue-uuid-100';
  const project = {
    id: 'proj-1',
    createdById: 'pm-user-1',
  };

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

  const mockComment1 = {
    id: 'comm-1',
    issueId,
    userId: 'user-1',
    content: 'Initial analysis completed.',
    createdAt: '2026-10-15T10:00:00Z',
    updatedAt: '2026-10-15T10:00:00Z',
  };

  const mockComment2 = {
    id: 'comm-2',
    issueId,
    userId: 'pm-user-1',
    content: 'Please prioritize this fix.',
    createdAt: '2026-10-15T11:00:00Z',
    updatedAt: '2026-10-15T11:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: {
        id: 'user-1',
        role: USER_ROLES.DEVELOPER,
      },
    });
  });

  it('renders loading state initially', () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockReturnValue(new Promise(() => {}));

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    expect(screen.getByText('Loading comments...')).toBeInTheDocument();
  });

  it('renders empty state when no comments exist', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText(/No comments yet\. Be the first to start the discussion!/i)).toBeInTheDocument();
    });
  });

  it('renders comments list with authors, dates, and contents', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [mockComment1, mockComment2],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText(/Comments \(2\)/i)).toBeInTheDocument();
      expect(screen.getByText('Alice Developer')).toBeInTheDocument();
      expect(screen.getByText('Initial analysis completed.')).toBeInTheDocument();
      expect(screen.getByText('Bob Manager')).toBeInTheDocument();
      expect(screen.getByText('Please prioritize this fix.')).toBeInTheDocument();
    });
  });

  it('submits a new comment and appends it to the list', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [mockComment1],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    const newComment = {
      id: 'comm-3',
      issueId,
      userId: 'user-1',
      content: 'PR has been submitted for review.',
      createdAt: '2026-10-15T12:00:00Z',
      updatedAt: '2026-10-15T12:00:00Z',
    };

    vi.spyOn(commentService, 'createComment').mockResolvedValueOnce(newComment);

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Initial analysis completed.')).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText('Leave a comment on this issue...');
    fireEvent.change(textarea, { target: { value: 'PR has been submitted for review.' } });

    const submitBtn = screen.getByRole('button', { name: 'Add Comment' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(commentService.createComment).toHaveBeenCalledWith({
        issueId,
        userId: 'user-1',
        content: 'PR has been submitted for review.',
      });
      expect(screen.getByText('Comment added successfully.')).toBeInTheDocument();
      expect(screen.getByText('PR has been submitted for review.')).toBeInTheDocument();
      expect(screen.getByText(/Comments \(2\)/i)).toBeInTheDocument();
    });
  });

  it('allows author to edit a comment and updates text inline', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [mockComment1],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    const updatedComment = {
      ...mockComment1,
      content: 'Initial analysis completed with corrected telemetry.',
      updatedAt: '2026-10-15T10:30:00Z',
    };

    vi.spyOn(commentService, 'updateComment').mockResolvedValueOnce(updatedComment);

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Initial analysis completed.')).toBeInTheDocument();
    });

    // Alice Developer is user-1 and logged in, so edit button should be visible
    const editBtn = screen.getByRole('button', { name: 'Edit comment by Alice Developer' });
    fireEvent.click(editBtn);

    // Form appears inline
    const editTextarea = screen.getByDisplayValue('Initial analysis completed.');
    fireEvent.change(editTextarea, { target: { value: 'Initial analysis completed with corrected telemetry.' } });

    const saveBtn = screen.getByRole('button', { name: 'Save Changes' });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(commentService.updateComment).toHaveBeenCalledWith('comm-1', {
        content: 'Initial analysis completed with corrected telemetry.',
      });
      expect(screen.getByText('Comment updated successfully.')).toBeInTheDocument();
      expect(screen.getByText('Initial analysis completed with corrected telemetry.')).toBeInTheDocument();
    });
  });

  it('allows author to delete comment with confirmation modal', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [mockComment1],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    vi.spyOn(commentService, 'deleteComment').mockResolvedValueOnce(null);

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Initial analysis completed.')).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole('button', { name: 'Delete comment by Alice Developer' });
    fireEvent.click(deleteBtn);

    // ConfirmModal appears
    expect(screen.getByRole('heading', { name: 'Delete Comment' })).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete this comment? This action cannot be undone.')).toBeInTheDocument();

    const confirmBtn = screen.getByRole('button', { name: 'Delete Comment' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(commentService.deleteComment).toHaveBeenCalledWith('comm-1');
      expect(screen.getByText('Comment deleted successfully.')).toBeInTheDocument();
      expect(screen.queryByText('Initial analysis completed.')).not.toBeInTheDocument();
      expect(screen.getByText(/No comments yet\. Be the first to start the discussion!/i)).toBeInTheDocument();
    });
  });

  it('enforces authorization: developer cannot edit or delete other users comments', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue').mockResolvedValueOnce({
      content: [mockComment2], // created by pm-user-1
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Bob Manager')).toBeInTheDocument();
      expect(screen.getByText('Please prioritize this fix.')).toBeInTheDocument();
    });

    // Logged in as user-1 (developer), cannot edit Bob's comment
    expect(screen.queryByRole('button', { name: 'Edit comment by Bob Manager' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete comment by Bob Manager' })).not.toBeInTheDocument();
  });

  it('renders pagination and navigates pages when totalPages > 1', async () => {
    vi.spyOn(commentService, 'getCommentsByIssue')
      .mockResolvedValueOnce({
        content: [mockComment1],
        page: 0,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: true,
        last: false,
      })
      .mockResolvedValueOnce({
        content: [mockComment2],
        page: 1,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true,
      });

    render(<IssueComments issueId={issueId} project={project} members={mockMembers} />);

    await waitFor(() => {
      expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
      expect(screen.getByText('Initial analysis completed.')).toBeInTheDocument();
    });

    const nextBtn = screen.getByRole('button', { name: 'Next page of comments' });
    expect(nextBtn).toBeEnabled();
    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(commentService.getCommentsByIssue).toHaveBeenCalledWith(issueId, {
        page: 1,
        size: 20,
        sort: 'createdAt,asc',
      });
      expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
      expect(screen.getByText('Please prioritize this fix.')).toBeInTheDocument();
    });
  });
});
