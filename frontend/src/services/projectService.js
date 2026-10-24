import api from './api';

/**
 * Projects API Service aligning directly with Spring Boot ProjectController endpoints.
 */
export const projectService = {
  /**
   * Retrieve paginated projects with optional filters and sorting.
   * Endpoint: GET /api/projects
   *
   * @param {Object} [params]
   * @param {string} [params.status] - Optional ProjectStatus filter (ACTIVE, ARCHIVED, COMPLETED)
   * @param {string} [params.search] - Case-insensitive query matching name/description
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.sort='createdAt,desc'] - Sort field and direction
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getProjects(params = {}) {
    const { status, search, page = 0, size = 20, sort = 'createdAt,desc' } = params;
    const queryParams = {
      page,
      size,
      sort,
    };

    if (status && status.trim() !== '') {
      queryParams.status = status;
    }

    if (search && search.trim() !== '') {
      queryParams.search = search.trim();
    }

    const response = await api.get('/api/projects', { params: queryParams });
    return response.data;
  },

  /**
   * Retrieve a single project by UUID.
   * Endpoint: GET /api/projects/:id
   *
   * @param {string} id - Project UUID
   * @returns {Promise<Object>} ProjectResponse
   */
  async getProjectById(id) {
    const response = await api.get(`/api/projects/${id}`);
    return response.data;
  },

  /**
   * Retrieve compact project summary metrics.
   * Endpoint: GET /api/projects/:id/summary
   *
   * @param {string} id - Project UUID
   * @returns {Promise<Object>} ProjectSummaryResponse
   */
  async getProjectSummary(id) {
    const response = await api.get(`/api/projects/${id}/summary`);
    return response.data;
  },

  /**
   * Create a new project.
   * Endpoint: POST /api/projects
   *
   * @param {Object} payload - { name, description, status }
   * @returns {Promise<Object>} Created ProjectResponse
   */
  async createProject({ name, description, status = 'ACTIVE' }) {
    const payload = {
      name: name?.trim(),
      description: description?.trim() || null,
      status,
    };
    const response = await api.post('/api/projects', payload);
    return response.data;
  },

  /**
   * Update an existing project.
   * Endpoint: PUT /api/projects/:id
   *
   * @param {string} id - Project UUID
   * @param {Object} payload - { name, description, status }
   * @returns {Promise<Object>} Updated ProjectResponse
   */
  async updateProject(id, { name, description, status }) {
    const payload = {
      name: name?.trim(),
      description: description?.trim() || null,
      status,
    };
    const response = await api.put(`/api/projects/${id}`, payload);
    return response.data;
  },

  /**
   * Delete a project.
   * Endpoint: DELETE /api/projects/:id
   *
   * @param {string} id - Project UUID
   * @returns {Promise<void>}
   */
  async deleteProject(id) {
    const response = await api.delete(`/api/projects/${id}`);
    return response.data;
  },

  /**
   * Retrieve sprints belonging to a project.
   * Endpoint: GET /api/projects/:projectId/sprints
   *
   * @param {string} projectId - Project UUID
   * @param {Object} [params]
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number }>}
   */
  async getProjectSprints(projectId, params = {}) {
    const { status, page = 0, size = 20, sort = 'startDate,asc' } = params;
    const queryParams = { page, size, sort };
    if (status) queryParams.status = status;

    const response = await api.get(`/api/projects/${projectId}/sprints`, { params: queryParams });
    return response.data;
  },
};

export default projectService;
