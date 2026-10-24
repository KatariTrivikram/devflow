import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { USER_ROLES, USER_ROLE_LABELS } from '../../constants/roles';
import ErrorState from '../../components/common/ErrorState';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const RegisterPage = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    role: USER_ROLES.DEVELOPER,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [generalError, setGeneralError] = useState(null);

  const validateForm = () => {
    const errors = {};
    const trimmedName = formData.name.trim();
    const trimmedEmail = formData.email.trim();

    if (!trimmedName) {
      errors.name = 'Full name is required.';
    } else if (trimmedName.length > 100) {
      errors.name = 'Full name cannot exceed 100 characters.';
    }

    if (!trimmedEmail) {
      errors.email = 'Email address is required.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      errors.email = 'Please enter a valid email address.';
    } else if (trimmedEmail.length > 100) {
      errors.email = 'Email address cannot exceed 100 characters.';
    }

    if (!formData.password) {
      errors.password = 'Password is required.';
    } else if (formData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters in length.';
    } else if (formData.password.length > 255) {
      errors.password = 'Password cannot exceed 255 characters.';
    }

    if (!formData.role) {
      errors.role = 'Platform role is required.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

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

      const response = await register({
        name: formData.name.trim(),
        email: formData.email.trim(),
        password: formData.password,
        role: formData.role,
      });

      // If backend returns credentials, user is immediately authenticated
      if (response?.accessToken) {
        navigate('/dashboard', { replace: true });
      } else {
        navigate('/login', {
          replace: true,
          state: { message: 'Account successfully registered! Please sign in.' },
        });
      }
    } catch (err) {
      const normalized = err.normalized;
      if (normalized?.status === 409) {
        setGeneralError({
          title: 'Account Already Exists',
          message: 'A user with this email address is already registered. Please sign in or use a different email.',
        });
        setFieldErrors((prev) => ({ ...prev, email: 'Email address already in use.' }));
      } else if (normalized?.status === 400) {
        if (normalized?.errors && Object.keys(normalized.errors).length > 0) {
          setFieldErrors(normalized.errors);
        }
        setGeneralError({
          title: 'Registration Validation Failed',
          message: normalized?.message || 'Please correct the highlighted fields.',
        });
      } else if (normalized?.isNetworkError) {
        setGeneralError({
          title: 'Network Error',
          message: 'Unable to reach DevFlow backend. Please verify your connection or backend server status.',
        });
      } else {
        setGeneralError({
          title: 'Registration Failed',
          message: normalized?.message || 'An unexpected error occurred during account creation. Please try again.',
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
          <h1 className="df-auth-title">Create an account</h1>
          <p className="df-auth-subtitle">Get started with your DevFlow team</p>
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
            <label htmlFor="reg-name" className="df-label">
              Full name
            </label>
            <input
              id="reg-name"
              name="name"
              type="text"
              autoComplete="name"
              required
              maxLength={100}
              value={formData.name}
              onChange={handleChange}
              placeholder="Jane Doe"
              className={`df-input ${fieldErrors.name ? 'df-input-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.name)}
              aria-describedby={fieldErrors.name ? 'reg-name-error' : undefined}
            />
            {fieldErrors.name && (
              <span id="reg-name-error" className="df-field-error" role="alert">
                {fieldErrors.name}
              </span>
            )}
          </div>

          <div className="df-form-group">
            <label htmlFor="reg-email" className="df-label">
              Email address
            </label>
            <input
              id="reg-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              maxLength={100}
              value={formData.email}
              onChange={handleChange}
              placeholder="jane@example.com"
              className={`df-input ${fieldErrors.email ? 'df-input-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'reg-email-error' : undefined}
            />
            {fieldErrors.email && (
              <span id="reg-email-error" className="df-field-error" role="alert">
                {fieldErrors.email}
              </span>
            )}
          </div>

          <div className="df-form-group">
            <label htmlFor="reg-password" className="df-label">
              Password <span className="df-label-hint">(min. 8 characters)</span>
            </label>
            <input
              id="reg-password"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={255}
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              className={`df-input ${fieldErrors.password ? 'df-input-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'reg-password-error' : undefined}
            />
            {fieldErrors.password && (
              <span id="reg-password-error" className="df-field-error" role="alert">
                {fieldErrors.password}
              </span>
            )}
          </div>

          <div className="df-form-group">
            <label htmlFor="reg-role" className="df-label">
              Platform Role
            </label>
            <select
              id="reg-role"
              name="role"
              value={formData.role}
              onChange={handleChange}
              className={`df-select ${fieldErrors.role ? 'df-select-error' : ''}`}
              disabled={loading}
              aria-invalid={Boolean(fieldErrors.role)}
              aria-describedby={fieldErrors.role ? 'reg-role-error' : undefined}
            >
              {Object.entries(USER_ROLES).map(([key, value]) => (
                <option key={key} value={value}>
                  {USER_ROLE_LABELS[value] || value}
                </option>
              ))}
            </select>
            {fieldErrors.role && (
              <span id="reg-role-error" className="df-field-error" role="alert">
                {fieldErrors.role}
              </span>
            )}
          </div>

          <button
            type="submit"
            className="df-btn df-btn-primary df-btn-block"
            disabled={loading}
          >
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <div className="df-auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="df-link">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
