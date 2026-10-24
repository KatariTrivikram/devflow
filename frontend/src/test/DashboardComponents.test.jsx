import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import {
  SummaryCard,
  DashboardSkeleton,
  IssueStatusSummary,
  IssuePrioritySummary,
  ProjectSummary,
  SprintSummary,
  AssignedIssuesSummary,
  RecentActivity,
} from '../components/dashboard';

describe('Dashboard Component Unit Tests', () => {
  describe('SummaryCard', () => {
    it('renders count, title, and subtitle cleanly', () => {
      render(
        <SummaryCard
          title="Active Projects"
          count={5}
          subtitle="In progress"
          variant="primary"
          testId="test-card"
        />
      );

      expect(screen.getByTestId('test-card')).toBeInTheDocument();
      expect(screen.getByText('Active Projects')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('In progress')).toBeInTheDocument();
    });

    it('renders as a clickable router link when link prop is provided', () => {
      render(
        <MemoryRouter>
          <SummaryCard
            title="Total Issues"
            count={12}
            link="/issues"
            linkText="View Board"
            testId="linked-card"
          />
        </MemoryRouter>
      );

      const linkEl = screen.getByRole('link', { name: /total issues: 12/i });
      expect(linkEl).toHaveAttribute('href', '/issues');
      expect(screen.getByText(/view board/i)).toBeInTheDocument();
    });
  });

  describe('DashboardSkeleton', () => {
    it('renders with accessible aria labels and pulse loaders', () => {
      render(<DashboardSkeleton />);
      const skeleton = screen.getByLabelText(/loading dashboard metrics/i);
      expect(skeleton).toBeInTheDocument();
      expect(skeleton).toHaveAttribute('aria-busy', 'true');
    });
  });

  describe('IssueStatusSummary', () => {
    it('renders distribution bars and breakdown counts correctly', () => {
      const summary = {
        total: 10,
        todo: 4,
        inProgress: 3,
        codeReview: 1,
        done: 2,
      };

      render(
        <MemoryRouter>
          <IssueStatusSummary issueSummary={summary} />
        </MemoryRouter>
      );

      expect(screen.getByText('Issue Status Breakdown')).toBeInTheDocument();
      expect(screen.getByTestId('status-item-todo')).toHaveTextContent('4');
      expect(screen.getByTestId('status-item-todo')).toHaveTextContent('40%');
      expect(screen.getByTestId('status-item-in_progress')).toHaveTextContent('3');
      expect(screen.getByTestId('status-item-in_progress')).toHaveTextContent('30%');
      expect(screen.getByTestId('status-item-code_review')).toHaveTextContent('1');
      expect(screen.getByTestId('status-item-code_review')).toHaveTextContent('10%');
      expect(screen.getByTestId('status-item-done')).toHaveTextContent('2');
      expect(screen.getByTestId('status-item-done')).toHaveTextContent('20%');
    });

    it('handles 0 total issues without division by zero', () => {
      render(
        <MemoryRouter>
          <IssueStatusSummary issueSummary={{ total: 0 }} />
        </MemoryRouter>
      );

      expect(screen.getByTestId('status-item-todo')).toHaveTextContent('0%');
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('IssuePrioritySummary', () => {
    it('renders priority distribution percentages and counts', () => {
      const summary = {
        total: 8,
        criticalPriority: 1,
        highPriority: 3,
        mediumPriority: 2,
        lowPriority: 2,
      };

      render(
        <MemoryRouter>
          <IssuePrioritySummary issueSummary={summary} />
        </MemoryRouter>
      );

      expect(screen.getByText('Priority Breakdown')).toBeInTheDocument();
      expect(screen.getByTestId('priority-row-critical')).toHaveTextContent('1');
      expect(screen.getByTestId('priority-row-high')).toHaveTextContent('3');
      expect(screen.getByTestId('priority-row-medium')).toHaveTextContent('2');
      expect(screen.getByTestId('priority-row-low')).toHaveTextContent('2');
    });
  });

  describe('ProjectSummary', () => {
    it('renders active, completed, and archived counts with directory link', () => {
      const summary = {
        total: 7,
        active: 4,
        completed: 2,
        archived: 1,
      };

      render(
        <MemoryRouter>
          <ProjectSummary projectSummary={summary} />
        </MemoryRouter>
      );

      expect(screen.getByText('Projects Overview')).toBeInTheDocument();
      expect(screen.getByTestId('project-stat-active')).toHaveTextContent('4');
      expect(screen.getByTestId('project-stat-completed')).toHaveTextContent('2');
      expect(screen.getByTestId('project-stat-archived')).toHaveTextContent('1');
      expect(screen.getByRole('link', { name: /directory/i })).toHaveAttribute('href', '/projects');
    });
  });

  describe('SprintSummary', () => {
    it('renders active, planned, and completed sprint statistics', () => {
      const summary = {
        total: 5,
        active: 2,
        planned: 2,
        completed: 1,
      };

      render(<SprintSummary sprintSummary={summary} />);

      expect(screen.getByText('Sprint Cadence')).toBeInTheDocument();
      expect(screen.getByTestId('sprint-stat-active')).toHaveTextContent('2');
      expect(screen.getByTestId('sprint-stat-planned')).toHaveTextContent('2');
      expect(screen.getByTestId('sprint-stat-completed')).toHaveTextContent('1');
    });
  });

  describe('AssignedIssuesSummary', () => {
    it('renders personal workload breakdown across stages', () => {
      const summary = {
        total: 6,
        todo: 2,
        inProgress: 3,
        codeReview: 1,
        done: 0,
      };

      render(
        <MemoryRouter>
          <AssignedIssuesSummary assignedSummary={summary} />
        </MemoryRouter>
      );

      expect(screen.getByText('My Assigned Issues')).toBeInTheDocument();
      expect(screen.getByTestId('assigned-total-box')).toHaveTextContent('6');
      expect(screen.getByTestId('assigned-todo')).toHaveTextContent('2');
      expect(screen.getByTestId('assigned-inprogress')).toHaveTextContent('3');
      expect(screen.getByTestId('assigned-review')).toHaveTextContent('1');
      expect(screen.getByTestId('assigned-done')).toHaveTextContent('0');
      expect(screen.getByRole('link', { name: /open tracker/i })).toHaveAttribute('href', '/issues');
    });
  });

  describe('RecentActivity', () => {
    it('renders activity items with user name, action diff, and issue link', () => {
      const activities = [
        {
          id: 'act-1',
          issueId: 'issue-123',
          issueTitle: 'Implement Payment Gateway',
          userName: 'Alice Smith',
          action: 'STATUS_CHANGED',
          oldValue: 'TODO',
          newValue: 'IN_PROGRESS',
          createdAt: '2026-10-15T11:00:00Z',
        },
      ];

      render(
        <MemoryRouter>
          <RecentActivity activities={activities} />
        </MemoryRouter>
      );

      expect(screen.getByText('Recent Activity')).toBeInTheDocument();
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
      expect(screen.getByText('Implement Payment Gateway')).toBeInTheDocument();
      const issueLink = screen.getByRole('link', { name: /implement payment gateway/i });
      expect(issueLink).toHaveAttribute('href', '/issues/issue-123');
      expect(screen.getByText('To Do')).toBeInTheDocument();
      expect(screen.getByText('In Progress')).toBeInTheDocument();
    });

    it('renders friendly empty message when activity list is empty', () => {
      render(<RecentActivity activities={[]} />);
      expect(screen.getByTestId('activity-empty')).toHaveTextContent(/no recent activity recorded yet/i);
    });
  });
});
