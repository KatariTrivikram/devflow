import api from './api';

/**
 * Issue API Service aligning strictly with Spring Boot IssueController endpoints.
 */
export const issueService = {
  /**
   * Retrieve paginated issues with dynamic multi-criteria filtering, search, and sorting.
   * Endpoint: GET /api/issues
   *
   * @param {Object} [params]
   * @param {string} [params.projectId] - Project UUID
   * @param {string} [params.sprintId] - Sprint UUID
   * @param {string} [params.assignedToId] - Assignee user UUID
   * @param {string} [params.status] - IssueStatus (TODO, IN_PROGRESS, CODE_REVIEW, DONE)
   * @param {string} [params.priority] - IssuePriority (LOW, MEDIUM, HIGH, CRITICAL)
   * @param {string} [params.type] - IssueType (BUG, FEATURE, TASK, IMPROVEMENT)
   * @param {string} [params.search] - Substring search matching title or description
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=10] - Page size
   * @param {string} [params.sort='createdAt,desc'] - Allowed sort fields: createdAt, updatedAt, title, priority, status, type
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getIssues(params = {}) {
    const {
      projectId,
      sprintId,
      noSprint,
      assignedToId,
      status,
      priority,
      type,
      search,
      page = 0,
      size = 10,
      sort = 'createdAt,desc',
    } = params;

    const queryParams = {
      page,
      size,
      sort,
    };

    if (projectId) queryParams.projectId = projectId;
    if (sprintId) queryParams.sprintId = sprintId;
    if (noSprint) queryParams.noSprint = true;
    if (assignedToId) queryParams.assignedToId = assignedToId;
    if (status) queryParams.status = status;
    if (priority) queryParams.priority = priority;
    if (type) queryParams.type = type;
    if (search && search.trim() !== '') queryParams.search = search.trim();

    const response = await api.get('/api/issues', { params: queryParams });
    return response.data;
  },

  /**
   * Retrieve a single issue by unique ID.
   * Endpoint: GET /api/issues/:id
   *
   * @param {string} id - Issue UUID
   * @returns {Promise<Object>} IssueResponse
   */
  async getIssueById(id) {
    const response = await api.get(`/api/issues/${id}`);
    return response.data;
  },

  /**
   * Create a new issue.
   * Endpoint: POST /api/issues
   *
   * @param {Object} payload
   * @param {string} payload.projectId - Required Project UUID
   * @param {string} payload.title - Required Issue title (max 200 chars)
   * @param {string} [payload.description] - Optional detailed description
   * @param {string} payload.type - Required IssueType (BUG, FEATURE, TASK, IMPROVEMENT)
   * @param {string} payload.priority - Required IssuePriority (LOW, MEDIUM, HIGH, CRITICAL)
   * @param {string} payload.status - Required IssueStatus (TODO, IN_PROGRESS, CODE_REVIEW, DONE)
   * @param {string} [payload.sprintId] - Optional Sprint UUID
   * @param {string} [payload.assignedToId] - Optional Assignee UUID
   * @returns {Promise<Object>} Created IssueResponse
   */
  async createIssue(payload) {
    const response = await api.post('/api/issues', payload);
    return response.data;
  },

  /**
   * Update an existing issue.
   * Endpoint: PUT /api/issues/:id
   *
   * @param {string} id - Issue UUID
   * @param {Object} payload - Fields to update
   * @param {string} [payload.title] - Updated title
   * @param {string} [payload.description] - Updated description
   * @param {string} [payload.type] - Updated IssueType
   * @param {string} [payload.priority] - Updated IssuePriority
   * @param {string} [payload.status] - Updated IssueStatus
   * @param {string} [payload.sprintId] - Updated Sprint UUID
   * @param {string} [payload.assignedToId] - Updated Assignee UUID
   * @returns {Promise<Object>} Updated IssueResponse
   */
  async updateIssue(id, payload) {
    const response = await api.put(`/api/issues/${id}`, payload);
    return response.data;
  },

  /**
   * Delete an issue.
   * Endpoint: DELETE /api/issues/:id
   *
   * @param {string} id - Issue UUID
   * @returns {Promise<void>}
   */
  async deleteIssue(id) {
    const response = await api.delete(`/api/issues/${id}`);
    return response.data;
  },
};

export default issueService;
