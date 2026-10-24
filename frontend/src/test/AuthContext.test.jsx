import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { useAuth } from '../hooks/useAuth';
import authService from '../services/authService';
import { setToken, setUser, getToken, getUser, clearAuth } from '../utils/storage';
import { USER_ROLES } from '../constants/roles';

// Test consumer component
const AuthConsumer = () => {
  const { user, token, isAuthenticated, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="is-authenticated">{isAuthenticated ? 'YES' : 'NO'}</span>
      <span data-testid="user-email">{user?.email || 'NONE'}</span>
      <span data-testid="user-role">{user?.role || 'NONE'}</span>
      <span data-testid="token-value">{token || 'NONE'}</span>
      <button
        onClick={() =>
          login({ email: 'test@example.com', password: 'Password123!' })
        }
      >
        Sign In Button
      </button>
      <button onClick={logout}>Sign Out Button</button>
    </div>
  );
};

describe('AuthContext and useAuth Hook', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('initializes as unauthenticated when localStorage is empty', () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('is-authenticated').textContent).toBe('NO');
    expect(screen.getByTestId('user-email').textContent).toBe('NONE');
    expect(screen.getByTestId('token-value').textContent).toBe('NONE');
  });

  it('initializes from stored token and user if present in localStorage', () => {
    setToken('stored-access-token');
    setUser({
      id: 'stored-id',
      name: 'Stored User',
      email: 'stored@example.com',
      role: USER_ROLES.DEVELOPER,
    });

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('is-authenticated').textContent).toBe('YES');
    expect(screen.getByTestId('user-email').textContent).toBe('stored@example.com');
    expect(screen.getByTestId('user-role').textContent).toBe(USER_ROLES.DEVELOPER);
    expect(screen.getByTestId('token-value').textContent).toBe('stored-access-token');
  });

  it('login updates context state and stores token and safe user payload', async () => {
    const mockAuthData = {
      accessToken: 'fresh-jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'new-id',
        name: 'New Developer',
        email: 'test@example.com',
        role: USER_ROLES.DEVELOPER,
      },
    };

    vi.spyOn(authService, 'login').mockResolvedValueOnce(mockAuthData);

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await act(async () => {
      screen.getByText('Sign In Button').click();
    });

    expect(screen.getByTestId('is-authenticated').textContent).toBe('YES');
    expect(screen.getByTestId('user-email').textContent).toBe('test@example.com');
    expect(screen.getByTestId('token-value').textContent).toBe('fresh-jwt-token');

    expect(getToken()).toBe('fresh-jwt-token');
    expect(getUser()).toEqual(mockAuthData.user);
  });

  it('logout clears localStorage and resets authentication state', async () => {
    setToken('to-be-cleared-token');
    setUser({
      id: 'active-id',
      name: 'Active User',
      email: 'active@example.com',
      role: USER_ROLES.ADMIN,
    });

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('is-authenticated').textContent).toBe('YES');

    await act(async () => {
      screen.getByText('Sign Out Button').click();
    });

    expect(screen.getByTestId('is-authenticated').textContent).toBe('NO');
    expect(screen.getByTestId('user-email').textContent).toBe('NONE');
    expect(screen.getByTestId('token-value').textContent).toBe('NONE');

    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
  });

  it('automatically clears auth on devflow:unauthorized window event', async () => {
    setToken('active-token');
    setUser({
      id: 'active-id',
      name: 'Active User',
      email: 'active@example.com',
      role: USER_ROLES.DEVELOPER,
    });

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('is-authenticated').textContent).toBe('YES');

    await act(async () => {
      window.dispatchEvent(new CustomEvent('devflow:unauthorized'));
    });

    expect(screen.getByTestId('is-authenticated').textContent).toBe('NO');
    expect(getToken()).toBeNull();
  });
});
