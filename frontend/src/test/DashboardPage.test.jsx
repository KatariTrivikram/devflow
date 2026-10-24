import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '../context/authContextDef';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import dashboardService from '../services/dashboardService';
import { USER_ROLES } from '../constants/roles';

const mockDashboardData = {
  userRole: 'DEVELOPER',
  projectSummary: { total: 4, active: 3, completed: 1, archived: 0 },
  issueSummary: {
    total: 12,
    todo: 4,
    inProgress: 4,
    codeReview: 2,
    done: 2,
    lowPriority: 2,
    mediumPriority: 6,
    highPriority: 3,
    criticalPriority: 1,
  },
  assignedIssueSummary: { total: 3, todo: 1, inProgress: 2, codeReview: 0, done: 0 },
  sprintSummary: { total: 3, planned: 1, active: 1, completed: 1 },
  recentActivity: [
    {
      id: 'act-101',
      issueId: 'iss-101',
      issueTitle: 'Optimize database queries',
      userName: 'Dev Tester',
      action: 'STATUS_CHANGED',
      oldValue: 'TODO',
      newValue: 'IN_PROGRESS',
      createdAt: '2026-10-15T09:30:00Z',
    },
  ],
};

const renderWithAuth = (userRole = USER_ROLES.DEVELOPER) => {
  const mockAuthContext = {
    user: {
      id: 'test-user-id-123',
      name: 'Jane Doe',
      email: 'jane@example.com',
      role: userRole,
    },
    token: 'fake-jwt',
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
  };

  return render(
    <MemoryRouter>
      <AuthContext.Provider value={mockAuthContext}>
        <DashboardPage />
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('DashboardPage Integration', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading skeleton initially while metrics are loading', () => {
    vi.spyOn(dashboardService, 'getDashboardSummary').mockImplementation(() => new Promise(() => {}));

    renderWithAuth(USER_ROLES.DEVELOPER);

    expect(screen.getByLabelText(/loading dashboard metrics/i)).toBeInTheDocument();
  });

  it('renders Developer view with prominent assigned workload and personal KPIs', async () => {
    vi.spyOn(dashboardService, 'getDashboardSummary').mockResolvedValueOnce(mockDashboardData);

    renderWithAuth(USER_ROLES.DEVELOPER);

    await waitFor(() => {
      expect(screen.queryByLabelText(/loading dashboard metrics/i)).not.toBeInTheDocument();
    });

    // Profile elements preserved
    expect(screen.getByTestId('dashboard-user-name')).toHaveTextContent('Jane Doe');
    expect(screen.getByTestId('dashboard-user-email')).toHaveTextContent('jane@example.com');
    expect(screen.getByTestId('dashboard-user-id')).toHaveTextContent('test-user-id-123');
    expect(screen.getByText(/authenticated \(active jwt\)/i)).toBeInTheDocument();

    // Developer KPI cards
    expect(screen.getByTestId('kpi-assigned-total')).toHaveTextContent('3');
    expect(screen.getByTestId('kpi-assigned-inprogress')).toHaveTextContent('2');

    // Assigned issues summary panel
    expect(screen.getByTestId('assigned-issues-summary')).toBeInTheDocument();
    expect(screen.getByTestId('assigned-total-box')).toHaveTextContent('3');

    // Workflow breakdown
    expect(screen.getByTestId('issue-status-summary')).toBeInTheDocument();
    expect(screen.getByTestId('issue-priority-summary')).toBeInTheDocument();
    expect(screen.getByTestId('recent-activity-panel')).toBeInTheDocument();
  });

  it('renders Project Manager view emphasizing project delivery and sprints', async () => {
    const pmData = {
      ...mockDashboardData,
      userRole: USER_ROLES.PROJECT_MANAGER,
    };
    vi.spyOn(dashboardService, 'getDashboardSummary').mockResolvedValueOnce(pmData);

    renderWithAuth(USER_ROLES.PROJECT_MANAGER);

    await waitFor(() => {
      expect(screen.getByTestId('kpi-projects-total')).toBeInTheDocument();
    });

    expect(screen.getByText('Managed Projects')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-sprints-active')).toHaveTextContent('1');
    expect(screen.getByTestId('project-summary')).toBeInTheDocument();
    expect(screen.getByTestId('sprint-summary')).toBeInTheDocument();
  });

  it('renders Admin view with platform-wide metrics', async () => {
    const adminData = {
      ...mockDashboardData,
      userRole: USER_ROLES.ADMIN,
    };
    vi.spyOn(dashboardService, 'getDashboardSummary').mockResolvedValueOnce(adminData);

    renderWithAuth(USER_ROLES.ADMIN);

    await waitFor(() => {
      expect(screen.getByText('Total Projects')).toBeInTheDocument();
    });

    expect(screen.getByText('Platform Issues')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-issues-total')).toHaveTextContent('12');
  });

  it('renders clean empty onboarding state when user has 0 accessible projects', async () => {
    const emptyData = {
      userRole: USER_ROLES.DEVELOPER,
      projectSummary: { total: 0, active: 0, completed: 0, archived: 0 },
      issueSummary: { total: 0, todo: 0, inProgress: 0, codeReview: 0, done: 0, lowPriority: 0, mediumPriority: 0, highPriority: 0, criticalPriority: 0 },
      assignedIssueSummary: { total: 0, todo: 0, inProgress: 0, codeReview: 0, done: 0 },
      sprintSummary: { total: 0, planned: 0, active: 0, completed: 0 },
      recentActivity: [],
    };
    vi.spyOn(dashboardService, 'getDashboardSummary').mockResolvedValueOnce(emptyData);

    renderWithAuth(USER_ROLES.DEVELOPER);

    await waitFor(() => {
      expect(screen.getByText('No Accessible Projects')).toBeInTheDocument();
    });

    expect(screen.getByText(/you are not currently assigned to any projects/i)).toBeInTheDocument();
  });

  it('renders error state and retries successfully', async () => {
    vi.spyOn(dashboardService, 'getDashboardSummary')
      .mockRejectedValueOnce(new Error('Connection failure'))
      .mockResolvedValueOnce(mockDashboardData);

    renderWithAuth(USER_ROLES.DEVELOPER);

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Dashboard')).toBeInTheDocument();
    });
    expect(screen.getByText('Connection failure')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /retry metrics/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByTestId('dashboard-kpi-grid')).toBeInTheDocument();
    });
  });
});
