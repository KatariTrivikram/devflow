import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { NotificationBell } from '../notifications/NotificationBell';
import { GlobalSearchInput } from '../search/GlobalSearchInput';

export const Header = () => {
  const { user, logout } = useAuth();

  return (
    <header className="df-header" role="banner">
      <div className="df-header-left">
        <h2 className="df-header-title">Workspace</h2>
      </div>

      <div className="df-header-center">
        <GlobalSearchInput />
      </div>

      <div className="df-header-right">
        <NotificationBell />

        <div className="df-header-user">
          <span className="df-header-email" title={user?.email}>
            {user?.email}
          </span>
          <button
            type="button"
            onClick={logout}
            className="df-btn df-btn-secondary df-btn-sm"
            aria-label="Sign out of account"
          >
            Sign out
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
