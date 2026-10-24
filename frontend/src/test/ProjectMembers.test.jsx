import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import ProjectMembers from '../components/projects/ProjectMembers';
import projectMemberService from '../services/projectMemberService';
import userService from '../services/userService';
import { USER_ROLES } from '../constants/roles';

describe('ProjectMembers Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const sampleMembers = [
    {
      id: 'member-rec-1',
      projectId: 'proj-1',
      userId: 'user-1',
      joinedAt: '2026-10-10T12:00:00Z',
    },
    {
      id: 'member-rec-2',
      projectId: 'proj-1',
      userId: 'user-2',
      joinedAt: '2026-10-11T14:30:00Z',
    },
  ];

  const user1 = {
    id: 'user-1',
    name: 'Dev Dave',
    email: 'dave@example.com',
    role: USER_ROLES.DEVELOPER,
  };

  const user2 = {
    id: 'user-2',
    name: 'Paula PM',
    email: 'paula@example.com',
    role: USER_ROLES.PROJECT_MANAGER,
  };

  it('renders loading state initially', () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockReturnValue(new Promise(() => {}));

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    expect(screen.getByText(/loading team members/i)).toBeInTheDocument();
  });

  it('renders empty state when project has no members', async () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValueOnce([]);

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'No team members assigned' })).toBeInTheDocument();
      expect(screen.getAllByRole('button', { name: /add member/i }).length).toBeGreaterThan(0);
    });
  });

  it('renders error state and retries on failure', async () => {
    const getSpy = vi.spyOn(projectMemberService, 'getProjectMembers')
      .mockRejectedValueOnce({
        normalized: { status: 500, message: 'Database connection failed' },
      })
      .mockResolvedValueOnce([]);

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    await waitFor(() => {
      expect(screen.getByText('Unable to load team members')).toBeInTheDocument();
      expect(screen.getByText('Database connection failed')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /try again/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'No team members assigned' })).toBeInTheDocument();
    });

    expect(getSpy).toHaveBeenCalledTimes(2);
  });

  it('renders members list with user details, role badge, and joined date', async () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValueOnce(sampleMembers);
    vi.spyOn(userService, 'getUserById')
      .mockResolvedValueOnce(user1)
      .mockResolvedValueOnce(user2);

    render(<ProjectMembers projectId="proj-1" canManageMembers={false} />);

    await waitFor(() => {
      expect(screen.getByText('Dev Dave')).toBeInTheDocument();
      expect(screen.getByText('dave@example.com')).toBeInTheDocument();
      expect(screen.getByText('Developer')).toBeInTheDocument();
      expect(screen.getByText('Paula PM')).toBeInTheDocument();
      expect(screen.getByText('paula@example.com')).toBeInTheDocument();
      expect(screen.getByText('Project Manager')).toBeInTheDocument();
    });

    // When canManageMembers is false, management buttons should NOT be rendered
    expect(screen.queryByRole('button', { name: /add member/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /remove/i })).not.toBeInTheDocument();
  });

  it('displays management controls when canManageMembers is true', async () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValueOnce(sampleMembers);
    vi.spyOn(userService, 'getUserById')
      .mockResolvedValueOnce(user1)
      .mockResolvedValueOnce(user2);

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    await waitFor(() => {
      expect(screen.getByText('Dev Dave')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /add member/i })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /remove/i }).length).toBe(2);
  });

  it('opens confirm modal and successfully removes a member', async () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValueOnce(sampleMembers);
    vi.spyOn(userService, 'getUserById')
      .mockResolvedValueOnce(user1)
      .mockResolvedValueOnce(user2);

    const deleteSpy = vi.spyOn(projectMemberService, 'removeMember').mockResolvedValueOnce(null);

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    await waitFor(() => {
      expect(screen.getByText('Dev Dave')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Remove Dev Dave from project'));

    expect(screen.getByRole('heading', { name: 'Remove Team Member' })).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to remove "dev dave" from this project team\?/i)).toBeInTheDocument();

    const removeConfirmButtons = screen.getAllByRole('button', { name: 'Remove Member' });
    fireEvent.click(removeConfirmButtons[removeConfirmButtons.length - 1]);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('proj-1', 'user-1');
      expect(screen.queryByText('Dev Dave')).not.toBeInTheDocument();
      expect(screen.getByText(/dev dave was removed from the project team/i)).toBeInTheDocument();
    });
  });

  it('handles backend 403 error on removal failure', async () => {
    vi.spyOn(projectMemberService, 'getProjectMembers').mockResolvedValueOnce([sampleMembers[0]]);
    vi.spyOn(userService, 'getUserById').mockResolvedValueOnce(user1);

    vi.spyOn(projectMemberService, 'removeMember').mockRejectedValueOnce({
      normalized: {
        status: 403,
        message: 'Access denied: Project Manager or Admin role required',
      },
    });

    render(<ProjectMembers projectId="proj-1" canManageMembers={true} />);

    await waitFor(() => {
      expect(screen.getByText('Dev Dave')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Remove Dev Dave from project'));
    const removeConfirmButtons = screen.getAllByRole('button', { name: 'Remove Member' });
    fireEvent.click(removeConfirmButtons[removeConfirmButtons.length - 1]);

    await waitFor(() => {
      expect(screen.getByText('Access denied: Project Manager or Admin role required')).toBeInTheDocument();
      // Member should still be present in the table
      expect(screen.getByText('Dev Dave')).toBeInTheDocument();
    });
  });
});
