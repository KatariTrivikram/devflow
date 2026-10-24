import React from 'react';

/**
 * Compact project overview summary cards.
 * Renders 6 real metrics: Total Issues, Open Issues, In Progress, Done, Active Sprint, Team Members.
 */
export const ProjectSummaryCards = ({
  summary = null,
  activeSprintName = null,
  onSelectStatus = null,
}) => {
  if (!summary) return null;

  const total = summary.totalIssues ?? 0;
  const open = summary.openIssues ?? 0;
  const inProgress = (summary.inProgressIssues ?? 0) + (summary.codeReviewIssues ?? 0);
  const done = summary.doneIssues ?? 0;
  const members = summary.memberCount ?? 0;
  const sprintLabel = summary.activeSprint?.name || activeSprintName || 'None';

  return (
    <div className="df-summary-grid" data-testid="project-summary-cards">
      <div
        className="df-summary-card df-summary-card--clickable"
        role="button"
        tabIndex={0}
        onClick={() => {
          if (onSelectStatus) onSelectStatus('');
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (onSelectStatus) onSelectStatus('');
          }
        }}
        data-testid="summary-card-total-issues"
      >
        <span className="df-summary-card-label">Total Issues</span>
        <span className="df-summary-card-value">{total}</span>
        <span className="df-summary-card-subtext">All project items</span>
      </div>

      <div
        className="df-summary-card df-summary-card--clickable"
        role="button"
        tabIndex={0}
        onClick={() => {
          if (onSelectStatus) onSelectStatus('TODO');
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (onSelectStatus) onSelectStatus('TODO');
          }
        }}
        data-testid="summary-card-open-issues"
      >
        <span className="df-summary-card-label">Open Issues</span>
        <span className="df-summary-card-value df-text-warning">{open}</span>
        <span className="df-summary-card-subtext">Awaiting completion</span>
      </div>

      <div
        className="df-summary-card df-summary-card--clickable"
        role="button"
        tabIndex={0}
        onClick={() => {
          if (onSelectStatus) onSelectStatus('IN_PROGRESS');
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (onSelectStatus) onSelectStatus('IN_PROGRESS');
          }
        }}
        data-testid="summary-card-in-progress"
      >
        <span className="df-summary-card-label">In Progress</span>
        <span className="df-summary-card-value df-text-primary">{inProgress}</span>
        <span className="df-summary-card-subtext">Actively being worked</span>
      </div>

      <div
        className="df-summary-card df-summary-card--clickable"
        role="button"
        tabIndex={0}
        onClick={() => {
          if (onSelectStatus) onSelectStatus('DONE');
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (onSelectStatus) onSelectStatus('DONE');
          }
        }}
        data-testid="summary-card-done"
      >
        <span className="df-summary-card-label">Done</span>
        <span className="df-summary-card-value df-text-success">{done}</span>
        <span className="df-summary-card-subtext">Closed & completed</span>
      </div>

      <div className="df-summary-card" data-testid="summary-card-active-sprint">
        <span className="df-summary-card-label">Active Sprint</span>
        <span className="df-summary-card-value df-summary-card-value--text" title={sprintLabel}>
          {sprintLabel}
        </span>
        <span className="df-summary-card-subtext">
          {summary.activeSprint ? `${summary.activeSprint.progressPercentage}% complete` : 'No active cycle'}
        </span>
      </div>

      <div className="df-summary-card" data-testid="summary-card-team-members">
        <span className="df-summary-card-label">Team Members</span>
        <span className="df-summary-card-value">{members}</span>
        <span className="df-summary-card-subtext">Active contributors</span>
      </div>
    </div>
  );
};

export default ProjectSummaryCards;
