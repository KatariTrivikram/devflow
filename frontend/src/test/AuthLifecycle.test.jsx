import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import AppRoutes from '../routes/AppRoutes';
import authService from '../services/authService';
import { clearAuth, setToken, setUser, getToken, getUser } from '../utils/storage';
import { USER_ROLES } from '../constants/roles';

const renderApp = (initialEntry = '/login') => {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('End-to-End Authentication Lifecycle', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('completes Login -> Dashboard Display -> Sign out flow', async () => {
    const mockAuthResponse = {
      accessToken: 'lifecycle-jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'user-lifecycle-uuid',
        name: 'Alex Engineer',
        email: 'alex@devflow.io',
        role: USER_ROLES.PROJECT_MANAGER,
      },
    };

    vi.spyOn(authService, 'login').mockResolvedValueOnce(mockAuthResponse);

    renderApp('/login');

    // 1. Verify Login form is visible
    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();

    // 2. Fill in credentials and submit
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'alex@devflow.io' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    // 3. User is navigated to Dashboard
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    });

    // 4. Verify Authenticated Profile displayed on Dashboard
    expect(screen.getByTestId('dashboard-user-name')).toHaveTextContent('Alex Engineer');
    expect(screen.getByTestId('dashboard-user-email')).toHaveTextContent('alex@devflow.io');
    expect(screen.getByTestId('dashboard-user-role')).toHaveTextContent('Project Manager');
    expect(screen.getByTestId('dashboard-user-id')).toHaveTextContent('user-lifecycle-uuid');
    expect(screen.getByText(/authenticated \(active jwt\)/i)).toBeInTheDocument();

    // 5. Verify credentials in storage
    expect(getToken()).toBe('lifecycle-jwt-token');
    expect(getUser()).toEqual(mockAuthResponse.user);

    // 6. Click "Sign out" button in Header
    const signOutBtn = screen.getByRole('button', { name: /sign out/i });
    fireEvent.click(signOutBtn);

    // 7. Verify redirected back to /login and storage is purged
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    });

    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
  });

  it('reactive 401 unauthorized event purges auth and redirects to /login', async () => {
    setToken('active-token-to-expire');
    setUser({
      id: 'expired-user-id',
      name: 'Expired Session User',
      email: 'expired@example.com',
      role: USER_ROLES.DEVELOPER,
    });

    renderApp('/dashboard');

    // 1. Confirm user is on Dashboard
    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-user-name')).toHaveTextContent('Expired Session User');

    // 2. Dispatch the custom devflow:unauthorized event (which Axios interceptor emits on HTTP 401)
    await act(async () => {
      window.dispatchEvent(new CustomEvent('devflow:unauthorized'));
    });

    // 3. Verify user is automatically booted back to Login
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    });

    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
  });
});
