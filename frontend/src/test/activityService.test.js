import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import activityService from '../services/activityService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('activityService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockActivity = {
    id: 'activity-1',
    issueId: 'issue-100',
    userId: 'user-200',
    action: 'STATUS_CHANGED',
    oldValue: 'TODO',
    newValue: 'IN_PROGRESS',
    createdAt: '2026-10-15T11:00:00Z',
  };

  describe('getActivitiesByIssue', () => {
    it('fetches paginated activities with default parameters', async () => {
      const mockPageResponse = {
        content: [mockActivity],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await activityService.getActivitiesByIssue('issue-100');

      expect(api.get).toHaveBeenCalledWith('/api/activities/issue/issue-100', {
        params: {
          page: 0,
          size: 20,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('passes custom pagination parameters', async () => {
      api.get.mockResolvedValueOnce({ data: { content: [] } });

      await activityService.getActivitiesByIssue('issue-100', { page: 1, size: 10, sort: 'createdAt,asc' });

      expect(api.get).toHaveBeenCalledWith('/api/activities/issue/issue-100', {
        params: {
          page: 1,
          size: 10,
          sort: 'createdAt,asc',
        },
      });
    });
  });
});
