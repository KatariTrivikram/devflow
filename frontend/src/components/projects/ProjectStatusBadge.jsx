import React from 'react';
import { PROJECT_STATUS } from '../../constants/projectEnums';

/**
 * Visual badge displaying project lifecycle status with accessible styling.
 *
 * @param {Object} props
 * @param {string} props.status - PROJECT_STATUS enum value (ACTIVE, ARCHIVED, COMPLETED)
 */
export const ProjectStatusBadge = ({ status }) => {
  const normalizedStatus = (status || '').toUpperCase();

  let variantClass = 'df-status-active';
  let label = 'Active';

  switch (normalizedStatus) {
    case PROJECT_STATUS.ARCHIVED:
      variantClass = 'df-status-archived';
      label = 'Archived';
      break;
    case PROJECT_STATUS.COMPLETED:
      variantClass = 'df-status-completed';
      label = 'Completed';
      break;
    case PROJECT_STATUS.ACTIVE:
    default:
      variantClass = 'df-status-active';
      label = 'Active';
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

export default ProjectStatusBadge;
