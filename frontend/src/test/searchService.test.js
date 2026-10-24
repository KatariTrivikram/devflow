import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import searchService from '../services/searchService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('searchService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockSearchResult = {
    id: 'res-1',
    type: 'ISSUE',
    title: 'Fix auth race condition',
    description: 'Investigating token expiry race',
    projectId: 'proj-1',
    projectName: 'DevFlow Platform',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    updatedAt: '2026-10-15T10:00:00Z',
  };

  const mockSearchResponse = {
    query: 'auth',
    type: 'ALL',
    content: [mockSearchResult],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  };

  it('searches with default type, pagination, and sort', async () => {
    api.get.mockResolvedValueOnce({ data: mockSearchResponse });

    const result = await searchService.search({ q: 'auth' });

    expect(api.get).toHaveBeenCalledWith('/api/search', {
      params: {
        q: 'auth',
        type: 'ALL',
        page: 0,
        size: 10,
        sort: 'updatedAt,desc',
      },
    });
    expect(result).toEqual(mockSearchResponse);
  });

  it('passes custom resource type, pagination, and sort', async () => {
    api.get.mockResolvedValueOnce({ data: { ...mockSearchResponse, type: 'ISSUE' } });

    await searchService.search({
      q: '  database  ',
      type: 'ISSUE',
      page: 2,
      size: 20,
      sort: 'createdAt,desc',
    });

    expect(api.get).toHaveBeenCalledWith('/api/search', {
      params: {
        q: 'database',
        type: 'ISSUE',
        page: 2,
        size: 20,
        sort: 'createdAt,desc',
      },
    });
  });

  it('propagates API errors correctly', async () => {
    const error = new Error('Network error');
    api.get.mockRejectedValueOnce(error);

    await expect(searchService.search({ q: 'crash' })).rejects.toThrow('Network error');
  });
});
