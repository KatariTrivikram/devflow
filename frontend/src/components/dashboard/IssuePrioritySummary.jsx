import React from 'react';
import { Link } from 'react-router-dom';

/**
 * IssuePrioritySummary Component
 * Priority distribution metrics for Low, Medium, High, and Critical issues.
 */
export const IssuePrioritySummary = ({ issueSummary = {} }) => {
  const {
    total = 0,
    lowPriority = 0,
    mediumPriority = 0,
    highPriority = 0,
    criticalPriority = 0,
  } = issueSummary;

  const pct = (val) => (total > 0 ? Math.round((val / total) * 100) : 0);

  const priorities = [
    { label: 'Critical', count: criticalPriority, pct: pct(criticalPriority), color: '#ef4444', key: 'CRITICAL' },
    { label: 'High', count: highPriority, pct: pct(highPriority), color: '#f97316', key: 'HIGH' },
    { label: 'Medium', count: mediumPriority, pct: pct(mediumPriority), color: '#f59e0b', key: 'MEDIUM' },
    { label: 'Low', count: lowPriority, pct: pct(lowPriority), color: '#64748b', key: 'LOW' },
  ];

  return (
    <div className="df-dashboard-panel" data-testid="issue-priority-summary">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">Priority Breakdown</h3>
          <p className="df-panel-subtitle">Risk and urgency across scoped issues</p>
        </div>
        <Link to="/issues" className="df-panel-link">
          Details
        </Link>
      </div>

      <div className="df-priority-rows">
        {priorities.map((p) => (
          <div key={p.key} className="df-priority-row" data-testid={`priority-row-${p.key.toLowerCase()}`}>
            <div className="df-priority-row-info">
              <span className="df-priority-badge-label" style={{ borderLeftColor: p.color }}>
                {p.label}
              </span>
              <span className="df-priority-count-badge">
                <strong>{p.count}</strong> <span className="df-priority-pct-text">({p.pct}%)</span>
              </span>
            </div>
            <div className="df-priority-bar-track">
              <div
                className="df-priority-bar-fill"
                style={{
                  width: `${p.pct}%`,
                  backgroundColor: p.color,
                }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default IssuePrioritySummary;
