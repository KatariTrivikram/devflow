import api from './api';

/**
 * User API Service aligning directly with Spring Boot UserController endpoints.
 */
export const userService = {
  /**
   * Retrieve a user by their unique UUID.
   * Endpoint: GET /api/users/:id
   *
   * @param {string} id - User UUID
   * @returns {Promise<Object>} UserResponse
   */
  async getUserById(id) {
    const response = await api.get(`/api/users/${id}`);
    return response.data;
  },

  /**
   * Look up a user by their registered email address.
   * Endpoint: GET /api/users/email?email=...
   *
   * @param {string} email - Registered user email address
   * @returns {Promise<Object>} UserResponse
   */
  async getUserByEmail(email) {
    const response = await api.get('/api/users/email', {
      params: { email: email?.trim() },
    });
    return response.data;
  },
};

export default userService;
