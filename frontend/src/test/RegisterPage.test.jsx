import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RegisterPage from '../pages/auth/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import authService from '../services/authService';
import { clearAuth, getToken, getUser } from '../utils/storage';
import { USER_ROLES } from '../constants/roles';

const renderRegisterPage = () => {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/dashboard" element={<div>Dashboard Destination</div>} />
          <Route path="/login" element={<div>Login Destination</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('RegisterPage Component', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders registration form elements cleanly', () => {
    renderRegisterPage();

    expect(screen.getByRole('heading', { name: /create an account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/platform role/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
  });

  it('validates client-side constraints on empty fields', async () => {
    const registerSpy = vi.spyOn(authService, 'register');
    renderRegisterPage();

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Full name is required.')).toBeInTheDocument();
    expect(await screen.findByText('Email address is required.')).toBeInTheDocument();
    expect(await screen.findByText('Password is required.')).toBeInTheDocument();
    expect(registerSpy).not.toHaveBeenCalled();
  });

  it('validates password minimum length of 8 characters', async () => {
    const registerSpy = vi.spyOn(authService, 'register');
    renderRegisterPage();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Alex' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'alex@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'short' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Password must be at least 8 characters in length.')).toBeInTheDocument();
    expect(registerSpy).not.toHaveBeenCalled();
  });

  it('successful registration calls authService.register and navigates to /dashboard', async () => {
    const mockAuthResponse = {
      accessToken: 'register-jwt-token-789',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'u-reg-1',
        name: 'Sarah Connor',
        email: 'sarah@example.com',
        role: USER_ROLES.DEVELOPER,
      },
    };

    const registerSpy = vi.spyOn(authService, 'register').mockResolvedValueOnce(mockAuthResponse);

    renderRegisterPage();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Sarah Connor' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'sarah@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'ValidPass123!' } });
    fireEvent.change(screen.getByLabelText(/platform role/i), { target: { value: USER_ROLES.DEVELOPER } });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText('Dashboard Destination')).toBeInTheDocument();
    });

    expect(registerSpy).toHaveBeenCalledWith({
      name: 'Sarah Connor',
      email: 'sarah@example.com',
      password: 'ValidPass123!',
      role: USER_ROLES.DEVELOPER,
    });

    expect(getToken()).toBe('register-jwt-token-789');
    expect(getUser()).toEqual(mockAuthResponse.user);
  });

  it('handles 409 conflict when email is already registered', async () => {
    const conflictError = new Error('User already exists');
    conflictError.normalized = {
      status: 409,
      message: 'User already exists with email sarah@example.com',
    };

    vi.spyOn(authService, 'register').mockRejectedValueOnce(conflictError);

    renderRegisterPage();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Sarah Connor' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'sarah@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'ValidPass123!' } });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Account Already Exists')).toBeInTheDocument();
    expect(screen.getByText(/already registered/i)).toBeInTheDocument();
    expect(screen.getByText('Email address already in use.')).toBeInTheDocument();
  });

  it('handles 400 validation error from backend with field-specific mapping', async () => {
    const validationError = new Error('Validation failed');
    validationError.normalized = {
      status: 400,
      message: 'Validation failed',
      errors: {
        email: 'must be a well-formed email address',
        password: 'size must be between 8 and 255',
      },
    };

    vi.spyOn(authService, 'register').mockRejectedValueOnce(validationError);

    renderRegisterPage();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Sarah Connor' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'valid@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'ValidPass123!' } });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Registration Validation Failed')).toBeInTheDocument();
    expect(screen.getByText('must be a well-formed email address')).toBeInTheDocument();
    expect(screen.getByText('size must be between 8 and 255')).toBeInTheDocument();
  });

  it('disables form controls during registration submission', async () => {
    let resolveRegister;
    const registerPromise = new Promise((resolve) => {
      resolveRegister = resolve;
    });

    vi.spyOn(authService, 'register').mockImplementation(() => registerPromise);

    renderRegisterPage();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Sarah' } });
    fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'sarah@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'ValidPass123!' } });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(screen.getByRole('button', { name: /creating account\.\.\./i })).toBeDisabled();
    expect(screen.getByLabelText(/full name/i)).toBeDisabled();
    expect(screen.getByLabelText(/email address/i)).toBeDisabled();
    expect(screen.getByLabelText(/password/i)).toBeDisabled();

    // Finish registration
    resolveRegister({
      accessToken: 'jwt-123',
      user: { id: 'u-1', name: 'Sarah', email: 'sarah@example.com', role: USER_ROLES.DEVELOPER },
    });

    await waitFor(() => {
      expect(screen.getByText('Dashboard Destination')).toBeInTheDocument();
    });
  });
});
