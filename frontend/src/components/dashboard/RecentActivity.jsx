import React from 'react';
import { Link } from 'react-router-dom';
import { formatDateTime } from '../../utils/date';
import { ISSUE_STATUS_LABELS, ISSUE_PRIORITY_LABELS } from '../../constants/issueEnums';

/**
 * Helper to render human-friendly action text and diffs.
 */
function renderActionDetails(activity) {
  const { action, oldValue, newValue } = activity;

  const formatVal = (val, isStatus) => {
    if (!val) return 'None';
    if (isStatus) return ISSUE_STATUS_LABELS[val] || val;
    return ISSUE_PRIORITY_LABELS[val] || val;
  };

  switch (action) {
    case 'ISSUE_CREATED':
      return <span className="df-activity-action-text">created issue</span>;

    case 'STATUS_CHANGED':
      return (
        <span className="df-activity-action-text">
          changed status {oldValue && <span className="df-diff-tag">{formatVal(oldValue, true)}</span>}
          {oldValue && <span className="df-diff-arrow">&rarr;</span>}
          <span className="df-diff-tag df-diff-tag--new">{formatVal(newValue, true)}</span>
        </span>
      );

    case 'PRIORITY_CHANGED':
      return (
        <span className="df-activity-action-text">
          changed priority {oldValue && <span className="df-diff-tag">{formatVal(oldValue, false)}</span>}
          {oldValue && <span className="df-diff-arrow">&rarr;</span>}
          <span className="df-diff-tag df-diff-tag--new">{formatVal(newValue, false)}</span>
        </span>
      );

    case 'ASSIGNED':
      return (
        <span className="df-activity-action-text">
          reassigned issue
        </span>
      );

    case 'COMMENT_ADDED':
      return <span className="df-activity-action-text">added a comment</span>;

    case 'SPRINT_CHANGED':
      return <span className="df-activity-action-text">updated sprint assignment</span>;

    default:
      return <span className="df-activity-action-text">{action?.toLowerCase().replace(/_/g, ' ') || 'updated'}</span>;
  }
}

/**
 * RecentActivity Component
 * Displays up to 10 recent activity audit events across scoped issues.
 */
export const RecentActivity = ({ activities = [] }) => {
  return (
    <div className="df-dashboard-panel" data-testid="recent-activity-panel">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">Recent Activity</h3>
          <p className="df-panel-subtitle">Audit trail of latest project changes</p>
        </div>
        <span className="df-panel-badge">{activities.length} recent</span>
      </div>

      {activities.length === 0 ? (
        <div className="df-activity-empty" data-testid="activity-empty">
          <p>No recent activity recorded yet.</p>
        </div>
      ) : (
        <ul className="df-activity-feed" role="list">
          {activities.map((act) => {
            const actorName = act.userName || act.userEmail || 'Team member';
            const issueTitle = act.issueTitle || 'Issue';

            return (
              <li key={act.id} className="df-activity-feed-item" data-testid={`activity-item-${act.id}`}>
                <div className="df-activity-bullet" aria-hidden="true" />
                <div className="df-activity-content">
                  <div className="df-activity-main-line">
                    <strong className="df-activity-actor">{actorName}</strong>{' '}
                    {renderActionDetails(act)}{' '}
                    {act.issueId ? (
                      <Link to={`/issues/${act.issueId}`} className="df-activity-issue-link">
                        {issueTitle}
                      </Link>
                    ) : (
                      <span className="df-activity-issue-title">{issueTitle}</span>
                    )}
                  </div>
                  <div className="df-activity-meta">
                    <time dateTime={act.createdAt}>{formatDateTime(act.createdAt)}</time>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default RecentActivity;
