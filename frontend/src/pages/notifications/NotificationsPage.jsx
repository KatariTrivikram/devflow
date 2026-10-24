import React, { useState, useEffect } from 'react';
import notificationService from '../../services/notificationService';
import NotificationItem from '../../components/notifications/NotificationItem';
import NotificationEmptyState from '../../components/notifications/NotificationEmptyState';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

const PAGE_SIZE = 15;

export const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [isMarkingAll, setIsMarkingAll] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let ignore = false;

    async function loadNotifications() {
      try {
        const data = await notificationService.getNotifications({
          unreadOnly: unreadOnly ? true : undefined,
          page,
          size: PAGE_SIZE,
          sort: 'createdAt,desc',
        });

        if (!ignore) {
          setNotifications(data?.content || []);
          setPagination({
            page: data?.page ?? 0,
            size: data?.size ?? PAGE_SIZE,
            totalElements: data?.totalElements ?? 0,
            totalPages: data?.totalPages ?? 0,
            first: data?.first ?? true,
            last: data?.last ?? true,
          });
          setFetchError(null);
        }
      } catch (err) {
        if (!ignore) {
          setFetchError(err.normalized?.message || err.message || 'Failed to load notifications');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadNotifications();

    return () => {
      ignore = true;
    };
  }, [unreadOnly, page, refreshKey]);

  const refreshNotifications = () => {
    setIsLoading(true);
    setFetchError(null);
    setRefreshKey((k) => k + 1);
  };

  const handleTabChange = (onlyUnread) => {
    if (unreadOnly !== onlyUnread) {
      setUnreadOnly(onlyUnread);
      setPage(0);
    }
  };

  const handleMarkRead = async (id) => {
    // Optimistic local update
    setNotifications((prev) =>
      prev.map((item) => (item.id === id ? { ...item, read: true } : item))
    );

    try {
      await notificationService.markAsRead(id);
      window.dispatchEvent(new CustomEvent('devflow:notifications-updated'));
    } catch {
      // Re-fetch on error to ensure sync
      refreshNotifications();
    }
  };

  const handleMarkAllRead = async () => {
    setIsMarkingAll(true);
    // Optimistic update
    setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));

    try {
      await notificationService.markAllAsRead();
      window.dispatchEvent(new CustomEvent('devflow:notifications-updated'));
      // If we are on the unread filter tab, re-fetch to reflect empty unread list
      if (unreadOnly) {
        refreshNotifications();
      }
    } catch (err) {
      setFetchError(err.normalized?.message || 'Failed to mark all notifications as read');
      refreshNotifications();
    } finally {
      setIsMarkingAll(false);
    }
  };

  const hasUnread = notifications.some((n) => !(n.read ?? n.isRead));

  return (
    <div className="df-notifications-page" data-testid="notifications-page">
      {/* Page Header */}
      <div className="df-page-header">
        <div>
          <h1 className="df-page-title">Notifications</h1>
          <p className="df-page-subtitle">
            Stay updated on your issue assignments, status transitions, and team activities.
          </p>
        </div>

        <div className="df-header-actions">
          <button
            type="button"
            onClick={handleMarkAllRead}
            disabled={isMarkingAll || (!hasUnread && unreadOnly)}
            className="df-btn df-btn-secondary"
            data-testid="mark-all-read-btn"
          >
            {isMarkingAll ? 'Marking read...' : 'Mark all as read'}
          </button>
        </div>
      </div>

      {/* Tabs Filter */}
      <div className="df-notifications-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={!unreadOnly}
          onClick={() => handleTabChange(false)}
          className={`df-notifications-tab ${!unreadOnly ? 'df-notifications-tab-active' : ''}`}
          data-testid="tab-all"
        >
          All Notifications
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={unreadOnly}
          onClick={() => handleTabChange(true)}
          className={`df-notifications-tab ${unreadOnly ? 'df-notifications-tab-active' : ''}`}
          data-testid="tab-unread"
        >
          Unread Only
        </button>
      </div>

      {/* Main Content Area */}
      <div className="df-notifications-card">
        {isLoading ? (
          <LoadingState message="Loading notifications..." />
        ) : fetchError ? (
          <ErrorState
            title="Unable to load notifications"
            message={fetchError}
            onRetry={refreshNotifications}
          />
        ) : notifications.length === 0 ? (
          <NotificationEmptyState
            title={unreadOnly ? 'No unread notifications' : 'No notifications yet'}
            message={
              unreadOnly
                ? "You're all caught up! You have no unread notifications right now."
                : 'Activity and alerts relevant to you will appear here once triggered.'
            }
          />
        ) : (
          <div className="df-notifications-list">
            {notifications.map((item) => (
              <NotificationItem
                key={item.id}
                notification={item}
                onMarkRead={handleMarkRead}
              />
            ))}
          </div>
        )}

        {/* Pagination Footer */}
        {!isLoading && !fetchError && pagination.totalPages > 1 && (
          <div className="df-pagination-bar">
            <span className="df-pagination-info">
              Showing page {pagination.page + 1} of {pagination.totalPages} ({pagination.totalElements} total)
            </span>

            <div className="df-pagination-actions">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={pagination.first || page === 0}
                className="df-btn df-btn-secondary df-btn-sm"
                data-testid="pagination-prev"
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={pagination.last || page >= pagination.totalPages - 1}
                className="df-btn df-btn-secondary df-btn-sm"
                data-testid="pagination-next"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
