import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import authService from '../services/authService';
import { clearAuth, getToken, getUser } from '../utils/storage';
import { USER_ROLES } from '../constants/roles';

const renderLoginPage = (initialState = null) => {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state: initialState }]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<div>Dashboard Destination</div>} />
          <Route path="/projects/123" element={<div>Target Protected Project</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('LoginPage Component', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders login form elements cleanly', () => {
    renderLoginPage();

    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create an account/i })).toBeInTheDocument();
  });

  it('client-side validation displays errors and halts submission when fields are empty', async () => {
    const loginSpy = vi.spyOn(authService, 'login');
    renderLoginPage();

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Email address is required.')).toBeInTheDocument();
    expect(await screen.findByText('Password is required.')).toBeInTheDocument();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('client-side validation checks email format', async () => {
    const loginSpy = vi.spyOn(authService, 'login');
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'not-an-email' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Secret123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Please enter a valid email address.')).toBeInTheDocument();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('successful login calls authService, stores credentials, and navigates to /dashboard', async () => {
    const mockAuthResponse = {
      accessToken: 'valid-jwt-token-123',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'u-1',
        name: 'Jane Doe',
        email: 'jane@example.com',
        role: USER_ROLES.DEVELOPER,
      },
    };

    vi.spyOn(authService, 'login').mockResolvedValueOnce(mockAuthResponse);

    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'SecretPassword123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Dashboard Destination')).toBeInTheDocument();
    });

    expect(getToken()).toBe('valid-jwt-token-123');
    expect(getUser()).toEqual(mockAuthResponse.user);
  });

  it('navigates to original protected path after login when location.state.from is present', async () => {
    const mockAuthResponse = {
      accessToken: 'valid-jwt-token-456',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'u-2',
        name: 'Project Lead',
        email: 'lead@example.com',
        role: USER_ROLES.PROJECT_MANAGER,
      },
    };

    vi.spyOn(authService, 'login').mockResolvedValueOnce(mockAuthResponse);

    renderLoginPage({ from: { pathname: '/projects/123' } });

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'lead@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'SecretPassword123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Target Protected Project')).toBeInTheDocument();
    });
  });

  it('displays safe error on 401 invalid credentials', async () => {
    const error401 = new Error('Bad credentials');
    error401.normalized = {
      status: 401,
      message: 'Invalid email or password',
    };

    vi.spyOn(authService, 'login').mockRejectedValueOnce(error401);

    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'WrongPassword!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Authentication Failed')).toBeInTheDocument();
    expect(screen.getByText('Invalid email or password. Please try again.')).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });

  it('displays network failure error cleanly', async () => {
    const networkError = new Error('Network error');
    networkError.normalized = {
      status: 0,
      isNetworkError: true,
      message: 'Unable to connect',
    };

    vi.spyOn(authService, 'login').mockRejectedValueOnce(networkError);

    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Network Error')).toBeInTheDocument();
    expect(screen.getByText(/unable to reach devflow backend/i)).toBeInTheDocument();
  });

  it('disables submit button and shows loading state during submission', async () => {
    let resolveLogin;
    const loginPromise = new Promise((resolve) => {
      resolveLogin = resolve;
    });

    vi.spyOn(authService, 'login').mockImplementation(() => loginPromise);

    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password123!' } });

    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitBtn);

    expect(screen.getByRole('button', { name: /signing in\.\.\./i })).toBeDisabled();
    expect(screen.getByLabelText(/email address/i)).toBeDisabled();
    expect(screen.getByLabelText(/password/i)).toBeDisabled();

    // Resolve login
    resolveLogin({
      accessToken: 'token-abc',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: { id: 'u-3', name: 'User', email: 'jane@example.com', role: USER_ROLES.DEVELOPER },
    });

    await waitFor(() => {
      expect(screen.getByText('Dashboard Destination')).toBeInTheDocument();
    });
  });
});
