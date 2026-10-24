import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLE_LABELS } from '../../constants/roles';

export const Sidebar = () => {
  const { user } = useAuth();

  const navItems = [
    {
      to: '/dashboard',
      label: 'Dashboard',
      icon: (
        <svg className="df-nav-icon" viewBox="0 0 20 20" fill="currentColor">
          <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z" />
        </svg>
      ),
    },
    {
      to: '/projects',
      label: 'Projects',
      icon: (
        <svg className="df-nav-icon" viewBox="0 0 20 20" fill="currentColor">
          <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
        </svg>
      ),
    },
    {
      to: '/issues',
      label: 'Issues',
      icon: (
        <svg className="df-nav-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
      ),
    },
    {
      to: '/notifications',
      label: 'Notifications',
      icon: (
        <svg className="df-nav-icon" viewBox="0 0 20 20" fill="currentColor">
          <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z" />
        </svg>
      ),
    },
  ];

  const roleLabel = user?.role ? (USER_ROLE_LABELS[user.role] || user.role) : 'User';

  return (
    <aside className="df-sidebar" aria-label="Main Navigation">
      <div className="df-sidebar-brand">
        <div className="df-brand-logo">DF</div>
        <div className="df-brand-text">
          <span className="df-brand-title">DevFlow</span>
          <span className="df-brand-subtitle">Project Platform</span>
        </div>
      </div>

      <nav className="df-sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `df-nav-link ${isActive ? 'df-nav-link-active' : ''}`
            }
          >
            {item.icon}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="df-sidebar-footer">
        <div className="df-user-badge">
          <div className="df-user-avatar">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="df-user-meta">
            <span className="df-user-name" title={user?.name}>{user?.name || 'User'}</span>
            <span className="df-role-tag">{roleLabel}</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
