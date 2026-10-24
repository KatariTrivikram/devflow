import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import notificationService from '../services/notificationService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('notificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockNotification = {
    id: 'notif-1',
    type: 'ISSUE_ASSIGNED',
    title: 'You were assigned an issue',
    message: 'Issue #101 was assigned to you',
    referenceType: 'ISSUE',
    referenceId: 'issue-101',
    actorId: 'user-1',
    actorName: 'Alice',
    read: false,
    createdAt: '2026-10-15T10:00:00Z',
  };

  describe('getNotifications', () => {
    it('fetches paginated notifications with default parameters', async () => {
      const mockPageResponse = {
        content: [mockNotification],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await notificationService.getNotifications();

      expect(api.get).toHaveBeenCalledWith('/api/notifications', {
        params: {
          page: 0,
          size: 20,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('passes unreadOnly filter and custom pagination', async () => {
      api.get.mockResolvedValueOnce({ data: { content: [] } });

      await notificationService.getNotifications({ unreadOnly: true, page: 2, size: 10, sort: 'createdAt,asc' });

      expect(api.get).toHaveBeenCalledWith('/api/notifications', {
        params: {
          unreadOnly: true,
          page: 2,
          size: 10,
          sort: 'createdAt,asc',
        },
      });
    });
  });

  describe('getUnreadCount', () => {
    it('fetches unread count from /api/notifications/unread-count', async () => {
      api.get.mockResolvedValueOnce({ data: { count: 3 } });

      const result = await notificationService.getUnreadCount();

      expect(api.get).toHaveBeenCalledWith('/api/notifications/unread-count');
      expect(result).toEqual({ count: 3 });
    });
  });

  describe('markAsRead', () => {
    it('sends PATCH request to mark single notification read', async () => {
      api.patch.mockResolvedValueOnce({ data: { ...mockNotification, read: true } });

      const result = await notificationService.markAsRead('notif-1');

      expect(api.patch).toHaveBeenCalledWith('/api/notifications/notif-1/read');
      expect(result.read).toBe(true);
    });
  });

  describe('markAllAsRead', () => {
    it('sends PATCH request to /api/notifications/read-all', async () => {
      api.patch.mockResolvedValueOnce({ data: { message: 'All notifications marked as read', updatedCount: 4 } });

      const result = await notificationService.markAllAsRead();

      expect(api.patch).toHaveBeenCalledWith('/api/notifications/read-all');
      expect(result.updatedCount).toBe(4);
    });
  });
});
