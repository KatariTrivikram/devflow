import React from 'react';
import { Link } from 'react-router-dom';

/**
 * ProjectSummary Component
 * Project lifecycle status breakdown (Active, Completed, Archived).
 */
export const ProjectSummary = ({ projectSummary = {} }) => {
  const {
    total = 0,
    active = 0,
    completed = 0,
    archived = 0,
  } = projectSummary;

  return (
    <div className="df-dashboard-panel" data-testid="project-summary">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">Projects Overview</h3>
          <p className="df-panel-subtitle">Lifecycle state across accessible projects</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span className="df-panel-badge">{total} Projects</span>
          <Link to="/projects" className="df-panel-link">
            Directory &rarr;
          </Link>
        </div>
      </div>

      <div className="df-project-summary-grid">
        <div className="df-project-stat-card df-project-stat--active" data-testid="project-stat-active">
          <span className="df-project-stat-label">Active</span>
          <span className="df-project-stat-num">{active}</span>
          <span className="df-project-stat-sub">In development</span>
        </div>
        <div className="df-project-stat-card df-project-stat--completed" data-testid="project-stat-completed">
          <span className="df-project-stat-label">Completed</span>
          <span className="df-project-stat-num">{completed}</span>
          <span className="df-project-stat-sub">Delivered</span>
        </div>
        <div className="df-project-stat-card df-project-stat--archived" data-testid="project-stat-archived">
          <span className="df-project-stat-label">Archived</span>
          <span className="df-project-stat-num">{archived}</span>
          <span className="df-project-stat-sub">Read-only</span>
        </div>
      </div>
    </div>
  );
};

export default ProjectSummary;
