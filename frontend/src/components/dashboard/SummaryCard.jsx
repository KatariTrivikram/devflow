import React from 'react';
import { Link } from 'react-router-dom';

/**
 * SummaryCard Component
 * Reusable KPI metric card with value, label, optional badge, icon, and direct link.
 */
export const SummaryCard = ({
  title,
  count = 0,
  subtitle,
  icon,
  badge,
  link,
  linkText,
  variant = 'default',
  testId,
}) => {
  const content = (
    <div className={`df-summary-card df-summary-card--${variant}`} data-testid={testId}>
      <div className="df-summary-card-header">
        <span className="df-summary-card-title">{title}</span>
        {icon && <span className="df-summary-card-icon" aria-hidden="true">{icon}</span>}
      </div>

      <div className="df-summary-card-body">
        <div className="df-summary-card-count">{count}</div>
        {badge && <span className="df-summary-card-badge">{badge}</span>}
      </div>

      {(subtitle || linkText) && (
        <div className="df-summary-card-footer">
          {subtitle && <span className="df-summary-card-subtitle">{subtitle}</span>}
          {link && linkText && (
            <span className="df-summary-card-link-text">
              {linkText} <span aria-hidden="true">&rarr;</span>
            </span>
          )}
        </div>
      )}
    </div>
  );

  if (link) {
    return (
      <Link to={link} className="df-summary-card-wrapper" aria-label={`${title}: ${count}`}>
        {content}
      </Link>
    );
  }

  return content;
};

export default SummaryCard;
