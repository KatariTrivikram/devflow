import React from 'react';
import { Link } from 'react-router-dom';

export const UnauthorizedPage = () => {
  return (
    <div className="df-status-page">
      <div className="df-status-card">
        <div className="df-status-code">403</div>
        <h1 className="df-status-title">Access Denied</h1>
        <p className="df-status-description">
          You do not have the required permissions or role to access this resource. If you believe this is an error, please contact your workspace administrator.
        </p>
        <div className="df-status-actions">
          <Link to="/dashboard" className="df-btn df-btn-primary">
            Return to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
};

export default UnauthorizedPage;
