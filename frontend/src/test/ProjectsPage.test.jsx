import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ProjectsPage from '../pages/projects/ProjectsPage';
import projectService from '../services/projectService';
import { PROJECT_STATUS } from '../constants/projectEnums';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ProjectsPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const sampleProjects = [
    {
      id: 'proj-1',
      name: 'Alpha Engine',
      description: 'Core backend engine',
      status: PROJECT_STATUS.ACTIVE,
      createdAt: '2026-10-10T10:00:00Z',
    },
    {
      id: 'proj-2',
      name: 'Beta Portal',
      description: 'Customer facing web portal',
      status: PROJECT_STATUS.COMPLETED,
      createdAt: '2026-09-15T08:30:00Z',
    },
  ];

  it('renders page header and fetches projects list on mount', async () => {
    vi.spyOn(projectService, 'getProjects').mockResolvedValueOnce({
      content: sampleProjects,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: 'Projects' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /new project/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
      expect(screen.getByText('Beta Portal')).toBeInTheDocument();
    });

    expect(screen.getByText('Core backend engine')).toBeInTheDocument();
    expect(screen.getByText('Customer facing web portal')).toBeInTheDocument();
    expect(screen.getByText(/showing 1 to 2 of 2 projects/i)).toBeInTheDocument();
  });

  it('displays empty state when no projects exist in workspace', async () => {
    vi.spyOn(projectService, 'getProjects').mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/no projects yet/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Create Project' })).toBeInTheDocument();
    });
  });

  it('displays error state and retries fetching when API fails', async () => {
    const getSpy = vi.spyOn(projectService, 'getProjects')
      .mockRejectedValueOnce({
        normalized: { message: 'Network connection failed' },
      })
      .mockResolvedValueOnce({
        content: sampleProjects,
        page: 0,
        size: 10,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/unable to load projects/i)).toBeInTheDocument();
      expect(screen.getByText('Network connection failed')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /try again/i }));

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
    });

    expect(getSpy).toHaveBeenCalledTimes(2);
  });

  it('filters projects by status when dropdown selection changes', async () => {
    const getSpy = vi.spyOn(projectService, 'getProjects').mockResolvedValue({
      content: [sampleProjects[0]],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/filter by status/i), {
      target: { value: PROJECT_STATUS.ACTIVE },
    });

    await waitFor(() => {
      expect(getSpy).toHaveBeenCalledWith(
        expect.objectContaining({ status: PROJECT_STATUS.ACTIVE })
      );
    });
  });

  it('opens create modal, creates project, and navigates to new project page', async () => {
    vi.spyOn(projectService, 'getProjects').mockResolvedValue({
      content: sampleProjects,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    const createSpy = vi.spyOn(projectService, 'createProject').mockResolvedValueOnce({
      id: 'new-proj-id',
      name: 'Gamma Service',
      description: 'Microservice Gamma',
      status: PROJECT_STATUS.ACTIVE,
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /new project/i }));

    expect(screen.getByRole('heading', { name: 'Create New Project' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/project name/i), {
      target: { value: 'Gamma Service' },
    });
    fireEvent.change(screen.getByLabelText(/description/i), {
      target: { value: 'Microservice Gamma' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Create Project' }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        name: 'Gamma Service',
        description: 'Microservice Gamma',
        status: PROJECT_STATUS.ACTIVE,
      });
      expect(mockNavigate).toHaveBeenCalledWith('/projects/new-proj-id');
    });
  });

  it('opens edit modal and updates project details', async () => {
    vi.spyOn(projectService, 'getProjects').mockResolvedValue({
      content: sampleProjects,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    const updateSpy = vi.spyOn(projectService, 'updateProject').mockResolvedValueOnce({
      id: 'proj-1',
      name: 'Alpha Engine Refactored',
      description: 'Core backend engine v2',
      status: PROJECT_STATUS.ACTIVE,
    });

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Edit Alpha Engine'));

    expect(screen.getByRole('heading', { name: 'Edit Project' })).toBeInTheDocument();
    expect(screen.getByLabelText(/project name/i)).toHaveValue('Alpha Engine');

    fireEvent.change(screen.getByLabelText(/project name/i), {
      target: { value: 'Alpha Engine Refactored' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('proj-1', {
        name: 'Alpha Engine Refactored',
        description: 'Core backend engine',
        status: PROJECT_STATUS.ACTIVE,
      });
    });
  });

  it('prompts confirmation dialog and deletes project', async () => {
    vi.spyOn(projectService, 'getProjects').mockResolvedValue({
      content: sampleProjects,
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    const deleteSpy = vi.spyOn(projectService, 'deleteProject').mockResolvedValueOnce(null);

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Engine')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Delete Alpha Engine'));

    expect(screen.getByRole('heading', { name: 'Delete Project' })).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to delete "alpha engine"\?/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Delete Project' }));

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('proj-1');
    });
  });
});
