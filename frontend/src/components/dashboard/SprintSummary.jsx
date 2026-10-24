import React from 'react';

/**
 * SprintSummary Component
 * High-level summary of active, planned, and completed sprints.
 */
export const SprintSummary = ({ sprintSummary = {} }) => {
  const {
    total = 0,
    planned = 0,
    active = 0,
    completed = 0,
  } = sprintSummary;

  return (
    <div className="df-dashboard-panel" data-testid="sprint-summary">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">Sprint Cadence</h3>
          <p className="df-panel-subtitle">Iteration delivery and planning status</p>
        </div>
        <span className="df-panel-badge">{total} Sprints</span>
      </div>

      <div className="df-sprint-summary-grid">
        <div className="df-sprint-stat-item df-sprint-stat--active" data-testid="sprint-stat-active">
          <div className="df-sprint-stat-header">
            <span className="df-sprint-stat-dot" aria-hidden="true" />
            <span className="df-sprint-stat-name">Active Sprints</span>
          </div>
          <span className="df-sprint-stat-val">{active}</span>
          <span className="df-sprint-stat-note">Running iterations</span>
        </div>

        <div className="df-sprint-stat-item df-sprint-stat--planned" data-testid="sprint-stat-planned">
          <div className="df-sprint-stat-header">
            <span className="df-sprint-stat-dot" aria-hidden="true" />
            <span className="df-sprint-stat-name">Planned</span>
          </div>
          <span className="df-sprint-stat-val">{planned}</span>
          <span className="df-sprint-stat-note">Upcoming roadmap</span>
        </div>

        <div className="df-sprint-stat-item df-sprint-stat--completed" data-testid="sprint-stat-completed">
          <div className="df-sprint-stat-header">
            <span className="df-sprint-stat-dot" aria-hidden="true" />
            <span className="df-sprint-stat-name">Completed</span>
          </div>
          <span className="df-sprint-stat-val">{completed}</span>
          <span className="df-sprint-stat-note">Past milestones</span>
        </div>
      </div>
    </div>
  );
};

export default SprintSummary;
