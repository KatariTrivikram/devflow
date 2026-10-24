import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import CommentForm from '../components/issues/CommentForm';

describe('CommentForm Component', () => {
  it('renders textarea with default placeholder and character counter', () => {
    render(<CommentForm onSubmit={vi.fn()} />);

    const textarea = screen.getByPlaceholderText('Add a comment...');
    expect(textarea).toBeInTheDocument();
    expect(screen.getByText(/0\s*\/\s*2000/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add Comment' })).toBeInTheDocument();
  });

  it('renders in edit mode with initial content and Save Changes label', () => {
    render(
      <CommentForm
        initialContent="Initial test comment"
        isEditing={true}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    const textarea = screen.getByDisplayValue('Initial test comment');
    expect(textarea).toBeInTheDocument();
    expect(screen.getByText(/20\s*\/\s*2000/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('disables submit button when content is empty', () => {
    const handleSubmit = vi.fn();
    render(<CommentForm onSubmit={handleSubmit} />);

    const submitBtn = screen.getByRole('button', { name: 'Add Comment' });
    expect(submitBtn).toBeDisabled();
  });

  it('validates maximum length constraint (2000 characters)', () => {
    const handleSubmit = vi.fn();
    render(<CommentForm onSubmit={handleSubmit} />);

    const textarea = screen.getByPlaceholderText('Add a comment...');
    const longText = 'a'.repeat(2001);
    fireEvent.change(textarea, { target: { value: longText } });

    const submitBtn = screen.getByRole('button', { name: 'Add Comment' });
    expect(submitBtn).toBeDisabled();

    // Triggering submit directly checks validation
    const form = textarea.closest('form');
    fireEvent.submit(form);

    expect(screen.getByRole('alert')).toHaveTextContent('Comment cannot exceed 2000 characters.');
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('submits trimmed content and resets form when not editing', async () => {
    const handleSubmit = vi.fn().mockResolvedValue();
    render(<CommentForm onSubmit={handleSubmit} />);

    const textarea = screen.getByPlaceholderText('Add a comment...');
    fireEvent.change(textarea, { target: { value: '  This is my comment.  ' } });

    const submitBtn = screen.getByRole('button', { name: 'Add Comment' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith('This is my comment.');
      expect(textarea.value).toBe('');
    });
  });

  it('invokes onCancel when cancel button is clicked', () => {
    const handleCancel = vi.fn();
    render(
      <CommentForm
        initialContent="Editing comment"
        isEditing={true}
        onSubmit={vi.fn()}
        onCancel={handleCancel}
      />
    );

    const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelBtn);

    expect(handleCancel).toHaveBeenCalledTimes(1);
  });

  it('disables submit button and shows loading text during submission', () => {
    render(<CommentForm onSubmit={vi.fn()} isSubmitting={true} />);

    const submitBtn = screen.getByRole('button', { name: 'Posting...' });
    expect(submitBtn).toBeDisabled();
  });
});
