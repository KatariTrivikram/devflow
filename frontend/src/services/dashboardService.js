import api from './api';

/**
 * Dashboard API Service aligning with Spring Boot DashboardController.
 * Retrieves role-scoped summary statistics and recent activity timeline.
 */
export const dashboardService = {
  /**
   * Retrieve aggregated dashboard summary metrics.
   * Endpoint: GET /api/dashboard/summary
   *
   * @returns {Promise<{
   *   userRole: string,
   *   projectSummary: { total: number, active: number, completed: number, archived: number },
   *   issueSummary: { total: number, todo: number, inProgress: number, codeReview: number, done: number, lowPriority: number, mediumPriority: number, highPriority: number, criticalPriority: number },
   *   assignedIssueSummary: { total: number, todo: number, inProgress: number, codeReview: number, done: number },
   *   sprintSummary: { total: number, planned: number, active: number, completed: number },
   *   recentActivity: Array<{ id: string, issueId: string, issueTitle: string, userId: string, userName: string, userEmail: string, action: string, oldValue: string, newValue: string, createdAt: string }>
   * }>}
   */
  async getDashboardSummary() {
    const response = await api.get('/api/dashboard/summary');
    return response.data;
  },
};

export default dashboardService;
