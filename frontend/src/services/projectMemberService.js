import api from './api';

/**
 * Project Members API Service aligning directly with Spring Boot ProjectMemberController endpoints.
 */
export const projectMemberService = {
  /**
   * List all members assigned to a project.
   * Endpoint: GET /api/project-members/project/:projectId
   *
   * @param {string} projectId - Project UUID
   * @returns {Promise<Array<Object>>} List of ProjectMemberResponse
   */
  async getProjectMembers(projectId) {
    const response = await api.get(`/api/project-members/project/${projectId}`);
    return response.data;
  },

  /**
   * Add a user to a project team.
   * Endpoint: POST /api/project-members
   *
   * @param {Object} payload
   * @param {string} payload.projectId - Project UUID
   * @param {string} payload.userId - User UUID
   * @returns {Promise<Object>} Created ProjectMemberResponse
   */
  async addMember({ projectId, userId }) {
    const response = await api.post('/api/project-members', {
      projectId,
      userId,
    });
    return response.data;
  },

  /**
   * Get specific membership record.
   * Endpoint: GET /api/project-members/project/:projectId/user/:userId
   *
   * @param {string} projectId - Project UUID
   * @param {string} userId - User UUID
   * @returns {Promise<Object>} ProjectMemberResponse
   */
  async getMembership(projectId, userId) {
    const response = await api.get(`/api/project-members/project/${projectId}/user/${userId}`);
    return response.data;
  },

  /**
   * List all project memberships for a given user.
   * Endpoint: GET /api/project-members/user/:userId
   *
   * @param {string} userId - User UUID
   * @returns {Promise<Array<Object>>} List of ProjectMemberResponse
   */
  async getUserMemberships(userId) {
    const response = await api.get(`/api/project-members/user/${userId}`);
    return response.data;
  },

  /**
   * Remove a member from a project team.
   * Endpoint: DELETE /api/project-members/project/:projectId/user/:userId
   *
   * @param {string} projectId - Project UUID
   * @param {string} userId - User UUID
   * @returns {Promise<void>}
   */
  async removeMember(projectId, userId) {
    const response = await api.delete(`/api/project-members/project/${projectId}/user/${userId}`);
    return response.data;
  },
};

export default projectMemberService;
