import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import sprintService from '../services/sprintService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('sprintService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockSprint = {
    id: 'sprint-uuid-1',
    projectId: 'proj-uuid-1',
    name: 'Sprint 1 - Foundation',
    status: 'PLANNED',
    startDate: '2026-11-01T00:00:00Z',
    endDate: '2026-11-15T23:59:59Z',
    createdAt: '2026-10-14T10:00:00Z',
    updatedAt: '2026-10-14T10:00:00Z',
  };

  describe('getProjectSprints', () => {
    it('fetches paginated sprints with default parameters', async () => {
      const mockPageResponse = {
        content: [mockSprint],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await sprintService.getProjectSprints('proj-uuid-1');

      expect(api.get).toHaveBeenCalledWith('/api/projects/proj-uuid-1/sprints', {
        params: {
          page: 0,
          size: 10,
          sort: 'startDate,asc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('passes custom filter status, pagination, and sorting', async () => {
      api.get.mockResolvedValueOnce({ data: { content: [] } });

      await sprintService.getProjectSprints('proj-uuid-1', {
        status: 'ACTIVE',
        page: 2,
        size: 20,
        sort: 'name,desc',
      });

      expect(api.get).toHaveBeenCalledWith('/api/projects/proj-uuid-1/sprints', {
        params: {
          status: 'ACTIVE',
          page: 2,
          size: 20,
          sort: 'name,desc',
        },
      });
    });
  });

  describe('getSprintById', () => {
    it('fetches single sprint by ID', async () => {
      api.get.mockResolvedValueOnce({ data: mockSprint });

      const result = await sprintService.getSprintById('sprint-uuid-1');

      expect(api.get).toHaveBeenCalledWith('/api/sprints/sprint-uuid-1');
      expect(result).toEqual(mockSprint);
    });
  });

  describe('createSprint', () => {
    it('creates a new sprint with projectId and payload', async () => {
      api.post.mockResolvedValueOnce({ data: mockSprint });

      const payload = {
        projectId: 'proj-uuid-1',
        name: 'Sprint 1 - Foundation',
        startDate: '2026-11-01T00:00:00Z',
        endDate: '2026-11-15T23:59:59Z',
        status: 'PLANNED',
      };

      const result = await sprintService.createSprint(payload);

      expect(api.post).toHaveBeenCalledWith('/api/sprints', payload);
      expect(result).toEqual(mockSprint);
    });
  });

  describe('updateSprint', () => {
    it('updates an existing sprint by ID', async () => {
      const updated = { ...mockSprint, name: 'Sprint 1 - Enhanced' };
      api.put.mockResolvedValueOnce({ data: updated });

      const payload = {
        name: 'Sprint 1 - Enhanced',
        startDate: '2026-11-01T00:00:00Z',
        endDate: '2026-11-15T23:59:59Z',
        status: 'ACTIVE',
      };

      const result = await sprintService.updateSprint('sprint-uuid-1', payload);

      expect(api.put).toHaveBeenCalledWith('/api/sprints/sprint-uuid-1', payload);
      expect(result).toEqual(updated);
    });
  });

  describe('deleteSprint', () => {
    it('deletes sprint by ID and returns null data', async () => {
      api.delete.mockResolvedValueOnce({ data: null });

      const result = await sprintService.deleteSprint('sprint-uuid-1');

      expect(api.delete).toHaveBeenCalledWith('/api/sprints/sprint-uuid-1');
      expect(result).toBeNull();
    });
  });
});
