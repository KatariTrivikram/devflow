import React from 'react';
import { Link } from 'react-router-dom';

/**
 * AssignedIssuesSummary Component
 * Shows personal assigned workload across workflow states for the authenticated user.
 */
export const AssignedIssuesSummary = ({ assignedSummary = {} }) => {
  const {
    total = 0,
    todo = 0,
    inProgress = 0,
    codeReview = 0,
    done = 0,
  } = assignedSummary;

  return (
    <div className="df-dashboard-panel df-dashboard-panel--assigned" data-testid="assigned-issues-summary">
      <div className="df-panel-header">
        <div>
          <h3 className="df-panel-title">My Assigned Issues</h3>
          <p className="df-panel-subtitle">Tasks and defects directly assigned to you</p>
        </div>
        <Link to="/issues" className="df-btn df-btn-secondary df-btn-sm">
          Open Tracker
        </Link>
      </div>

      <div className="df-assigned-grid">
        <div className="df-assigned-total-box" data-testid="assigned-total-box">
          <span className="df-assigned-total-label">Total Assigned</span>
          <span className="df-assigned-total-num">{total}</span>
        </div>

        <div className="df-assigned-stages-grid">
          <div className="df-assigned-stage-pill df-stage--todo" data-testid="assigned-todo">
            <span className="df-stage-label">To Do</span>
            <span className="df-stage-num">{todo}</span>
          </div>
          <div className="df-assigned-stage-pill df-stage--inprogress" data-testid="assigned-inprogress">
            <span className="df-stage-label">In Progress</span>
            <span className="df-stage-num">{inProgress}</span>
          </div>
          <div className="df-assigned-stage-pill df-stage--review" data-testid="assigned-review">
            <span className="df-stage-label">Code Review</span>
            <span className="df-stage-num">{codeReview}</span>
          </div>
          <div className="df-assigned-stage-pill df-stage--done" data-testid="assigned-done">
            <span className="df-stage-label">Done</span>
            <span className="df-stage-num">{done}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssignedIssuesSummary;
