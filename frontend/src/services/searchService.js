import api from './api';

/**
 * Global Search API Service connecting to DevFlow Spring Boot SearchController endpoints.
 */
export const searchService = {
  /**
   * Execute global search across accessible Projects, Issues, and Sprints.
   * Endpoint: GET /api/search
   *
   * @param {Object} params
   * @param {string} params.q - Search keyword (required)
   * @param {string} [params.type='ALL'] - Resource type filter (ALL, PROJECT, ISSUE, SPRINT)
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=10] - Page size
   * @param {string} [params.sort='updatedAt,desc'] - Sort field and direction
   * @returns {Promise<{ query: string, type: string, content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async search(params = {}) {
    const { q, type = 'ALL', page = 0, size = 10, sort = 'updatedAt,desc' } = params;

    const queryParams = {
      q: q?.trim(),
      type,
      page,
      size,
      sort,
    };

    const response = await api.get('/api/search', { params: queryParams });
    return response.data;
  },
};

export default searchService;
