import React, { useState, useEffect, useCallback } from 'react';
import LoadingState from '../common/LoadingState';
import ErrorState from '../common/ErrorState';
import activityService from '../../services/activityService';
import { formatDateTime } from '../../utils/date';
import {
  ISSUE_STATUS_LABELS,
  ISSUE_PRIORITY_LABELS,
} from '../../constants/issueEnums';

const ACTIVITY_PAGE_SIZE = 20;

/**
 * IssueActivity Component
 * Displays a chronological audit log / activity timeline for an issue.
 */
const IssueActivity = ({ issueId, members = [] }) => {
  const [activities, setActivities] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Pagination
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: ACTIVITY_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  // Refresh trigger
  const [refreshKey, setRefreshKey] = useState(0);
  const handleRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadActivities() {
      setIsLoading(true);
      setFetchError(null);

      try {
        const data = await activityService.getActivitiesByIssue(issueId, {
          page,
          size: ACTIVITY_PAGE_SIZE,
          sort: 'createdAt,desc',
        });

        if (!ignore) {
          setActivities(data.content || []);
          setPagination({
            page: data.page ?? page,
            size: data.size ?? ACTIVITY_PAGE_SIZE,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
            first: data.first ?? true,
            last: data.last ?? true,
          });
        }
      } catch (err) {
        if (!ignore) {
          setFetchError(
            err.normalized || {
              status: 500,
              message: 'Failed to load activity history for this issue.',
            }
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadActivities();

    return () => {
      ignore = true;
    };
  }, [issueId, page, refreshKey]);

  // Lookup actor information from members list
  const getActorName = (userId) => {
    if (!userId) return 'System';
    const member = members.find((m) => m.userId === userId || m.user?.id === userId);
    return member?.user?.name || member?.user?.email || `User #${userId.substring(0, 8)}`;
  };

  // Human-readable formatters
  const formatValue = (action, val) => {
    if (!val) return 'None';
    if (action === 'STATUS_CHANGED') {
      return ISSUE_STATUS_LABELS[val] || val;
    }
    if (action === 'PRIORITY_CHANGED') {
      return ISSUE_PRIORITY_LABELS[val] || val;
    }
    if (action === 'ASSIGNED' || action === 'UNASSIGNED') {
      return getActorName(val);
    }
    return val;
  };

  const renderActionDescription = (activity) => {
    const { action, oldValue, newValue } = activity;

    switch (action) {
      case 'ISSUE_CREATED':
        return <span className="df-activity-action">created this issue</span>;

      case 'STATUS_CHANGED':
        return (
          <>
            <span className="df-activity-action">changed status</span>
            {(oldValue || newValue) && (
              <div className="df-activity-diff">
                {oldValue && (
                  <span className="df-activity-diff-old">
                    {formatValue(action, oldValue)}
                  </span>
                )}
                <span className="df-activity-diff-arrow">→</span>
                <span className="df-activity-diff-new">
                  {formatValue(action, newValue)}
                </span>
              </div>
            )}
          </>
        );

      case 'PRIORITY_CHANGED':
        return (
          <>
            <span className="df-activity-action">changed priority</span>
            {(oldValue || newValue) && (
              <div className="df-activity-diff">
                {oldValue && (
                  <span className="df-activity-diff-old">
                    {formatValue(action, oldValue)}
                  </span>
                )}
                <span className="df-activity-diff-arrow">→</span>
                <span className="df-activity-diff-new">
                  {formatValue(action, newValue)}
                </span>
              </div>
            )}
          </>
        );

      case 'ASSIGNED':
        return (
          <span className="df-activity-action">
            assigned to <strong>{formatValue(action, newValue)}</strong>
          </span>
        );

      case 'UNASSIGNED':
        return (
          <span className="df-activity-action">
            unassigned from <strong>{formatValue(action, oldValue)}</strong>
          </span>
        );

      case 'COMMENT_ADDED':
        return <span className="df-activity-action">added a comment</span>;

      default:
        return (
          <span className="df-activity-action">
            performed {action ? action.replace(/_/g, ' ').toLowerCase() : 'an action'}
          </span>
        );
    }
  };

  return (
    <section className="df-section-card" aria-label="Issue activity history">
      <div className="df-section-header">
        <div>
          <h2 className="df-section-title" style={{ fontSize: '1rem' }}>
            Activity History ({pagination.totalElements})
          </h2>
          <p className="df-section-subtitle">
            Audit log of status updates, field modifications, and collaborator actions.
          </p>
        </div>
      </div>

      {isLoading ? (
        <LoadingState message="Loading activity history..." />
      ) : fetchError ? (
        <ErrorState
          title="Unable to load activity"
          message={fetchError.message}
          onRetry={handleRefresh}
        />
      ) : activities.length === 0 ? (
        <div
          className="df-empty-container"
          style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--df-text-subtle)' }}
        >
          <p style={{ fontStyle: 'italic', fontSize: '0.875rem' }}>
            No activity recorded yet for this issue.
          </p>
        </div>
      ) : (
        <div className="df-activity-timeline">
          {activities.map((activity) => {
            const actorName = getActorName(activity.userId);
            return (
              <div
                key={activity.id}
                className="df-activity-item"
                data-testid={`activity-item-${activity.id}`}
              >
                <span className="df-activity-dot" aria-hidden="true" />
                <div className="df-activity-header">
                  <span className="df-activity-actor">{actorName}</span>
                  {renderActionDescription(activity)}
                  <time className="df-activity-date" dateTime={activity.createdAt}>
                    {formatDateTime(activity.createdAt)}
                  </time>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Activities Pagination */}
      {pagination.totalPages > 1 && (
        <div className="df-pagination" style={{ marginTop: '1.25rem' }}>
          <button
            type="button"
            className="df-pagination-btn"
            disabled={pagination.first || isLoading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            aria-label="Previous page of activity"
          >
            Previous
          </button>
          <span className="df-pagination-info">
            Page {pagination.page + 1} of {pagination.totalPages}
          </span>
          <button
            type="button"
            className="df-pagination-btn"
            disabled={pagination.last || isLoading}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Next page of activity"
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
};

export default IssueActivity;
