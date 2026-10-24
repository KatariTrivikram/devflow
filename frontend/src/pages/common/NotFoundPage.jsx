import React from 'react';
import { Link } from 'react-router-dom';

export const NotFoundPage = () => {
  return (
    <div className="df-status-page">
      <div className="df-status-card">
        <div className="df-status-code">404</div>
        <h1 className="df-status-title">Page Not Found</h1>
        <p className="df-status-description">
          The page you are looking for does not exist or may have been moved.
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

export default NotFoundPage;
