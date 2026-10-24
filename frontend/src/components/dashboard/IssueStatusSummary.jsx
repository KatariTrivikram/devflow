import React from 'react';
import { Link } from 'react-router-dom';

/**
 * IssueStatusSummary Component
 * Visual workflow progress bar and breakdown across To Do, In Progress, Code Review, and Done.
 */
export const IssueStatusSummary = ({ issueSummary = {} }) => {
  const {
    total = 0,
    todo = 0,
    inProgress = 0,
    codeReview = 0,
    done = 0,
  } = issueSummary;

  const pct = (val) => (total > 0 ? Math.round((val / total) * 100) : 0);

  const todoPct = pct(todo);
  const inProgressPct = pct(inProgress);
  const codeReviewPct = pct(codeReview);
  const donePct = pct(done);

  const statuses = [
    { label: 'To Do', count: todo, pct: todoPct, colorClass: 'df-bar-todo', dotClass: 'df-dot-todo', key: 'TODO' },
    { label: 'In Progress', count: inProgress, pct: inProgressPct, colorClass: 'df-bar-inprogress', dotClass: 'df-dot-inprogress', key: 'IN_PROGRESS' },
    { label: 'Code Review', count: codeReview, pct: codeReviewPct, colorClass: 'df-bar-review', dotClass: 'df-dot-review', key: 'CODE_REVIEW' },
    { label: 'Done', count: done, pct: donePct, colorClass: 'df-bar-done', dotClass: 'df-dot-done', key: 'DONE' },
  ];

  return (
    <div className="df-dashboard-panel" data-testid="issue-status-summary">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">Issue Status Breakdown</h3>
          <p className="df-panel-subtitle">Current workflow distribution across all tracked issues</p>
        </div>
        <Link to="/issues" className="df-panel-link">
          View All ({total})
        </Link>
      </div>

      {/* Segmented Distribution Bar */}
      <div
        className="df-progress-bar-container"
        role="progressbar"
        aria-label="Issue workflow distribution"
        aria-valuenow={donePct}
        aria-valuemin="0"
        aria-valuemax="100"
      >
        {total === 0 ? (
          <div className="df-progress-segment df-bar-empty" style={{ width: '100%' }} title="No issues" />
        ) : (
          <>
            {todoPct > 0 && (
              <div
                className="df-progress-segment df-bar-todo"
                style={{ width: `${todoPct}%` }}
                title={`To Do: ${todo} (${todoPct}%)`}
              />
            )}
            {inProgressPct > 0 && (
              <div
                className="df-progress-segment df-bar-inprogress"
                style={{ width: `${inProgressPct}%` }}
                title={`In Progress: ${inProgress} (${inProgressPct}%)`}
              />
            )}
            {codeReviewPct > 0 && (
              <div
                className="df-progress-segment df-bar-review"
                style={{ width: `${codeReviewPct}%` }}
                title={`Code Review: ${codeReview} (${codeReviewPct}%)`}
              />
            )}
            {donePct > 0 && (
              <div
                className="df-progress-segment df-bar-done"
                style={{ width: `${donePct}%` }}
                title={`Done: ${done} (${donePct}%)`}
              />
            )}
          </>
        )}
      </div>

      {/* Legend & Count Breakdown */}
      <div className="df-status-breakdown-grid">
        {statuses.map((s) => (
          <div key={s.key} className="df-status-breakdown-item" data-testid={`status-item-${s.key.toLowerCase()}`}>
            <div className="df-status-item-header">
              <span className={`df-status-dot-indicator ${s.dotClass}`} aria-hidden="true" />
              <span className="df-status-item-label">{s.label}</span>
            </div>
            <div className="df-status-item-values">
              <span className="df-status-item-count">{s.count}</span>
              <span className="df-status-item-pct">{s.pct}%</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default IssueStatusSummary;
