import React from 'react';
import EmptyState from '../../components/common/EmptyState';

export const IssuesPage = () => {
  return (
    <div className="df-page">
      <div className="df-page-header">
        <div>
          <h1 className="df-page-title">Issues</h1>
          <p className="df-page-subtitle">
            Track and triage bugs, features, tasks, and improvements across all projects.
          </p>
        </div>
      </div>

      <div className="df-section-card">
        <EmptyState
          title="Issue Tracker Initialized"
          description="Frontend issue routing and API contract alignment is ready. Dynamic filtering, sorting, comments, and activity audit trails will be connected in the next step."
        />
      </div>
    </div>
  );
};

export default IssuesPage;
