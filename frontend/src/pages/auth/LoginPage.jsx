import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import ErrorState from '../../components/common/ErrorState';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [generalError, setGeneralError] = useState(null);

  // Preserve redirect path if redirected from a protected route
  const from = location.state?.from?.pathname || '/dashboard';

  const validateForm = () => {
    const errors = {};
    if (!formData.email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!EMAIL_REGEX.test(formData.email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }

    if (!formData.password) {
      errors.password = 'Password is required.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    // Clear field-specific error upon typing
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
    if (generalError) {
      setGeneralError(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      setGeneralError(null);

      await login({
        email: formData.email.trim(),
        password: formData.password,
      });

      // Successful authentication: navigate to originally requested route
      navigate(from, { replace: true });
    } catch (err) {
      const normalized = err.normalized;
      if (normalized?.status === 401) {
        setGeneralError({
          title: 'Authentication Failed',
          message: 'Invalid email or password. Please try again.',
        });
      } else if (normalized?.status === 400) {
        if (normalized?.errors && Object.keys(normalized.errors).length > 0) {
          setFieldErrors(normalized.errors);
        }
        setGeneralError({
          title: 'Invalid Request',
          message: normalized?.message || 'Please check your submitted credentials.',
        });
      } else if (normalized?.isNetworkError) {
        setGeneralError({
          title: 'Network Error',
          message: 'Unable to reach DevFlow backend. Please verify your connection or backend server status.',
        });
      } else {
        setGeneralError({
          title: 'Sign in Error',
          message: normalized?.message || 'An unexpected error occurred during sign in. Please try again.',
        });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="df-auth-page">
      <div className="df-auth-card">
        <div className="df-auth-header">
          <div className="df-brand-logo df-brand-logo-lg">DF</div>
          <h1 className="df-auth-title">Welcome back</h1>
          <p className="df-auth-subtitle">Sign in to your DevFlow workspace</p>
        </div>

        {generalError && (
          <ErrorState
            title={generalError.title}
            message={generalError.message}
            compact
          />
        )}

        <form onSubmit={handleSubmit} className="df-form" noValidate>
          <div className="df-form-group">
            <label htmlFor="login-email" className="df-label">
              Email address
            </label>
            <input
              id="login-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={formData.email}
              onChange={handleChange}
              placeholder="you@example.com"
              className={`df-input ${fieldErrors.email ? 'df-input-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'login-email-error' : undefined}
            />
            {fieldErrors.email && (
              <span id="login-email-error" className="df-field-error" role="alert">
                {fieldErrors.email}
              </span>
            )}
          </div>

          <div className="df-form-group">
            <label htmlFor="login-password" className="df-label">
              Password
            </label>
            <input
              id="login-password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              className={`df-input ${fieldErrors.password ? 'df-input-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'login-password-error' : undefined}
            />
            {fieldErrors.password && (
              <span id="login-password-error" className="df-field-error" role="alert">
                {fieldErrors.password}
              </span>
            )}
          </div>

          <button
            type="submit"
            className="df-btn df-btn-primary df-btn-block"
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="df-auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="df-link">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
