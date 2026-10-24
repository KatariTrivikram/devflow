import React from 'react';
import { ISSUE_STATUS, ISSUE_STATUS_LABELS } from '../../constants/issueEnums';

/**
 * Visual badge displaying issue status with accessible styling.
 *
 * @param {Object} props
 * @param {string} props.status - ISSUE_STATUS enum value (TODO, IN_PROGRESS, CODE_REVIEW, DONE)
 */
export const IssueStatusBadge = ({ status }) => {
  const normalizedStatus = (status || '').toUpperCase();

  let variantClass = 'df-status-todo';
  let label = ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO];

  switch (normalizedStatus) {
    case ISSUE_STATUS.IN_PROGRESS:
      variantClass = 'df-status-in-progress';
      label = ISSUE_STATUS_LABELS[ISSUE_STATUS.IN_PROGRESS];
      break;
    case ISSUE_STATUS.CODE_REVIEW:
      variantClass = 'df-status-code-review';
      label = ISSUE_STATUS_LABELS[ISSUE_STATUS.CODE_REVIEW];
      break;
    case ISSUE_STATUS.DONE:
      variantClass = 'df-status-done';
      label = ISSUE_STATUS_LABELS[ISSUE_STATUS.DONE];
      break;
    case ISSUE_STATUS.TODO:
    default:
      variantClass = 'df-status-todo';
      label = ISSUE_STATUS_LABELS[ISSUE_STATUS.TODO];
      break;
  }

  return (
    <span
      className={`df-status-pill ${variantClass}`}
      role="status"
      aria-label={`Status: ${label}`}
    >
      <span className="df-status-dot" aria-hidden="true" />
      {label}
    </span>
  );
};

export default IssueStatusBadge;
