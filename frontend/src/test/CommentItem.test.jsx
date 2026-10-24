import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import CommentItem from '../components/issues/CommentItem';

describe('CommentItem Component', () => {
  const mockComment = {
    id: 'comm-123',
    issueId: 'issue-456',
    userId: 'user-789',
    content: 'This is an important update on the defect root cause.',
    createdAt: '2026-10-15T10:00:00Z',
    updatedAt: '2026-10-15T10:00:00Z',
  };

  const mockAuthor = {
    id: 'user-789',
    name: 'Alice Engineer',
    email: 'alice@example.com',
  };

  it('renders author info, initials avatar, formatted date, and comment text', () => {
    render(
      <CommentItem
        comment={mockComment}
        author={mockAuthor}
        canModify={false}
      />
    );

    expect(screen.getByText('Alice Engineer')).toBeInTheDocument();
    expect(screen.getByText('AE')).toBeInTheDocument();
    expect(screen.getByText('This is an important update on the defect root cause.')).toBeInTheDocument();
    expect(screen.queryByText('(edited)')).not.toBeInTheDocument();
  });

  it('renders fallback author name when author object is absent', () => {
    render(
      <CommentItem
        comment={mockComment}
        author={null}
        canModify={false}
      />
    );

    expect(screen.getByText('User user-789')).toBeInTheDocument();
  });

  it('displays (edited) badge when updatedAt is later than createdAt', () => {
    const editedComment = {
      ...mockComment,
      updatedAt: '2026-10-15T11:30:00Z',
    };

    render(
      <CommentItem
        comment={editedComment}
        author={mockAuthor}
        canModify={false}
      />
    );

    expect(screen.getByText('(edited)')).toBeInTheDocument();
  });

  it('renders Edit and Delete buttons when canModify is true', () => {
    const handleEdit = vi.fn();
    const handleDelete = vi.fn();

    render(
      <CommentItem
        comment={mockComment}
        author={mockAuthor}
        canModify={true}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
    );

    const editBtn = screen.getByRole('button', { name: 'Edit comment by Alice Engineer' });
    const deleteBtn = screen.getByRole('button', { name: 'Delete comment by Alice Engineer' });

    expect(editBtn).toBeInTheDocument();
    expect(deleteBtn).toBeInTheDocument();

    fireEvent.click(editBtn);
    expect(handleEdit).toHaveBeenCalledWith(mockComment);

    fireEvent.click(deleteBtn);
    expect(handleDelete).toHaveBeenCalledWith(mockComment);
  });

  it('does not render Edit or Delete buttons when canModify is false', () => {
    render(
      <CommentItem
        comment={mockComment}
        author={mockAuthor}
        canModify={false}
      />
    );

    expect(screen.queryByRole('button', { name: /Edit/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Delete/i })).not.toBeInTheDocument();
  });

  it('disables actions when isDeleting is true', () => {
    render(
      <CommentItem
        comment={mockComment}
        author={mockAuthor}
        canModify={true}
        onEdit={vi.fn()}
        onDelete={vi.fn()}
        isDeleting={true}
      />
    );

    const editBtn = screen.getByRole('button', { name: 'Edit comment by Alice Engineer' });
    const deleteBtn = screen.getByRole('button', { name: 'Delete comment by Alice Engineer' });

    expect(editBtn).toBeDisabled();
    expect(deleteBtn).toBeDisabled();
    expect(deleteBtn).toHaveTextContent('Deleting...');
  });
});
