import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import commentService from '../services/commentService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('commentService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockComment = {
    id: 'comment-1',
    issueId: 'issue-100',
    userId: 'user-200',
    content: 'Investigation complete, root cause identified.',
    createdAt: '2026-10-15T10:00:00Z',
    updatedAt: '2026-10-15T10:00:00Z',
  };

  describe('getCommentsByIssue', () => {
    it('fetches paginated comments with default parameters', async () => {
      const mockPageResponse = {
        content: [mockComment],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await commentService.getCommentsByIssue('issue-100');

      expect(api.get).toHaveBeenCalledWith('/api/comments/issue/issue-100', {
        params: {
          page: 0,
          size: 20,
          sort: 'createdAt,asc',
        },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it('passes custom pagination parameters', async () => {
      api.get.mockResolvedValueOnce({ data: { content: [] } });

      await commentService.getCommentsByIssue('issue-100', { page: 2, size: 10, sort: 'createdAt,desc' });

      expect(api.get).toHaveBeenCalledWith('/api/comments/issue/issue-100', {
        params: {
          page: 2,
          size: 10,
          sort: 'createdAt,desc',
        },
      });
    });
  });

  describe('getCommentById', () => {
    it('fetches comment by id', async () => {
      api.get.mockResolvedValueOnce({ data: mockComment });

      const result = await commentService.getCommentById('comment-1');

      expect(api.get).toHaveBeenCalledWith('/api/comments/comment-1');
      expect(result).toEqual(mockComment);
    });
  });

  describe('createComment', () => {
    it('creates comment with issueId, userId, and content', async () => {
      const payload = {
        issueId: 'issue-100',
        userId: 'user-200',
        content: 'New comment text',
      };
      api.post.mockResolvedValueOnce({ data: { ...mockComment, ...payload } });

      const result = await commentService.createComment(payload);

      expect(api.post).toHaveBeenCalledWith('/api/comments', payload);
      expect(result.content).toBe('New comment text');
    });
  });

  describe('updateComment', () => {
    it('updates comment content by id', async () => {
      const updateData = { content: 'Updated comment text' };
      api.put.mockResolvedValueOnce({ data: { ...mockComment, content: 'Updated comment text' } });

      const result = await commentService.updateComment('comment-1', updateData);

      expect(api.put).toHaveBeenCalledWith('/api/comments/comment-1', updateData);
      expect(result.content).toBe('Updated comment text');
    });
  });

  describe('deleteComment', () => {
    it('deletes comment by id', async () => {
      api.delete.mockResolvedValueOnce({ data: null });

      const result = await commentService.deleteComment('comment-1');

      expect(api.delete).toHaveBeenCalledWith('/api/comments/comment-1');
      expect(result).toBeNull();
    });
  });
});
