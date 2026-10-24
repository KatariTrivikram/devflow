import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ProjectModal from '../components/projects/ProjectModal';
import { PROJECT_STATUS } from '../constants/projectEnums';

describe('ProjectModal Component', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ProjectModal isOpen={false} onClose={vi.fn()} onSubmit={vi.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders in create mode with default fields', () => {
    render(
      <ProjectModal isOpen={true} onClose={vi.fn()} onSubmit={vi.fn()} />
    );

    expect(screen.getByRole('heading', { name: 'Create New Project' })).toBeInTheDocument();
    expect(screen.getByLabelText(/project name/i)).toHaveValue('');
    expect(screen.getByLabelText(/description/i)).toHaveValue('');
    expect(screen.getByLabelText(/status/i)).toHaveValue(PROJECT_STATUS.ACTIVE);
    expect(screen.getByRole('button', { name: 'Create Project' })).toBeInTheDocument();
  });

  it('renders in edit mode with initial data prefilled', () => {
    const initialData = {
      id: 'proj-123',
      name: 'Existing Project',
      description: 'Existing Description',
      status: PROJECT_STATUS.COMPLETED,
    };

    render(
      <ProjectModal
        isOpen={true}
        initialData={initialData}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { name: 'Edit Project' })).toBeInTheDocument();
    expect(screen.getByLabelText(/project name/i)).toHaveValue('Existing Project');
    expect(screen.getByLabelText(/description/i)).toHaveValue('Existing Description');
    expect(screen.getByLabelText(/status/i)).toHaveValue(PROJECT_STATUS.COMPLETED);
    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeInTheDocument();
  });

  it('displays client validation error when project name is empty', () => {
    const onSubmit = vi.fn();
    render(
      <ProjectModal isOpen={true} onClose={vi.fn()} onSubmit={onSubmit} />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Create Project' }));

    expect(screen.getByRole('alert')).toHaveTextContent(/project name is required/i);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with sanitized data when valid', () => {
    const onSubmit = vi.fn();
    render(
      <ProjectModal isOpen={true} onClose={vi.fn()} onSubmit={onSubmit} />
    );

    fireEvent.change(screen.getByLabelText(/project name/i), {
      target: { value: '  DevFlow New Module  ' },
    });
    fireEvent.change(screen.getByLabelText(/description/i), {
      target: { value: '  A new module for workflows  ' },
    });
    fireEvent.change(screen.getByLabelText(/status/i), {
      target: { value: PROJECT_STATUS.ARCHIVED },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Project' }));

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'DevFlow New Module',
      description: 'A new module for workflows',
      status: PROJECT_STATUS.ARCHIVED,
    });
  });

  it('displays server error message and field-level validation errors', () => {
    const serverError = {
      status: 400,
      message: 'Validation failed for project payload',
      errors: {
        name: 'Project name already in use within workspace',
      },
    };

    render(
      <ProjectModal
        isOpen={true}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
        serverError={serverError}
      />
    );

    expect(screen.getByText('Validation failed for project payload')).toBeInTheDocument();
    expect(screen.getAllByText('Project name already in use within workspace').length).toBeGreaterThan(0);
  });

  it('triggers onClose when Cancel button or close icon is clicked', () => {
    const onClose = vi.fn();
    render(
      <ProjectModal isOpen={true} onClose={onClose} onSubmit={vi.fn()} />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByLabelText('Close modal'));
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
