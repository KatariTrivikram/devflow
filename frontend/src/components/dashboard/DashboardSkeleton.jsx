import React from 'react';

/**
 * DashboardSkeleton Component
 * Accessible loading skeleton matching the layout of the DevFlow dashboard.
 */
export const DashboardSkeleton = () => {
  return (
    <div className="df-dashboard-skeleton" aria-label="Loading dashboard metrics" aria-busy="true">
      <div className="df-skeleton-kpi-grid">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="df-skeleton-card">
            <div className="df-skeleton-line df-skeleton-w-40" />
            <div className="df-skeleton-line df-skeleton-w-20 df-skeleton-h-lg" />
            <div className="df-skeleton-line df-skeleton-w-60" />
          </div>
        ))}
      </div>

      <div className="df-skeleton-main-grid">
        <div className="df-skeleton-card df-skeleton-h-300">
          <div className="df-skeleton-line df-skeleton-w-30" />
          <div className="df-skeleton-line df-skeleton-w-full df-skeleton-h-md" />
          <div className="df-skeleton-line df-skeleton-w-80" />
          <div className="df-skeleton-line df-skeleton-w-60" />
        </div>
        <div className="df-skeleton-card df-skeleton-h-300">
          <div className="df-skeleton-line df-skeleton-w-30" />
          <div className="df-skeleton-line df-skeleton-w-full df-skeleton-h-md" />
          <div className="df-skeleton-line df-skeleton-w-80" />
          <div className="df-skeleton-line df-skeleton-w-60" />
        </div>
      </div>

      <div className="df-skeleton-card df-skeleton-h-250">
        <div className="df-skeleton-line df-skeleton-w-30" />
        <div className="df-skeleton-line df-skeleton-w-90" />
        <div className="df-skeleton-line df-skeleton-w-70" />
        <div className="df-skeleton-line df-skeleton-w-80" />
      </div>
    </div>
  );
};

export default DashboardSkeleton;
