import React from 'react';
import { formatDate } from '../../utils/date';
import SprintStatusBadge from './SprintStatusBadge';

/**
 * Prominent active sprint card displaying cycle details, dates, status, real issue progress,
 * and quick filtering action.
 */
export const ActiveSprintCard = ({
  activeSprint = null,
  canManageSprints = false,
  onCreateSprint = null,
  onFilterSprint = null,
  isFiltered = false,
}) => {
  if (!activeSprint) {
    return (
      <div className="df-active-sprint-card df-active-sprint-card--empty" data-testid="no-active-sprint-card">
        <div className="df-active-sprint-empty-content">
          <div className="df-active-sprint-badge-wrap">
            <span className="df-sprint-banner-tag">Sprint Cycle</span>
          </div>
          <h3 className="df-active-sprint-title">No active sprint</h3>
          <p className="df-active-sprint-desc">
            There is currently no active sprint in this project. Create or start a sprint to organize upcoming work and track iteration progress.
          </p>
        </div>

        {canManageSprints && onCreateSprint && (
          <div className="df-active-sprint-actions">
            <button
              type="button"
              className="df-btn df-btn-primary df-btn-sm"
              onClick={onCreateSprint}
              data-testid="create-sprint-btn-prompt"
            >
              + Create Sprint
            </button>
          </div>
        )}
      </div>
    );
  }

  const {
    id,
    name,
    status = 'ACTIVE',
    startDate,
    endDate,
    totalIssues = 0,
    completedIssues = 0,
    inProgressIssues = 0,
    todoIssues = 0,
    progressPercentage = 0,
  } = activeSprint;

  const dateRangeStr = `${formatDate(startDate)} – ${formatDate(endDate)}`;

  return (
    <div className="df-active-sprint-card" data-testid="active-sprint-card">
      <div className="df-active-sprint-header">
        <div className="df-active-sprint-title-area">
          <div className="df-active-sprint-meta-top">
            <span className="df-sprint-banner-tag">Active Sprint</span>
            <SprintStatusBadge status={status} />
          </div>
          <h3 className="df-active-sprint-title">{name}</h3>
          <div className="df-active-sprint-date-range">
            <svg
              style={{ width: '14px', height: '14px', display: 'inline', marginRight: '4px', verticalAlign: '-2px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            {dateRangeStr}
          </div>
        </div>

        <div className="df-active-sprint-header-actions">
          {onFilterSprint && (
            <button
              type="button"
              onClick={() => onFilterSprint(isFiltered ? '' : id)}
              className={`df-btn ${isFiltered ? 'df-btn-primary' : 'df-btn-secondary'} df-btn-sm`}
              data-testid="filter-active-sprint-btn"
            >
              {isFiltered ? 'Viewing Sprint Issues ✓' : 'Filter Issues to this Sprint'}
            </button>
          )}
        </div>
      </div>

      <div className="df-active-sprint-body">
        <div className="df-active-sprint-stats-line">
          <span className="df-stat-pill"><strong>{totalIssues}</strong> total issues</span>
          <span className="df-stat-pill df-stat-completed"><strong>{completedIssues}</strong> completed</span>
          <span className="df-stat-pill df-stat-inprogress"><strong>{inProgressIssues}</strong> in progress</span>
          <span className="df-stat-pill df-stat-todo"><strong>{todoIssues}</strong> todo</span>
        </div>

        <div className="df-active-sprint-progress-wrap" aria-label={`Sprint progress: ${progressPercentage}%`}>
          <div className="df-active-sprint-progress-header">
            <span className="df-progress-label">Progress</span>
            <span className="df-progress-pct" data-testid="sprint-progress-pct">{progressPercentage}%</span>
          </div>
          <div className="df-progress-bar-track" role="progressbar" aria-valuenow={progressPercentage} aria-valuemin={0} aria-valuemax={100}>
            <div
              className="df-progress-bar-fill"
              style={{ width: `${Math.min(Math.max(progressPercentage, 0), 100)}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ActiveSprintCard;
