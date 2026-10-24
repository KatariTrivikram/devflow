import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../services/api';
import dashboardService from '../services/dashboardService';

describe('dashboardService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('getDashboardSummary calls GET /api/dashboard/summary and returns response payload', async () => {
    const mockSummary = {
      userRole: 'PROJECT_MANAGER',
      projectSummary: { total: 3, active: 2, completed: 1, archived: 0 },
      issueSummary: {
        total: 10,
        todo: 4,
        inProgress: 3,
        codeReview: 1,
        done: 2,
        lowPriority: 1,
        mediumPriority: 5,
        highPriority: 3,
        criticalPriority: 1,
      },
      assignedIssueSummary: { total: 2, todo: 1, inProgress: 1, codeReview: 0, done: 0 },
      sprintSummary: { total: 2, planned: 1, active: 1, completed: 0 },
      recentActivity: [
        {
          id: 'act-1',
          issueId: 'iss-1',
          issueTitle: 'Refactor Auth',
          userName: 'Bob Smith',
          action: 'STATUS_CHANGED',
          oldValue: 'TODO',
          newValue: 'IN_PROGRESS',
          createdAt: '2026-10-15T10:00:00Z',
        },
      ],
    };

    vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockSummary });

    const result = await dashboardService.getDashboardSummary();

    expect(api.get).toHaveBeenCalledWith('/api/dashboard/summary');
    expect(result).toEqual(mockSummary);
    expect(result.userRole).toBe('PROJECT_MANAGER');
    expect(result.projectSummary.total).toBe(3);
    expect(result.issueSummary.total).toBe(10);
    expect(result.recentActivity).toHaveLength(1);
  });

  it('propagates API errors rejected by axios interceptor', async () => {
    const apiError = new Error('Network timeout');
    vi.spyOn(api, 'get').mockRejectedValueOnce(apiError);

    await expect(dashboardService.getDashboardSummary()).rejects.toThrow('Network timeout');
  });
});
