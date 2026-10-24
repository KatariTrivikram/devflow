import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import AppRoutes from '../routes/AppRoutes';
import { setToken, setUser, clearAuth } from '../utils/storage';
import { USER_ROLES } from '../constants/roles';

const renderWithRouter = (initialEntry = '/') => {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Route Protection Architecture & Initial Pages', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders login page when unauthenticated visitor navigates to /login', async () => {
    renderWithRouter('/login');

    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('renders register page when unauthenticated visitor navigates to /register', async () => {
    renderWithRouter('/register');

    expect(screen.getByRole('heading', { name: /create an account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/platform role/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  it('redirects unauthenticated user accessing protected /dashboard to /login', async () => {
    renderWithRouter('/dashboard');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    });
  });

  it('redirects unauthenticated user accessing protected /projects to /login', async () => {
    renderWithRouter('/projects');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    });
  });

  it('authenticated user can access protected application shell and dashboard', async () => {
    setToken('valid-user-jwt');
    setUser({
      id: 'auth-user-id',
      name: 'Sarah Connor',
      email: 'sarah@example.com',
      role: USER_ROLES.DEVELOPER,
    });

    renderWithRouter('/dashboard');

    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    expect(screen.getAllByText(/sarah connor/i).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Projects' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Issues' })).toBeInTheDocument();
  });

  it('authenticated user navigating to public /login is redirected to /dashboard', async () => {
    setToken('valid-user-jwt');
    setUser({
      id: 'auth-user-id',
      name: 'Sarah Connor',
      email: 'sarah@example.com',
      role: USER_ROLES.DEVELOPER,
    });

    renderWithRouter('/login');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    });
  });

  it('renders unauthorized 403 page on /unauthorized', () => {
    renderWithRouter('/unauthorized');

    expect(screen.getByText('403')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /access denied/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /return to dashboard/i })).toBeInTheDocument();
  });

  it('renders 404 Not Found page for unknown routes', () => {
    renderWithRouter('/non-existent-route-xyz');

    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /page not found/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /return to dashboard/i })).toBeInTheDocument();
  });
});
