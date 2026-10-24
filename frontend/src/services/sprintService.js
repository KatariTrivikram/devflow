import api from './api';

/**
 * Sprint API Service aligning directly with Spring Boot SprintController and ProjectController endpoints.
 */
export const sprintService = {
  /**
   * Retrieve paginated sprints belonging to a project with optional status filter and sorting.
   * Endpoint: GET /api/projects/:projectId/sprints
   *
   * @param {string} projectId - Project UUID
   * @param {Object} [params]
   * @param {string} [params.status] - Optional SprintStatus filter (PLANNED, ACTIVE, COMPLETED)
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=10] - Page size
   * @param {string} [params.sort='startDate,asc'] - Supported sort fields: startDate, endDate, name, status, createdAt, updatedAt
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getProjectSprints(projectId, params = {}) {
    const { status, page = 0, size = 10, sort = 'startDate,asc' } = params;
    const queryParams = {
      page,
      size,
      sort,
    };

    if (status && status.trim() !== '') {
      queryParams.status = status;
    }

    const response = await api.get(`/api/projects/${projectId}/sprints`, { params: queryParams });
    return response.data;
  },

  /**
   * Retrieve a single sprint by UUID.
   * Endpoint: GET /api/sprints/:id
   *
   * @param {string} id - Sprint UUID
   * @returns {Promise<Object>} SprintResponse
   */
  async getSprintById(id) {
    const response = await api.get(`/api/sprints/${id}`);
    return response.data;
  },

  /**
   * Create a new sprint in a project.
   * Endpoint: POST /api/sprints
   *
   * @param {Object} payload
   * @param {string} payload.projectId - Project UUID
   * @param {string} payload.name - Sprint name
   * @param {string} payload.startDate - ISO timestamp
   * @param {string} payload.endDate - ISO timestamp
   * @param {string} [payload.status='PLANNED'] - SprintStatus (PLANNED, ACTIVE, COMPLETED)
   * @returns {Promise<Object>} Created SprintResponse
   */
  async createSprint({ projectId, name, startDate, endDate, status = 'PLANNED' }) {
    const payload = {
      projectId,
      name: name?.trim(),
      startDate,
      endDate,
      status,
    };
    const response = await api.post('/api/sprints', payload);
    return response.data;
  },

  /**
   * Update an existing sprint.
   * Endpoint: PUT /api/sprints/:id
   *
   * @param {string} id - Sprint UUID
   * @param {Object} payload
   * @param {string} [payload.name] - Updated name
   * @param {string} [payload.startDate] - Updated start ISO timestamp
   * @param {string} [payload.endDate] - Updated end ISO timestamp
   * @param {string} [payload.status] - Updated SprintStatus
   * @returns {Promise<Object>} Updated SprintResponse
   */
  async updateSprint(id, { name, startDate, endDate, status }) {
    const payload = {
      name: name?.trim(),
      startDate,
      endDate,
      status,
    };
    const response = await api.put(`/api/sprints/${id}`, payload);
    return response.data;
  },

  /**
   * Delete a sprint.
   * Endpoint: DELETE /api/sprints/:id
   *
   * @param {string} id - Sprint UUID
   * @returns {Promise<void>}
   */
  async deleteSprint(id) {
    const response = await api.delete(`/api/sprints/${id}`);
    return response.data;
  },
};

export default sprintService;
