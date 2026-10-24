import React from 'react';
import { ISSUE_TYPE, ISSUE_TYPE_LABELS } from '../../constants/issueEnums';

/**
 * Visual badge displaying issue type with accessible styling.
 *
 * @param {Object} props
 * @param {string} props.type - ISSUE_TYPE enum value (BUG, FEATURE, TASK, IMPROVEMENT)
 */
export const IssueTypeBadge = ({ type }) => {
  const normalizedType = (type || '').toUpperCase();

  let variantClass = 'df-type-task';
  let label = ISSUE_TYPE_LABELS[ISSUE_TYPE.TASK];

  switch (normalizedType) {
    case ISSUE_TYPE.BUG:
      variantClass = 'df-type-bug';
      label = ISSUE_TYPE_LABELS[ISSUE_TYPE.BUG];
      break;
    case ISSUE_TYPE.FEATURE:
      variantClass = 'df-type-feature';
      label = ISSUE_TYPE_LABELS[ISSUE_TYPE.FEATURE];
      break;
    case ISSUE_TYPE.IMPROVEMENT:
      variantClass = 'df-type-improvement';
      label = ISSUE_TYPE_LABELS[ISSUE_TYPE.IMPROVEMENT];
      break;
    case ISSUE_TYPE.TASK:
    default:
      variantClass = 'df-type-task';
      label = ISSUE_TYPE_LABELS[ISSUE_TYPE.TASK];
      break;
  }

  return (
    <span
      className={`df-type-pill ${variantClass}`}
      role="status"
      aria-label={`Type: ${label}`}
    >
      {label}
    </span>
  );
};

export default IssueTypeBadge;
