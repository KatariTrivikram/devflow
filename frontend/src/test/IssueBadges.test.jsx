import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import IssueStatusBadge from '../components/projects/IssueStatusBadge';
import IssuePriorityBadge from '../components/projects/IssuePriorityBadge';
import IssueTypeBadge from '../components/projects/IssueTypeBadge';
import { ISSUE_STATUS, ISSUE_PRIORITY, ISSUE_TYPE } from '../constants/issueEnums';

describe('Issue Badge Components', () => {
  describe('IssueStatusBadge', () => {
    it('renders To Do status correctly', () => {
      const { container } = render(<IssueStatusBadge status={ISSUE_STATUS.TODO} />);
      expect(screen.getByText('To Do')).toBeInTheDocument();
      expect(container.querySelector('.df-status-todo')).toBeInTheDocument();
    });

    it('renders In Progress status correctly', () => {
      const { container } = render(<IssueStatusBadge status={ISSUE_STATUS.IN_PROGRESS} />);
      expect(screen.getByText('In Progress')).toBeInTheDocument();
      expect(container.querySelector('.df-status-in-progress')).toBeInTheDocument();
    });

    it('renders Code Review status correctly', () => {
      const { container } = render(<IssueStatusBadge status={ISSUE_STATUS.CODE_REVIEW} />);
      expect(screen.getByText('Code Review')).toBeInTheDocument();
      expect(container.querySelector('.df-status-code-review')).toBeInTheDocument();
    });

    it('renders Done status correctly', () => {
      const { container } = render(<IssueStatusBadge status={ISSUE_STATUS.DONE} />);
      expect(screen.getByText('Done')).toBeInTheDocument();
      expect(container.querySelector('.df-status-done')).toBeInTheDocument();
    });

    it('falls back to To Do for missing or invalid status', () => {
      render(<IssueStatusBadge status="INVALID" />);
      expect(screen.getByText('To Do')).toBeInTheDocument();
    });
  });

  describe('IssuePriorityBadge', () => {
    it('renders Low priority correctly', () => {
      const { container } = render(<IssuePriorityBadge priority={ISSUE_PRIORITY.LOW} />);
      expect(screen.getByText('Low')).toBeInTheDocument();
      expect(container.querySelector('.df-priority-low')).toBeInTheDocument();
    });

    it('renders Medium priority correctly', () => {
      const { container } = render(<IssuePriorityBadge priority={ISSUE_PRIORITY.MEDIUM} />);
      expect(screen.getByText('Medium')).toBeInTheDocument();
      expect(container.querySelector('.df-priority-medium')).toBeInTheDocument();
    });

    it('renders High priority correctly', () => {
      const { container } = render(<IssuePriorityBadge priority={ISSUE_PRIORITY.HIGH} />);
      expect(screen.getByText('High')).toBeInTheDocument();
      expect(container.querySelector('.df-priority-high')).toBeInTheDocument();
    });

    it('renders Critical priority correctly', () => {
      const { container } = render(<IssuePriorityBadge priority={ISSUE_PRIORITY.CRITICAL} />);
      expect(screen.getByText('Critical')).toBeInTheDocument();
      expect(container.querySelector('.df-priority-critical')).toBeInTheDocument();
    });

    it('falls back to Medium for invalid priority', () => {
      render(<IssuePriorityBadge priority="INVALID" />);
      expect(screen.getByText('Medium')).toBeInTheDocument();
    });
  });

  describe('IssueTypeBadge', () => {
    it('renders Bug type correctly', () => {
      const { container } = render(<IssueTypeBadge type={ISSUE_TYPE.BUG} />);
      expect(screen.getByText('Bug')).toBeInTheDocument();
      expect(container.querySelector('.df-type-bug')).toBeInTheDocument();
    });

    it('renders Feature type correctly', () => {
      const { container } = render(<IssueTypeBadge type={ISSUE_TYPE.FEATURE} />);
      expect(screen.getByText('Feature')).toBeInTheDocument();
      expect(container.querySelector('.df-type-feature')).toBeInTheDocument();
    });

    it('renders Task type correctly', () => {
      const { container } = render(<IssueTypeBadge type={ISSUE_TYPE.TASK} />);
      expect(screen.getByText('Task')).toBeInTheDocument();
      expect(container.querySelector('.df-type-task')).toBeInTheDocument();
    });

    it('renders Improvement type correctly', () => {
      const { container } = render(<IssueTypeBadge type={ISSUE_TYPE.IMPROVEMENT} />);
      expect(screen.getByText('Improvement')).toBeInTheDocument();
      expect(container.querySelector('.df-type-improvement')).toBeInTheDocument();
    });

    it('falls back to Task for invalid type', () => {
      render(<IssueTypeBadge type="INVALID" />);
      expect(screen.getByText('Task')).toBeInTheDocument();
    });
  });
});
