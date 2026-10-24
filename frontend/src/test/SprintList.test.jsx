import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import SprintList from '../components/projects/SprintList';
import sprintService from '../services/sprintService';
import { SPRINT_STATUS } from '../constants/projectEnums';

describe('SprintList Component', () => {
  const projectId = 'proj-uuid-1';

  const mockSprint1 = {
    id: 'sprint-1',
    projectId,
    name: 'Sprint 1 - Foundation',
    status: SPRINT_STATUS.COMPLETED,
    startDate: '2026-10-01T00:00:00Z',
    endDate: '2026-10-15T23:59:59Z',
  };

  const mockSprint2 = {
    id: 'sprint-2',
    projectId,
    name: 'Sprint 2 - Backend Auth',
    status: SPRINT_STATUS.ACTIVE,
    startDate: '2026-10-16T00:00:00Z',
    endDate: '2026-10-31T23:59:59Z',
  };

  const emptyPage = {
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };

  const populatedPage = {
    content: [mockSprint1, mockSprint2],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    first: true,
    last: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading state initially', async () => {
    vi.spyOn(sprintService, 'getProjectSprints').mockImplementationOnce(
      () => new Promise(() => {}) // pending promise
    );

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    expect(screen.getByText('Loading sprints...')).toBeInTheDocument();
  });

  it('renders empty state when no sprints exist', async () => {
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValueOnce(emptyPage);

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('No sprints created yet')).toBeInTheDocument();
    });
  });

  it('renders sprints list with status badges and action buttons for authorized users', async () => {
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValueOnce(populatedPage);

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
      expect(screen.getByText('Sprint 2 - Backend Auth')).toBeInTheDocument();
      expect(document.getElementById('btn-create-sprint')).toBeInTheDocument();
      expect(screen.getByLabelText('Edit Sprint 1 - Foundation')).toBeInTheDocument();
      expect(screen.getByLabelText('Delete Sprint 1 - Foundation')).toBeInTheDocument();
    });
  });

  it('hides create and action buttons when canManageSprints is false', async () => {
    vi.spyOn(sprintService, 'getProjectSprints').mockResolvedValueOnce(populatedPage);

    render(<SprintList projectId={projectId} canManageSprints={false} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    });

    expect(document.getElementById('btn-create-sprint')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Edit Sprint 1 - Foundation')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Delete Sprint 1 - Foundation')).not.toBeInTheDocument();
  });

  it('filters sprints by status when filter dropdown changes', async () => {
    const getSprintsSpy = vi.spyOn(sprintService, 'getProjectSprints')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce({
        content: [mockSprint2],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    });

    const filterSelect = screen.getByLabelText('Filter sprints by status');
    fireEvent.change(filterSelect, { target: { value: SPRINT_STATUS.ACTIVE } });

    await waitFor(() => {
      expect(getSprintsSpy).toHaveBeenLastCalledWith(projectId, {
        status: SPRINT_STATUS.ACTIVE,
        page: 0,
        size: 10,
        sort: 'startDate,asc',
      });
    });
  });

  it('changes sorting when sort dropdown changes', async () => {
    const getSprintsSpy = vi.spyOn(sprintService, 'getProjectSprints')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce(populatedPage);

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    });

    const sortSelect = screen.getByLabelText('Sort sprints');
    fireEvent.change(sortSelect, { target: { value: 'name,asc' } });

    await waitFor(() => {
      expect(getSprintsSpy).toHaveBeenLastCalledWith(projectId, {
        status: undefined,
        page: 0,
        size: 10,
        sort: 'name,asc',
      });
    });
  });

  it('opens create modal, creates a sprint, and refreshes the list', async () => {
    vi.spyOn(sprintService, 'getProjectSprints')
      .mockResolvedValueOnce(emptyPage)
      .mockResolvedValueOnce(populatedPage);

    const createSpy = vi.spyOn(sprintService, 'createSprint').mockResolvedValueOnce(mockSprint1);

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('No sprints created yet')).toBeInTheDocument();
    });

    fireEvent.click(document.getElementById('btn-create-sprint'));

    expect(screen.getByRole('heading', { name: 'Create New Sprint' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/sprint name/i), {
      target: { value: 'Sprint 1 - Foundation' },
    });
    fireEvent.change(screen.getByLabelText(/start date/i), {
      target: { value: '2026-10-01' },
    });
    fireEvent.change(screen.getByLabelText(/end date/i), {
      target: { value: '2026-10-15' },
    });

    // Modal submit button is labeled 'Create Sprint'
    const modalSubmitBtn = screen.getAllByRole('button', { name: 'Create Sprint' }).pop();
    fireEvent.click(modalSubmitBtn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        projectId,
        name: 'Sprint 1 - Foundation',
        status: SPRINT_STATUS.PLANNED,
        startDate: '2026-10-01T00:00:00Z',
        endDate: '2026-10-15T23:59:59Z',
      });
      expect(screen.getByText('Sprint created successfully.')).toBeInTheDocument();
    });
  });

  it('opens edit modal, updates a sprint, and refreshes the list', async () => {
    vi.spyOn(sprintService, 'getProjectSprints')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce(populatedPage);

    const updateSpy = vi.spyOn(sprintService, 'updateSprint').mockResolvedValueOnce({
      ...mockSprint1,
      name: 'Sprint 1 - Foundation Updated',
    });

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Edit Sprint 1 - Foundation'));

    expect(screen.getByRole('heading', { name: 'Edit Sprint' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/sprint name/i), {
      target: { value: 'Sprint 1 - Foundation Updated' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('sprint-1', {
        name: 'Sprint 1 - Foundation Updated',
        status: SPRINT_STATUS.COMPLETED,
        startDate: '2026-10-01T00:00:00Z',
        endDate: '2026-10-15T23:59:59Z',
      });
      expect(screen.getByText('Sprint updated successfully.')).toBeInTheDocument();
    });
  });

  it('opens delete confirmation and deletes sprint', async () => {
    vi.spyOn(sprintService, 'getProjectSprints')
      .mockResolvedValueOnce(populatedPage)
      .mockResolvedValueOnce(emptyPage);

    const deleteSpy = vi.spyOn(sprintService, 'deleteSprint').mockResolvedValueOnce(null);

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByText('Sprint 1 - Foundation')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Delete Sprint 1 - Foundation'));

    expect(screen.getByRole('heading', { name: 'Delete Sprint' })).toBeInTheDocument();
    expect(
      screen.getByText(/are you sure you want to delete "sprint 1 - foundation"\?/i)
    ).toBeInTheDocument();

    // Confirm delete inside modal
    const confirmDeleteBtn = screen.getAllByRole('button', { name: 'Delete Sprint' }).pop();
    fireEvent.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('sprint-1');
      expect(screen.getByText('Sprint "Sprint 1 - Foundation" was deleted.')).toBeInTheDocument();
    });
  });

  it('displays error state when loading sprints fails', async () => {
    vi.spyOn(sprintService, 'getProjectSprints').mockRejectedValueOnce({
      normalized: { status: 500, message: 'Server error loading sprints.' },
    });

    render(<SprintList projectId={projectId} canManageSprints={true} />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText('Server error loading sprints.')).toBeInTheDocument();
    });
  });
});
