import api from './api';

/**
 * Authentication API Service aligning directly with Spring Boot AuthController endpoints.
 */
export const authService = {
  /**
   * Authenticate an existing user.
   * Endpoint: POST /api/auth/login
   * @param {Object} credentials - { email, password }
   * @returns {Promise<{ accessToken: string, tokenType: string, expiresIn: number, user: Object }>}
   */
  async login({ email, password }) {
    const response = await api.post('/api/auth/login', { email, password });
    return response.data;
  },

  /**
   * Register a new user account.
   * Endpoint: POST /api/auth/register
   * @param {Object} userData - { name, email, password, role }
   * @returns {Promise<{ accessToken: string, tokenType: string, expiresIn: number, user: Object }>}
   */
  async register({ name, email, password, role }) {
    const response = await api.post('/api/auth/register', { name, email, password, role });
    return response.data;
  },
};

export default authService;
