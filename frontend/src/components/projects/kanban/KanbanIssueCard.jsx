import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import IssueTypeBadge from '../IssueTypeBadge';
import IssuePriorityBadge from '../IssuePriorityBadge';
import {
  ISSUE_STATUS,
  ISSUE_STATUS_LABELS,
} from '../../../constants/issueEnums';

/**
 * KanbanIssueCard Component
 * Displays an individual issue card with metadata, drag-and-drop capability,
 * and an accessible fallback status selector for keyboard users.
 */
const KanbanIssueCard = ({
  issue,
  canEdit = false,
  onStatusChange,
  onEdit,
  sprints = [],
  members = [],
  isUpdating = false,
}) => {
  const [isDragging, setIsDragging] = useState(false);

  // Lookup sprint name
  const sprint = sprints.find((s) => s.id === issue.sprintId);
  const sprintName = sprint ? sprint.name : issue.sprintId ? `Sprint #${issue.sprintId.substring(0, 8)}` : null;

  // Lookup assignee name
  const memberRecord = members.find(
    (m) => m.userId === issue.assignedToId || m.user?.id === issue.assignedToId
  );
  const assigneeName = memberRecord
    ? memberRecord.user?.name || memberRecord.user?.email || `User ${issue.assignedToId.substring(0, 8)}`
    : issue.assignedToId
    ? `User #${issue.assignedToId.substring(0, 8)}`
    : null;

  const handleDragStart = (e) => {
    if (!canEdit || isUpdating) {
      e.preventDefault();
      return;
    }
    setIsDragging(true);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData(
      'text/plain',
      JSON.stringify({ issueId: issue.id, currentStatus: issue.status })
    );
  };

  const handleDragEnd = () => {
    setIsDragging(false);
  };

  const handleSelectStatusChange = (e) => {
    const newStatus = e.target.value;
    if (newStatus && newStatus !== issue.status && onStatusChange) {
      onStatusChange(issue, newStatus);
    }
  };

  const shortId = issue.id ? `#${issue.id.substring(0, 8)}` : '';

  return (
    <div
      className={`df-kanban-card ${isDragging ? 'df-kanban-card-dragging' : ''} ${
        isUpdating ? 'df-kanban-card-updating' : ''
      } ${!canEdit ? 'df-kanban-card-disabled' : ''}`}
      draggable={canEdit && !isUpdating}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      data-testid={`kanban-card-${issue.id}`}
      aria-label={`Issue ${issue.title}`}
    >
      {/* Card Header: Short ID & Badges */}
      <div className="df-kanban-card-header">
        <span className="df-kanban-card-id" title={issue.id}>
          {shortId}
        </span>
        <div className="df-kanban-card-badges">
          <IssueTypeBadge type={issue.type} />
          <IssuePriorityBadge priority={issue.priority} />
        </div>
      </div>

      {/* Card Title */}
      <h3 className="df-kanban-card-title">
        <Link
          to={`/issues/${issue.id}`}
          style={{ textDecoration: 'none', color: 'inherit' }}
          onClick={(e) => e.stopPropagation()}
        >
          {issue.title}
        </Link>
      </h3>

      {/* Description Preview (if present) */}
      {issue.description && (
        <p className="df-kanban-card-desc">{issue.description}</p>
      )}

      {/* Optional Metadata: Sprint */}
      {sprintName && (
        <div className="df-kanban-card-meta">
          <span className="df-kanban-card-tag" title={`Sprint: ${sprintName}`}>
            <svg
              style={{ width: '12px', height: '12px' }}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
            {sprintName}
          </span>
        </div>
      )}

      {/* Card Footer: Assignee & Controls */}
      <div className="df-kanban-card-footer">
        {/* Assignee */}
        <div className="df-kanban-card-assignee" title={assigneeName || 'Unassigned'}>
          <svg
            style={{ width: '13px', height: '13px', flexShrink: 0 }}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
            />
          </svg>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {assigneeName || 'Unassigned'}
          </span>
        </div>

        {/* Actions: Accessible Status Selector & Edit */}
        <div className="df-kanban-card-actions">
          {/* Accessible Status Move Selector */}
          <select
            className="df-select-xs"
            value={issue.status}
            onChange={handleSelectStatusChange}
            disabled={!canEdit || isUpdating}
            aria-label={`Move issue ${issue.title} to status`}
            title={canEdit ? 'Move issue to another status' : 'You do not have permission to move this issue'}
          >
            <option value={ISSUE_STATUS.TODO}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO]}</option>
            <option value={ISSUE_STATUS.IN_PROGRESS}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.IN_PROGRESS]}</option>
            <option value={ISSUE_STATUS.CODE_REVIEW}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.CODE_REVIEW]}</option>
            <option value={ISSUE_STATUS.DONE}>{ISSUE_STATUS_LABELS[ISSUE_STATUS.DONE]}</option>
          </select>

          {/* Edit Button */}
          {canEdit && onEdit && (
            <button
              type="button"
              className="df-btn df-btn-secondary df-btn-sm"
              onClick={() => onEdit(issue)}
              disabled={isUpdating}
              aria-label={`Edit issue ${issue.title}`}
              style={{ padding: '0.1875rem 0.5rem', fontSize: '0.6875rem', minHeight: '24px' }}
            >
              Edit
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default KanbanIssueCard;
