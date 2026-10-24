import React, { useState, useEffect, useCallback } from 'react';
import notificationService from '../../services/notificationService';
import NotificationDropdown from './NotificationDropdown';

export const NotificationBell = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // Fetch unread count on mount and listen to notification update events
  const fetchUnreadCount = useCallback(async () => {
    try {
      const data = await notificationService.getUnreadCount();
      setUnreadCount(typeof data?.count === 'number' ? data.count : 0);
    } catch {
      // Graceful fallback: keep current count or 0 without breaking header
    }
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadCount() {
      try {
        const data = await notificationService.getUnreadCount();
        if (!ignore) {
          setUnreadCount(typeof data?.count === 'number' ? data.count : 0);
        }
      } catch {
        // Graceful fallback: keep current count or 0 without breaking header
      }
    }

    loadCount();

    const handleUpdate = () => {
      fetchUnreadCount();
    };

    window.addEventListener('devflow:notifications-updated', handleUpdate);
    return () => {
      ignore = true;
      window.removeEventListener('devflow:notifications-updated', handleUpdate);
    };
  }, [fetchUnreadCount]);

  // When dropdown opens, fetch recent notifications
  const handleToggle = async () => {
    const nextState = !isOpen;
    setIsOpen(nextState);

    if (nextState) {
      setIsLoading(true);
      try {
        const pageData = await notificationService.getNotifications({ page: 0, size: 7 });
        setNotifications(pageData?.content || []);
      } catch {
        // Fallback to empty notifications list
        setNotifications([]);
      } finally {
        setIsLoading(false);
      }
    }
  };

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleMarkRead = async (id) => {
    // Optimistic UI update
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));

    try {
      await notificationService.markAsRead(id);
      window.dispatchEvent(new CustomEvent('devflow:notifications-updated'));
    } catch {
      // Re-fetch to sync if failed
      fetchUnreadCount();
    }
  };

  const handleMarkAllRead = async () => {
    // Optimistic UI update
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);

    try {
      await notificationService.markAllAsRead();
      window.dispatchEvent(new CustomEvent('devflow:notifications-updated'));
    } catch {
      fetchUnreadCount();
    }
  };

  const badgeText = unreadCount > 99 ? '99+' : unreadCount;

  return (
    <div className="df-notification-bell-container">
      <button
        type="button"
        onClick={handleToggle}
        className={`df-notification-bell-btn ${isOpen ? 'df-notification-bell-active' : ''}`}
        aria-label={unreadCount > 0 ? `${unreadCount} unread notifications` : 'Notifications'}
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        data-testid="notification-bell-button"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="df-bell-icon"
        >
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>

        {unreadCount > 0 && (
          <span className="df-notification-count-badge" data-testid="notification-badge">
            {badgeText}
          </span>
        )}
      </button>

      <NotificationDropdown
        isOpen={isOpen}
        onClose={handleClose}
        notifications={notifications}
        unreadCount={unreadCount}
        isLoading={isLoading}
        onMarkRead={handleMarkRead}
        onMarkAllRead={handleMarkAllRead}
      />
    </div>
  );
};

export default NotificationBell;
