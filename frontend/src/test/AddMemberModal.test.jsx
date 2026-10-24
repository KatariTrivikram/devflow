import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import AddMemberModal from '../components/projects/AddMemberModal';
import userService from '../services/userService';
import projectMemberService from '../services/projectMemberService';
import { USER_ROLES } from '../constants/roles';

describe('AddMemberModal Component', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockFoundUser = {
    id: 'user-uuid-99',
    name: 'Carlos Mendez',
    email: 'carlos@example.com',
    role: USER_ROLES.DEVELOPER,
  };

  it('does not render when isOpen is false', () => {
    const { container } = render(
      <AddMemberModal
        isOpen={false}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders search input when isOpen is true', () => {
    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { name: 'Add Team Member' })).toBeInTheDocument();
    expect(screen.getByLabelText(/user email address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Find User' })).toBeInTheDocument();
  });

  it('displays error when searching with empty email', async () => {
    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    const input = screen.getByLabelText(/user email address/i);
    fireEvent.change(input, { target: { value: '   ' } });

    fireEvent.submit(input.closest('form'));

    expect(screen.getByRole('alert')).toHaveTextContent(/please enter a valid user email/i);
  });

  it('searches user by email and displays user preview card', async () => {
    vi.spyOn(userService, 'getUserByEmail').mockResolvedValueOnce(mockFoundUser);

    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText(/user email address/i), {
      target: { value: 'carlos@example.com' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Find User' }));

    await waitFor(() => {
      expect(screen.getByText('Carlos Mendez')).toBeInTheDocument();
      expect(screen.getByText('carlos@example.com')).toBeInTheDocument();
      expect(screen.getByText('Developer')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Add to Project' })).toBeEnabled();
  });

  it('displays not found alert when email does not exist', async () => {
    vi.spyOn(userService, 'getUserByEmail').mockRejectedValueOnce({
      normalized: { status: 404, message: 'User not found' },
    });

    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText(/user email address/i), {
      target: { value: 'unknown@example.com' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Find User' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        /no registered user found with email "unknown@example.com"/i
      );
    });
  });

  it('disables Add button and shows warning if user is already a member', async () => {
    vi.spyOn(userService, 'getUserByEmail').mockResolvedValueOnce(mockFoundUser);

    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        existingMemberUserIds={['user-uuid-99']}
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText(/user email address/i), {
      target: { value: 'carlos@example.com' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Find User' }));

    await waitFor(() => {
      expect(screen.getByText('Carlos Mendez')).toBeInTheDocument();
      expect(screen.getByText(/this user is already a member of this project team/i)).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Add to Project' })).toBeDisabled();
  });

  it('successfully adds member and calls onMemberAdded and onClose', async () => {
    vi.spyOn(userService, 'getUserByEmail').mockResolvedValueOnce(mockFoundUser);
    const addSpy = vi.spyOn(projectMemberService, 'addMember').mockResolvedValueOnce({
      id: 'membership-rec-1',
      projectId: 'proj-1',
      userId: 'user-uuid-99',
      joinedAt: '2026-10-14T12:00:00Z',
    });

    const onMemberAdded = vi.fn();
    const onClose = vi.fn();

    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        existingMemberUserIds={[]}
        onClose={onClose}
        onMemberAdded={onMemberAdded}
      />
    );

    fireEvent.change(screen.getByLabelText(/user email address/i), {
      target: { value: 'carlos@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Find User' }));

    await waitFor(() => {
      expect(screen.getByText('Carlos Mendez')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add to Project' }));

    await waitFor(() => {
      expect(addSpy).toHaveBeenCalledWith({
        projectId: 'proj-1',
        userId: 'user-uuid-99',
      });
      expect(onMemberAdded).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'membership-rec-1',
          userId: 'user-uuid-99',
          user: mockFoundUser,
        })
      );
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('displays server error when backend rejects addition', async () => {
    vi.spyOn(userService, 'getUserByEmail').mockResolvedValueOnce(mockFoundUser);
    vi.spyOn(projectMemberService, 'addMember').mockRejectedValueOnce({
      normalized: {
        status: 403,
        message: 'Access denied: Project Manager or Admin role required',
      },
    });

    render(
      <AddMemberModal
        isOpen={true}
        projectId="proj-1"
        onClose={vi.fn()}
        onMemberAdded={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText(/user email address/i), {
      target: { value: 'carlos@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Find User' }));

    await waitFor(() => {
      expect(screen.getByText('Carlos Mendez')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Add to Project' }));

    await waitFor(() => {
      expect(screen.getByText('Access Denied')).toBeInTheDocument();
      expect(screen.getByText('Access denied: Project Manager or Admin role required')).toBeInTheDocument();
    });
  });
});
