import api from './api';

/**
 * Notifications API Service connecting to DevFlow Spring Boot NotificationController endpoints.
 */
export const notificationService = {
  /**
   * Retrieve paginated notifications for current authenticated user.
   * Endpoint: GET /api/notifications
   *
   * @param {Object} [params]
   * @param {boolean} [params.unreadOnly] - Filter only unread notifications
   * @param {number} [params.page=0] - 0-based page index
   * @param {number} [params.size=20] - Page size
   * @param {string} [params.sort='createdAt,desc'] - Sort field and direction
   * @returns {Promise<{ content: Array, page: number, size: number, totalElements: number, totalPages: number, first: boolean, last: boolean }>}
   */
  async getNotifications(params = {}) {
    const { unreadOnly, page = 0, size = 20, sort = 'createdAt,desc' } = params;
    const queryParams = {
      page,
      size,
      sort,
    };

    if (typeof unreadOnly === 'boolean') {
      queryParams.unreadOnly = unreadOnly;
    }

    const response = await api.get('/api/notifications', { params: queryParams });
    return response.data;
  },

  /**
   * Retrieve unread notifications count for current user.
   * Endpoint: GET /api/notifications/unread-count
   *
   * @returns {Promise<{ count: number }>}
   */
  async getUnreadCount() {
    const response = await api.get('/api/notifications/unread-count');
    return response.data;
  },

  /**
   * Mark a single notification as read.
   * Endpoint: PATCH /api/notifications/:id/read
   *
   * @param {string} id - Notification UUID
   * @returns {Promise<Object>} NotificationResponse
   */
  async markAsRead(id) {
    const response = await api.patch(`/api/notifications/${id}/read`);
    return response.data;
  },

  /**
   * Mark all unread notifications as read in bulk.
   * Endpoint: PATCH /api/notifications/read-all
   *
   * @returns {Promise<{ message: string, updatedCount: number }>}
   */
  async markAllAsRead() {
    const response = await api.patch('/api/notifications/read-all');
    return response.data;
  },
};

export default notificationService;
