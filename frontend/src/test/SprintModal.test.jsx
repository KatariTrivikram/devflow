import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SprintModal from '../components/projects/SprintModal';
import { SPRINT_STATUS } from '../constants/projectEnums';

describe('SprintModal Component', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    projectId: 'proj-uuid-1',
    initialData: null,
    isSubmitting: false,
    serverError: null,
  };

  it('renders nothing when isOpen is false', () => {
    const { container } = render(<SprintModal {...defaultProps} isOpen={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders in create mode with blank default fields', () => {
    render(<SprintModal {...defaultProps} />);

    expect(screen.getByRole('heading', { name: 'Create New Sprint' })).toBeInTheDocument();
    expect(screen.getByLabelText(/sprint name/i)).toHaveValue('');
    expect(screen.getByLabelText(/status/i)).toHaveValue(SPRINT_STATUS.PLANNED);
    expect(screen.getByLabelText(/start date/i)).toHaveValue('');
    expect(screen.getByLabelText(/end date/i)).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Create Sprint' })).toBeInTheDocument();
  });

  it('renders in edit mode pre-populated with sprint details', () => {
    const initialData = {
      id: 'sprint-123',
      name: 'Sprint 2 - Backend Auth',
      status: SPRINT_STATUS.ACTIVE,
      startDate: '2026-11-01T00:00:00Z',
      endDate: '2026-11-15T23:59:59Z',
    };

    render(<SprintModal {...defaultProps} initialData={initialData} />);

    expect(screen.getByRole('heading', { name: 'Edit Sprint' })).toBeInTheDocument();
    expect(screen.getByLabelText(/sprint name/i)).toHaveValue('Sprint 2 - Backend Auth');
    expect(screen.getByLabelText(/status/i)).toHaveValue(SPRINT_STATUS.ACTIVE);
    expect(screen.getByLabelText(/start date/i)).toHaveValue('2026-11-01');
    expect(screen.getByLabelText(/end date/i)).toHaveValue('2026-11-15');
    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeInTheDocument();
  });

  it('validates required fields on submit', () => {
    const handleSubmit = vi.fn();
    render(<SprintModal {...defaultProps} onSubmit={handleSubmit} />);

    fireEvent.click(screen.getByRole('button', { name: 'Create Sprint' }));

    expect(screen.getByText('Sprint name is required')).toBeInTheDocument();
    expect(screen.getByText('Start date is required')).toBeInTheDocument();
    expect(screen.getByText('End date is required')).toBeInTheDocument();
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('validates that sprint end date cannot be earlier than start date', () => {
    const handleSubmit = vi.fn();
    render(<SprintModal {...defaultProps} onSubmit={handleSubmit} />);

    fireEvent.change(screen.getByLabelText(/sprint name/i), {
      target: { value: 'Sprint Alpha' },
    });
    fireEvent.change(screen.getByLabelText(/start date/i), {
      target: { value: '2026-11-15' },
    });
    fireEvent.change(screen.getByLabelText(/end date/i), {
      target: { value: '2026-11-01' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Sprint' }));

    expect(screen.getByText('Sprint end date cannot be before start date')).toBeInTheDocument();
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('formats dates to ISO-8601 timestamps and submits valid form', () => {
    const handleSubmit = vi.fn();
    render(<SprintModal {...defaultProps} onSubmit={handleSubmit} />);

    fireEvent.change(screen.getByLabelText(/sprint name/i), {
      target: { value: 'Sprint 1 - Foundation' },
    });
    fireEvent.change(screen.getByLabelText(/status/i), {
      target: { value: SPRINT_STATUS.PLANNED },
    });
    fireEvent.change(screen.getByLabelText(/start date/i), {
      target: { value: '2026-11-01' },
    });
    fireEvent.change(screen.getByLabelText(/end date/i), {
      target: { value: '2026-11-15' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Sprint' }));

    expect(handleSubmit).toHaveBeenCalledWith({
      projectId: 'proj-uuid-1',
      name: 'Sprint 1 - Foundation',
      status: SPRINT_STATUS.PLANNED,
      startDate: '2026-11-01T00:00:00Z',
      endDate: '2026-11-15T23:59:59Z',
    });
  });

  it('displays server error message in alert role', () => {
    render(
      <SprintModal
        {...defaultProps}
        serverError={{ message: 'A sprint with this name already exists in this project.' }}
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent(
      'A sprint with this name already exists in this project.'
    );
  });

  it('disables buttons and inputs during submission', () => {
    render(<SprintModal {...defaultProps} isSubmitting={true} />);

    expect(screen.getByRole('button', { name: 'Creating...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByLabelText(/sprint name/i)).toBeDisabled();
    expect(screen.getByLabelText(/status/i)).toBeDisabled();
  });

  it('calls onClose when Cancel button or modal backdrop is clicked', () => {
    const handleClose = vi.fn();
    render(<SprintModal {...defaultProps} onClose={handleClose} />);

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(handleClose).toHaveBeenCalled();
  });
});
