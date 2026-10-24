import { describe, it, expect, vi, afterEach } from 'vitest';
import api from '../services/api';
import projectService from '../services/projectService';
import { PROJECT_STATUS } from '../constants/projectEnums';

describe('projectService', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('getProjects requests GET /api/projects with default parameters', async () => {
    const mockPageResponse = {
      content: [{ id: 'p1', name: 'DevFlow Platform', status: 'ACTIVE' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockPageResponse });

    const result = await projectService.getProjects();

    expect(getSpy).toHaveBeenCalledWith('/api/projects', {
      params: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
      },
    });
    expect(result).toEqual(mockPageResponse);
  });

  it('getProjects includes status and search parameters when provided', async () => {
    const mockPageResponse = {
      content: [],
      page: 1,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockPageResponse });

    await projectService.getProjects({
      status: PROJECT_STATUS.ACTIVE,
      search: 'core engine',
      page: 1,
      size: 10,
      sort: 'name,asc',
    });

    expect(getSpy).toHaveBeenCalledWith('/api/projects', {
      params: {
        status: 'ACTIVE',
        search: 'core engine',
        page: 1,
        size: 10,
        sort: 'name,asc',
      },
    });
  });

  it('getProjectById requests GET /api/projects/:id', async () => {
    const mockProject = {
      id: 'uuid-1234',
      name: 'Analytics Service',
      description: 'Analytics microservice',
      status: 'ACTIVE',
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockProject });

    const result = await projectService.getProjectById('uuid-1234');

    expect(getSpy).toHaveBeenCalledWith('/api/projects/uuid-1234');
    expect(result).toEqual(mockProject);
  });

  it('createProject requests POST /api/projects with name, description, and status without client createdById', async () => {
    const payload = {
      name: 'New Platform',
      description: 'Next-gen platform',
      status: 'ACTIVE',
    };

    const mockResponse = {
      id: 'new-uuid',
      ...payload,
      createdAt: '2026-10-14T10:00:00Z',
    };

    const postSpy = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: mockResponse });

    const result = await projectService.createProject(payload);

    expect(postSpy).toHaveBeenCalledWith('/api/projects', {
      name: 'New Platform',
      description: 'Next-gen platform',
      status: 'ACTIVE',
    });
    // Ensure createdById was not sent in payload
    expect(postSpy.mock.calls[0][1]).not.toHaveProperty('createdById');
    expect(result).toEqual(mockResponse);
  });

  it('updateProject requests PUT /api/projects/:id with updated fields', async () => {
    const payload = {
      name: 'Updated Platform Name',
      description: 'Updated description',
      status: 'COMPLETED',
    };

    const mockResponse = {
      id: 'uuid-1234',
      ...payload,
      updatedAt: '2026-10-14T12:00:00Z',
    };

    const putSpy = vi.spyOn(api, 'put').mockResolvedValueOnce({ data: mockResponse });

    const result = await projectService.updateProject('uuid-1234', payload);

    expect(putSpy).toHaveBeenCalledWith('/api/projects/uuid-1234', {
      name: 'Updated Platform Name',
      description: 'Updated description',
      status: 'COMPLETED',
    });
    expect(result).toEqual(mockResponse);
  });

  it('deleteProject requests DELETE /api/projects/:id', async () => {
    const deleteSpy = vi.spyOn(api, 'delete').mockResolvedValueOnce({ data: null });

    await projectService.deleteProject('uuid-1234');

    expect(deleteSpy).toHaveBeenCalledWith('/api/projects/uuid-1234');
  });

  it('getProjectSprints requests GET /api/projects/:projectId/sprints', async () => {
    const mockSprintsResponse = {
      content: [{ id: 'sprint-1', name: 'Sprint 1', status: 'ACTIVE' }],
      page: 0,
      size: 5,
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockSprintsResponse });

    const result = await projectService.getProjectSprints('uuid-1234', { size: 5 });

    expect(getSpy).toHaveBeenCalledWith('/api/projects/uuid-1234/sprints', {
      params: {
        page: 0,
        size: 5,
        sort: 'startDate,asc',
      },
    });
    expect(result).toEqual(mockSprintsResponse);
  });
});
