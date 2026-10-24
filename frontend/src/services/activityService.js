import api from './api';

/**
 * Activity API Service aligning strictly with Spring Boot ActivityController endpoints.
 */
export const activityService = {
  /**
   * Retrieve paginated audit activity entries for an issue.
   * Endpoint: GET /api/activities/issue/:issueId
   *
   * @param {string} issueId - Issue UUID
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.sort='createdAt,desc'] - Sort field and direction
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getActivitiesByIssue(issueId, params = {}) {
    const { page = 0, size = 20, sort = 'createdAt,desc' } = params;
    const response = await api.get(`/api/activities/issue/${issueId}`, {
      params: { page, size, sort },
    });
    return response.data;
  },

  /**
   * Retrieve recent audit activity entries for a project across all its issues.
   * Endpoint: GET /api/activities/project/:projectId
   *
   * @param {string} projectId - Project UUID
   * @param {number} [limit=10] - Number of items to retrieve
   * @returns {Promise<Array>} List of ActivityResponse
   */
  async getActivitiesByProject(projectId, limit = 10) {
    const response = await api.get(`/api/activities/project/${projectId}`, {
      params: { limit },
    });
    return response.data;
  },
};

export default activityService;
