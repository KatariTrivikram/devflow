import React from 'react';
import { ISSUE_PRIORITY, ISSUE_PRIORITY_LABELS } from '../../constants/issueEnums';

/**
 * Visual badge displaying issue priority with accessible styling.
 *
 * @param {Object} props
 * @param {string} props.priority - ISSUE_PRIORITY enum value (LOW, MEDIUM, HIGH, CRITICAL)
 */
export const IssuePriorityBadge = ({ priority }) => {
  const normalizedPriority = (priority || '').toUpperCase();

  let variantClass = 'df-priority-medium';
  let label = ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.MEDIUM];

  switch (normalizedPriority) {
    case ISSUE_PRIORITY.LOW:
      variantClass = 'df-priority-low';
      label = ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.LOW];
      break;
    case ISSUE_PRIORITY.HIGH:
      variantClass = 'df-priority-high';
      label = ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.HIGH];
      break;
    case ISSUE_PRIORITY.CRITICAL:
      variantClass = 'df-priority-critical';
      label = ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.CRITICAL];
      break;
    case ISSUE_PRIORITY.MEDIUM:
    default:
      variantClass = 'df-priority-medium';
      label = ISSUE_PRIORITY_LABELS[ISSUE_PRIORITY.MEDIUM];
      break;
  }

  return (
    <span
      className={`df-priority-pill ${variantClass}`}
      role="status"
      aria-label={`Priority: ${label}`}
    >
      {label}
    </span>
  );
};

export default IssuePriorityBadge;
