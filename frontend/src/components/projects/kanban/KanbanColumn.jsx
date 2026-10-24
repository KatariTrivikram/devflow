import React, { useState } from 'react';
import KanbanIssueCard from './KanbanIssueCard';
import { ISSUE_STATUS } from '../../../constants/issueEnums';

/**
 * KanbanColumn Component
 * Represents a single workflow column (TODO, IN_PROGRESS, CODE_REVIEW, DONE).
 * Handles drop target events and renders issue cards or a clean empty placeholder.
 */
const KanbanColumn = ({
  status,
  title,
  issues = [],
  onDropIssue,
  onStatusChange,
  onEditIssue,
  canEditIssue,
  updatingIssueId,
  sprints = [],
  members = [],
}) => {
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (!isDragOver) {
      setIsDragOver(true);
    }
  };

  const handleDragLeave = (e) => {
    // Check if we are actually leaving the column element
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setIsDragOver(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);

    try {
      const data = e.dataTransfer.getData('text/plain');
      if (!data) return;

      const { issueId, currentStatus } = JSON.parse(data);
      if (issueId && currentStatus !== status && onDropIssue) {
        onDropIssue(issueId, status);
      }
    } catch {
      // Ignore invalid drag payload
    }
  };

  // Determine indicator dot color per status
  const getDotClass = () => {
    switch (status) {
      case ISSUE_STATUS.TODO:
        return 'df-status-todo';
      case ISSUE_STATUS.IN_PROGRESS:
        return 'df-status-in-progress';
      case ISSUE_STATUS.CODE_REVIEW:
        return 'df-status-code-review';
      case ISSUE_STATUS.DONE:
        return 'df-status-done';
      default:
        return '';
    }
  };

  return (
    <section
      className={`df-kanban-column ${isDragOver ? 'df-kanban-column-dropzone' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      data-testid={`kanban-column-${status}`}
      aria-label={`${title} column`}
    >
      {/* Column Header */}
      <div className="df-kanban-column-header">
        <div className="df-kanban-column-title">
          <span className={`df-status-dot ${getDotClass()}`} aria-hidden="true" />
          <span>{title}</span>
        </div>
        <span
          className="df-kanban-column-count"
          aria-label={`${issues.length} issues in ${title}`}
        >
          {issues.length}
        </span>
      </div>

      {/* Column Body: Issue Cards or Compact Empty Placeholder */}
      <div className="df-kanban-column-body">
        {issues.length === 0 ? (
          <div className="df-kanban-empty">No issues</div>
        ) : (
          issues.map((issue) => (
            <KanbanIssueCard
              key={issue.id}
              issue={issue}
              canEdit={canEditIssue ? canEditIssue(issue) : false}
              onStatusChange={onStatusChange}
              onEdit={onEditIssue}
              sprints={sprints}
              members={members}
              isUpdating={updatingIssueId === issue.id}
            />
          ))
        )}
      </div>
    </section>
  );
};

export default KanbanColumn;
