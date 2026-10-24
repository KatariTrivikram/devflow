import api from './api';

/**
 * Comment API Service aligning strictly with Spring Boot CommentController endpoints.
 */
export const commentService = {
  /**
   * Retrieve paginated comments for an issue.
   * Endpoint: GET /api/comments/issue/:issueId
   *
   * @param {string} issueId - Issue UUID
   * @param {Object} [params]
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.sort='createdAt,asc'] - Sort field and direction
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getCommentsByIssue(issueId, params = {}) {
    const { page = 0, size = 20, sort = 'createdAt,asc' } = params;
    const response = await api.get(`/api/comments/issue/${issueId}`, {
      params: { page, size, sort },
    });
    return response.data;
  },

  /**
   * Retrieve a single comment by ID.
   * Endpoint: GET /api/comments/:id
   *
   * @param {string} id - Comment UUID
   * @returns {Promise<Object>} CommentResponse
   */
  async getCommentById(id) {
    const response = await api.get(`/api/comments/${id}`);
    return response.data;
  },

  /**
   * Create a new comment on an issue.
   * Endpoint: POST /api/comments
   *
   * @param {Object} payload
   * @param {string} payload.issueId - Issue UUID
   * @param {string} payload.userId - Acting user UUID (satisfies @NotNull)
   * @param {string} payload.content - Comment text
   * @returns {Promise<Object>} Created CommentResponse
   */
  async createComment(payload) {
    const response = await api.post('/api/comments', payload);
    return response.data;
  },

  /**
   * Update an existing comment.
   * Endpoint: PUT /api/comments/:id
   *
   * @param {string} id - Comment UUID
   * @param {Object} payload
   * @param {string} payload.content - Updated comment text
   * @returns {Promise<Object>} Updated CommentResponse
   */
  async updateComment(id, payload) {
    const response = await api.put(`/api/comments/${id}`, payload);
    return response.data;
  },

  /**
   * Delete a comment.
   * Endpoint: DELETE /api/comments/:id
   *
   * @param {string} id - Comment UUID
   * @returns {Promise<void>}
   */
  async deleteComment(id) {
    const response = await api.delete(`/api/comments/${id}`);
    return response.data;
  },
};

export default commentService;
