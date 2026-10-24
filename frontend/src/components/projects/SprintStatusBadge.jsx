import React from 'react';
import { SPRINT_STATUS } from '../../constants/projectEnums';

/**
 * Visual badge displaying sprint lifecycle status with accessible styling.
 *
 * @param {Object} props
 * @param {string} props.status - SPRINT_STATUS enum value (PLANNED, ACTIVE, COMPLETED)
 */
export const SprintStatusBadge = ({ status }) => {
  const normalizedStatus = (status || '').toUpperCase();

  let variantClass = 'df-status-planned';
  let label = 'Planned';

  switch (normalizedStatus) {
    case SPRINT_STATUS.ACTIVE:
      variantClass = 'df-status-active';
      label = 'Active';
      break;
    case SPRINT_STATUS.COMPLETED:
      variantClass = 'df-status-completed';
      label = 'Completed';
      break;
    case SPRINT_STATUS.PLANNED:
    default:
      variantClass = 'df-status-planned';
      label = 'Planned';
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

export default SprintStatusBadge;
