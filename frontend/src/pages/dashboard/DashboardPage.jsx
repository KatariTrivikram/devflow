import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLES, USER_ROLE_LABELS } from '../../constants/roles';
import dashboardService from '../../services/dashboardService';
import {
  SummaryCard,
  DashboardSkeleton,
  IssueStatusSummary,
  IssuePrioritySummary,
  ProjectSummary,
  SprintSummary,
  AssignedIssuesSummary,
  RecentActivity,
} from '../../components/dashboard';
import ErrorState from '../../components/common/ErrorState';
import EmptyState from '../../components/common/EmptyState';

export const DashboardPage = () => {
  const { user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showProfileDetails, setShowProfileDetails] = useState(false);

  const role = user?.role || summary?.userRole || USER_ROLES.DEVELOPER;
  const roleLabel = user?.role ? (USER_ROLE_LABELS[user.role] || user.role) : 'User';

  const [refreshKey, setRefreshKey] = useState(0);

  const handleRetry = () => {
    setIsLoading(true);
    setError(null);
    setRefreshKey((k) => k + 1);
  };

  useEffect(() => {
    let ignore = false;

    async function loadDashboard() {
      try {
        const data = await dashboardService.getDashboardSummary();
        if (!ignore) {
          setSummary(data);
          setError(null);
        }
      } catch (err) {
        if (!ignore) {
          setError(err?.message || 'Unable to retrieve dashboard metrics from the server.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      ignore = true;
    };
  }, [refreshKey]);

  const projectSummary = summary?.projectSummary || { total: 0, active: 0, completed: 0, archived: 0 };
  const issueSummary = summary?.issueSummary || {
    total: 0,
    todo: 0,
    inProgress: 0,
    codeReview: 0,
    done: 0,
    lowPriority: 0,
    mediumPriority: 0,
    highPriority: 0,
    criticalPriority: 0,
  };
  const assignedSummary = summary?.assignedIssueSummary || {
    total: 0,
    todo: 0,
    inProgress: 0,
    codeReview: 0,
    done: 0,
  };
  const sprintSummary = summary?.sprintSummary || { total: 0, planned: 0, active: 0, completed: 0 };
  const recentActivity = summary?.recentActivity || [];

  const hasNoProjects = !isLoading && !error && projectSummary.total === 0;

  return (
    <div className="df-page df-dashboard-page" data-testid="dashboard-page">
      {/* Page Header */}
      <div className="df-page-header df-dashboard-header">
        <div>
          <h1 className="df-page-title">Dashboard</h1>
          <p className="df-page-subtitle">
            Welcome back, <strong>{user?.name || 'Developer'}</strong>. Here is your engineering workflow summary.
          </p>
        </div>
        <div className="df-dashboard-header-actions">
          <span className="df-role-tag df-role-tag-lg" data-testid="dashboard-header-role">
            {roleLabel}
          </span>
          <button
            type="button"
            className="df-btn df-btn-secondary df-btn-sm"
            onClick={() => setShowProfileDetails((prev) => !prev)}
            aria-expanded={showProfileDetails}
          >
            {showProfileDetails ? 'Hide Profile' : 'Profile Details'}
          </button>
        </div>
      </div>

      {/* Authenticated Profile Info (collapsible or preserved for accessibility and tests) */}
      <div
        className={`df-profile-card ${showProfileDetails ? '' : 'df-profile-card--compact'}`}
        data-testid="dashboard-profile-card"
      >
        <div className="df-profile-row">
          <span className="df-profile-key">Full Name</span>
          <span className="df-profile-val" data-testid="dashboard-user-name">
            {user?.name || 'Not provided'}
          </span>
        </div>

        <div className="df-profile-row">
          <span className="df-profile-key">Email Address</span>
          <span className="df-profile-val" data-testid="dashboard-user-email">
            {user?.email || 'Not provided'}
          </span>
        </div>

        <div className="df-profile-row">
          <span className="df-profile-key">Platform Role</span>
          <span className="df-profile-val" data-testid="dashboard-user-role">
            {roleLabel}
          </span>
        </div>

        {user?.id && (
          <div className="df-profile-row">
            <span className="df-profile-key">User Identifier (UUID)</span>
            <span
              className="df-profile-val"
              data-testid="dashboard-user-id"
              style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}
            >
              {user.id}
            </span>
          </div>
        )}

        <div className="df-profile-row">
          <span className="df-profile-key">Session Status</span>
          <span className="df-profile-val">
            <span className="df-status-pill">
              <span className="df-status-dot" />
              Authenticated (Active JWT)
            </span>
          </span>
        </div>
      </div>

      {/* Loading Skeleton */}
      {isLoading && <DashboardSkeleton />}

      {/* Error State with Retry */}
      {!isLoading && error && (
        <ErrorState
          title="Failed to Load Dashboard"
          message={error}
          onRetry={handleRetry}
          retryLabel="Retry Metrics"
        />
      )}

      {/* Main Dashboard Content */}
      {!isLoading && !error && (
        <>
          {/* Empty state when user has 0 projects */}
          {hasNoProjects && (
            <div className="df-dashboard-empty-banner">
              <EmptyState
                title="No Accessible Projects"
                description={
                  role === USER_ROLES.DEVELOPER
                    ? "You are not currently assigned to any projects. Contact your Project Manager or Administrator to be added to a workspace."
                    : "No projects found in your scope. Create your first project to start tracking sprints, issues, and delivery analytics."
                }
                actionLabel={role !== USER_ROLES.DEVELOPER ? "Go to Projects" : null}
                onAction={role !== USER_ROLES.DEVELOPER ? () => { window.location.href = '/projects'; } : null}
              />
            </div>
          )}

          {/* KPI Summary Cards Grid - Scoped & Tailored by Role */}
          <div className="df-kpi-grid" data-testid="dashboard-kpi-grid">
            {role === USER_ROLES.DEVELOPER ? (
              <>
                <SummaryCard
                  title="My Assigned Issues"
                  count={assignedSummary.total}
                  subtitle={`${assignedSummary.inProgress} in progress`}
                  link="/issues"
                  linkText="View My Tasks"
                  variant="primary"
                  testId="kpi-assigned-total"
                />
                <SummaryCard
                  title="Assigned In-Progress"
                  count={assignedSummary.inProgress}
                  subtitle="Active work in flight"
                  link="/issues"
                  linkText="View"
                  variant="warning"
                  testId="kpi-assigned-inprogress"
                />
                <SummaryCard
                  title="Projects Scope"
                  count={projectSummary.total}
                  subtitle={`${projectSummary.active} active`}
                  link="/projects"
                  linkText="Directory"
                  variant="default"
                  testId="kpi-projects-total"
                />
                <SummaryCard
                  title="Total Project Issues"
                  count={issueSummary.total}
                  subtitle={`${issueSummary.done} completed`}
                  link="/issues"
                  linkText="Board"
                  variant="neutral"
                  testId="kpi-issues-total"
                />
              </>
            ) : role === USER_ROLES.PROJECT_MANAGER ? (
              <>
                <SummaryCard
                  title="Managed Projects"
                  count={projectSummary.total}
                  subtitle={`${projectSummary.active} active development`}
                  link="/projects"
                  linkText="Projects"
                  variant="primary"
                  testId="kpi-projects-total"
                />
                <SummaryCard
                  title="Active Sprints"
                  count={sprintSummary.active}
                  subtitle={`${sprintSummary.planned} planned iterations`}
                  link="/projects"
                  linkText="Sprints"
                  variant="warning"
                  testId="kpi-sprints-active"
                />
                <SummaryCard
                  title="Total Issues"
                  count={issueSummary.total}
                  subtitle={`${issueSummary.inProgress} in progress, ${issueSummary.done} done`}
                  link="/issues"
                  linkText="Issue Tracker"
                  variant="default"
                  testId="kpi-issues-total"
                />
                <SummaryCard
                  title="My Assigned"
                  count={assignedSummary.total}
                  subtitle={`${assignedSummary.inProgress} currently working`}
                  link="/issues"
                  linkText="My Issues"
                  variant="neutral"
                  testId="kpi-assigned-total"
                />
              </>
            ) : (
              <>
                <SummaryCard
                  title="Total Projects"
                  count={projectSummary.total}
                  subtitle={`${projectSummary.active} active, ${projectSummary.completed} completed`}
                  link="/projects"
                  linkText="All Projects"
                  variant="primary"
                  testId="kpi-projects-total"
                />
                <SummaryCard
                  title="Platform Issues"
                  count={issueSummary.total}
                  subtitle={`${issueSummary.criticalPriority} critical priority`}
                  link="/issues"
                  linkText="All Issues"
                  variant="default"
                  testId="kpi-issues-total"
                />
                <SummaryCard
                  title="Active Sprints"
                  count={sprintSummary.active}
                  subtitle={`${sprintSummary.total} total iterations`}
                  link="/projects"
                  linkText="Sprints"
                  variant="warning"
                  testId="kpi-sprints-active"
                />
                <SummaryCard
                  title="Assigned to Me"
                  count={assignedSummary.total}
                  subtitle={`${assignedSummary.inProgress} in progress`}
                  link="/issues"
                  linkText="My Issues"
                  variant="neutral"
                  testId="kpi-assigned-total"
                />
              </>
            )}
          </div>

          {/* Developer Spotlight Section */}
          {role === USER_ROLES.DEVELOPER && (
            <div className="df-dashboard-row">
              <AssignedIssuesSummary assignedSummary={assignedSummary} />
            </div>
          )}

          {/* Workflow Analytics Grid */}
          <div className="df-dashboard-analytics-grid">
            <IssueStatusSummary issueSummary={issueSummary} />
            <IssuePrioritySummary issueSummary={issueSummary} />
          </div>

          {/* Delivery & Lifecycle Grid */}
          <div className="df-dashboard-delivery-grid">
            <ProjectSummary projectSummary={projectSummary} />
            <SprintSummary sprintSummary={sprintSummary} />
          </div>

          {/* Role Manager/Admin Assigned Section */}
          {role !== USER_ROLES.DEVELOPER && assignedSummary.total > 0 && (
            <div className="df-dashboard-row">
              <AssignedIssuesSummary assignedSummary={assignedSummary} />
            </div>
          )}

          {/* Recent Activity Audit Trail */}
          <div className="df-dashboard-row">
            <RecentActivity activities={recentActivity} />
          </div>

          {/* Quick Workspace Navigation */}
          <div className="df-section-card df-dashboard-nav-section">
            <div className="df-section-header">
              <h3 className="df-section-title">Workspace Modules</h3>
              <p className="df-section-subtitle">Direct access to core engineering features</p>
            </div>
            <div className="df-action-grid">
              <Link to="/projects" className="df-action-card">
                <div className="df-action-title">Projects Directory</div>
                <div className="df-action-description">
                  Browse projects, view sprint milestones, and manage team members.
                </div>
              </Link>
              <Link to="/issues" className="df-action-card">
                <div className="df-action-title">Issue Tracker</div>
                <div className="df-action-description">
                  Track bugs, tasks, and feature improvements with multi-criteria filtering.
                </div>
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default DashboardPage;
