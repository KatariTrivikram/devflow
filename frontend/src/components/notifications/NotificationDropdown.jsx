import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import NotificationItem from './NotificationItem';
import NotificationEmptyState from './NotificationEmptyState';

export const NotificationDropdown = ({
  isOpen,
  onClose,
  notifications = [],
  unreadCount = 0,
  isLoading = false,
  onMarkRead,
  onMarkAllRead,
}) => {
  const dropdownRef = useRef(null);
  const navigate = useNavigate();

  // Close when clicking outside of dropdown
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        onClose();
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  const handleViewAll = () => {
    onClose();
    navigate('/notifications');
  };

  const handleItemClick = (notification) => {
    onClose();
    if (notification.referenceType === 'ISSUE' && notification.referenceId) {
      navigate(`/issues/${notification.referenceId}`);
    } else if (notification.referenceType === 'PROJECT' && notification.referenceId) {
      navigate(`/projects/${notification.referenceId}`);
    } else if (notification.referenceType === 'SPRINT') {
      if (notification.projectId) {
        navigate(`/projects/${notification.projectId}`);
      } else {
        navigate('/projects');
      }
    }
  };

  return (
    <div
      className="df-notification-dropdown"
      ref={dropdownRef}
      role="region"
      aria-label="Recent notifications"
      data-testid="notification-dropdown"
    >
      <div className="df-notification-dropdown-header">
        <div className="df-notification-dropdown-title-row">
          <h3 className="df-notification-dropdown-title">Notifications</h3>
          {unreadCount > 0 && (
            <span className="df-notification-badge-pill" data-testid="dropdown-unread-pill">
              {unreadCount} new
            </span>
          )}
        </div>

        {unreadCount > 0 && onMarkAllRead && (
          <button
            type="button"
            onClick={onMarkAllRead}
            className="df-notification-mark-all-btn"
            data-testid="dropdown-mark-all-read"
          >
            Mark all read
          </button>
        )}
      </div>

      <div className="df-notification-dropdown-body">
        {isLoading ? (
          <div className="df-notification-dropdown-loading">
            <span className="df-spinner df-spinner-sm" />
            <span>Loading alerts...</span>
          </div>
        ) : notifications.length === 0 ? (
          <NotificationEmptyState
            compact
            title="No notifications"
            message="You have no notifications right now."
          />
        ) : (
          <div className="df-notification-dropdown-list">
            {notifications.slice(0, 7).map((item) => (
              <NotificationItem
                key={item.id}
                notification={item}
                compact
                onMarkRead={onMarkRead}
                onItemClick={handleItemClick}
              />
            ))}
          </div>
        )}
      </div>

      <div className="df-notification-dropdown-footer">
        <button
          type="button"
          onClick={handleViewAll}
          className="df-notification-dropdown-view-all"
        >
          View all notifications
        </button>
      </div>
    </div>
  );
};

export default NotificationDropdown;
