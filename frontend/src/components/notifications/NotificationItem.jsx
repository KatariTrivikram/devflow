import React from 'react';
import { useNavigate } from 'react-router-dom';
import { formatRelativeTime } from '../../utils/date';

/**
 * Returns an SVG icon corresponding to the notification type.
 */
function getNotificationIcon(type) {
  switch (type) {
    case 'ISSUE_ASSIGNED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
        </svg>
      );
    case 'ISSUE_STATUS_CHANGED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd" />
        </svg>
      );
    case 'ISSUE_PRIORITY_CHANGED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
      );
    case 'COMMENT_ADDED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clipRule="evenodd" />
        </svg>
      );
    case 'PROJECT_MEMBER_ADDED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z" />
        </svg>
      );
    case 'SPRINT_STATUS_CHANGED':
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
        </svg>
      );
    default:
      return (
        <svg className="df-notification-icon" viewBox="0 0 20 20" fill="currentColor">
          <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z" />
        </svg>
      );
  }
}

export const NotificationItem = ({
  notification,
  onMarkRead,
  onItemClick,
  compact = false,
}) => {
  const navigate = useNavigate();
  const isRead = notification.read ?? notification.isRead ?? false;

  const handleClick = () => {
    // If not read, automatically mark as read
    if (!isRead && onMarkRead) {
      onMarkRead(notification.id);
    }

    if (onItemClick) {
      onItemClick(notification);
      return;
    }

    // Default reference-based navigation
    if (notification.referenceType === 'ISSUE' && notification.referenceId) {
      navigate(`/issues/${notification.referenceId}`);
    } else if (notification.referenceType === 'PROJECT' && notification.referenceId) {
      navigate(`/projects/${notification.referenceId}`);
    } else if (notification.referenceType === 'SPRINT') {
      // Sprints belong to projects; if projectId is known or fallback to projects
      if (notification.projectId) {
        navigate(`/projects/${notification.projectId}`);
      } else {
        navigate('/projects');
      }
    }
  };

  const handleQuickMarkRead = (e) => {
    e.stopPropagation();
    if (onMarkRead) {
      onMarkRead(notification.id);
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleClick(e);
        }
      }}
      className={`df-notification-item ${!isRead ? 'df-notification-unread' : ''} ${
        compact ? 'df-notification-compact' : ''
      }`}
      data-testid={`notification-item-${notification.id}`}
    >
      <div className="df-notification-icon-wrapper">
        {getNotificationIcon(notification.type)}
        {!isRead && <span className="df-notification-unread-dot" title="Unread" />}
      </div>

      <div className="df-notification-content">
        <div className="df-notification-header">
          <span className="df-notification-title">{notification.title}</span>
          <span className="df-notification-time" title={notification.createdAt}>
            {formatRelativeTime(notification.createdAt)}
          </span>
        </div>

        <p className="df-notification-message">{notification.message}</p>

        {notification.actorName && (
          <div className="df-notification-actor">
            By <span className="df-notification-actor-name">{notification.actorName}</span>
          </div>
        )}
      </div>

      {!isRead && onMarkRead && (
        <div className="df-notification-actions">
          <button
            type="button"
            onClick={handleQuickMarkRead}
            className="df-notification-read-btn"
            title="Mark as read"
            aria-label="Mark as read"
          >
            <svg viewBox="0 0 20 20" fill="currentColor" className="df-icon-xs">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
};

export default NotificationItem;
