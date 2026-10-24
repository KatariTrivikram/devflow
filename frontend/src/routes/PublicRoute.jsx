import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoadingState from '../components/common/LoadingState';

/**
 * Route wrapper that restricts access to unauthenticated visitors (e.g. Login, Register).
 * Redirects authenticated users to the dashboard.
 */
export const PublicRoute = ({ children = null }) => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingState fullPage message="Loading..." />;
  }

  if (isAuthenticated) {
    const destination = location.state?.from?.pathname || '/dashboard';
    return <Navigate to={destination} replace />;
  }

  return children ? children : <Outlet />;
};

export default PublicRoute;
