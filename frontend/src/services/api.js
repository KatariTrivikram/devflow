import axios from 'axios';
import { getToken, clearAuth } from '../utils/storage';

/**
 * Centralized Axios instance configuration for DevFlow.
 *
 * Base URL resolution:
 * Uses environment variable VITE_API_BASE_URL (e.g., http://localhost:8080).
 * Strips any redundant trailing slashes to ensure consistent path concatenation.
 */
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
const normalizedBaseUrl = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const api = axios.create({
  baseURL: normalizedBaseUrl,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

/**
 * Request Interceptor:
 * Attaches the Bearer JWT token to outgoing requests if available.
 * Security enforcement:
 * - Never logs tokens to console or output.
 * - Never appends token to URLs or query parameters.
 */
api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response / Error Interceptor:
 * Normalizes HTTP error responses matching DevFlow Spring Boot ErrorResponse schema:
 * { timestamp, status, message, path, errors }
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const responseData = error.response?.data;

    // Build standardized error metadata without leaking raw internal stack traces
    const normalizedError = {
      status: status || 0,
      message: responseData?.message || getFallbackErrorMessage(status, error.message),
      errors: responseData?.errors || null,
      path: responseData?.path || error.config?.url || '',
      timestamp: responseData?.timestamp || new Date().toISOString(),
      isNetworkError: !error.response,
    };

    // Attach normalized error to the Axios error instance
    error.normalized = normalizedError;

    // Handle authentication expiration or revocation
    if (status === 401) {
      clearAuth();
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('devflow:unauthorized', { detail: { path: normalizedError.path } }));
      }
    }

    return Promise.reject(error);
  }
);

function getFallbackErrorMessage(status, defaultMessage) {
  switch (status) {
    case 400:
      return 'The request was invalid. Please check the submitted data.';
    case 401:
      return 'Authentication required. Please sign in again.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'A conflict occurred with an existing resource.';
    case 500:
      return 'An unexpected server error occurred. Please try again later.';
    default:
      return defaultMessage || 'A network error occurred. Please verify your connection.';
  }
}

export default api;
