import React from 'react';

export const NotificationEmptyState = ({
  title = 'No notifications yet',
  message = "You're all caught up! New alerts about issue assignments, status updates, and comments will show up here.",
  compact = false,
}) => {
  return (
    <div className={`df-notification-empty ${compact ? 'df-notification-empty-compact' : ''}`}>
      <div className="df-notification-empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="df-icon-lg">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
          />
        </svg>
      </div>
      <h4 className="df-notification-empty-title">{title}</h4>
      <p className="df-notification-empty-message">{message}</p>
    </div>
  );
};

export default NotificationEmptyState;
