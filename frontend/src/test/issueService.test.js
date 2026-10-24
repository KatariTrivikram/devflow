import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import issueService from '../services/issueService';
import { ISSUE_TYPE, ISSUE_PRIORITY, ISSUE_STATUS } from '../constants/issueEnums';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('issueService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockIssue = {
    id: 'issue-uuid-1',
    projectId: 'proj-uuid-1',
    sprintId: 'sprint-uuid-1',
    assignedToId: 'user-uuid-1',
    title: 'Fix database connection timeout',
    description: 'Optimize pool size and timeout thresholds',
    type: ISSUE_TYPE.BUG,
    priority: ISSUE_PRIORITY.HIGH,
    status: ISSUE_STATUS.IN_PROGRESS,
    createdAt: '2026-10-14T10:00:00Z',
    updatedAt: '2026-10-14T12:00:00Z',
  };

  describe('getIssues', () => {
    it('fetches paginated issues with default parameters', async () => {
      const mockPageResponse = {
        content: [mockIssue],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await issueService.getIssues();

      expect(api.get).toHaveBeenCalledWith('/api/issues', {
        params: {
          page: 0,
          size: 10,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('passes custom filters, search term, pagination, and sorting', async () => {
      api.get.mockResolvedValueOnce({ data: { content: [] } });

      await issueService.getIssues({
        projectId: 'proj-uuid-1',
        sprintId: 'sprint-uuid-1',
        assignedToId: 'user-uuid-1',
        status: ISSUE_STATUS.DONE,
        priority: ISSUE_PRIORITY.CRITICAL,
        type: ISSUE_TYPE.FEATURE,
        search: 'authentication',
        page: 2,
        size: 25,
        sort: 'priority,desc',
      });

      expect(api.get).toHaveBeenCalledWith('/api/issues', {
        params: {
          projectId: 'proj-uuid-1',
          sprintId: 'sprint-uuid-1',
          assignedToId: 'user-uuid-1',
          status: ISSUE_STATUS.DONE,
          priority: ISSUE_PRIORITY.CRITICAL,
          type: ISSUE_TYPE.FEATURE,
          search: 'authentication',
          page: 2,
          size: 25,
          sort: 'priority,desc',
        },
      });
    });
  });

  describe('getIssueById', () => {
    it('fetches single issue by ID', async () => {
      api.get.mockResolvedValueOnce({ data: mockIssue });

      const result = await issueService.getIssueById('issue-uuid-1');

      expect(api.get).toHaveBeenCalledWith('/api/issues/issue-uuid-1');
      expect(result).toEqual(mockIssue);
    });
  });

  describe('createIssue', () => {
    it('creates a new issue with payload', async () => {
      api.post.mockResolvedValueOnce({ data: mockIssue });

      const payload = {
        projectId: 'proj-uuid-1',
        title: 'New Feature Task',
        description: 'Detailed description',
        type: ISSUE_TYPE.FEATURE,
        priority: ISSUE_PRIORITY.MEDIUM,
        status: ISSUE_STATUS.TODO,
        sprintId: 'sprint-uuid-1',
        assignedToId: 'user-uuid-1',
      };

      const result = await issueService.createIssue(payload);

      expect(api.post).toHaveBeenCalledWith('/api/issues', payload);
      expect(result).toEqual(mockIssue);
    });
  });

  describe('updateIssue', () => {
    it('updates an existing issue by ID', async () => {
      const updated = { ...mockIssue, status: ISSUE_STATUS.DONE };
      api.put.mockResolvedValueOnce({ data: updated });

      const payload = {
        status: ISSUE_STATUS.DONE,
      };

      const result = await issueService.updateIssue('issue-uuid-1', payload);

      expect(api.put).toHaveBeenCalledWith('/api/issues/issue-uuid-1', payload);
      expect(result).toEqual(updated);
    });
  });

  describe('deleteIssue', () => {
    it('deletes issue by ID and returns null data', async () => {
      api.delete.mockResolvedValueOnce({ data: null });

      const result = await issueService.deleteIssue('issue-uuid-1');

      expect(api.delete).toHaveBeenCalledWith('/api/issues/issue-uuid-1');
      expect(result).toBeNull();
    });
  });
});
