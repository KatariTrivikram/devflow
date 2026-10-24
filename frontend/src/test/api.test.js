import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import api from '../services/api';
import { setToken, getToken, clearAuth } from '../utils/storage';

describe('Centralized Axios API Configuration & Interceptors', () => {
  beforeEach(() => {
    clearAuth();
    localStorage.clear();
  });

  afterEach(() => {
    clearAuth();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('Axios instance has configured timeout and JSON headers', () => {
    expect(api.defaults.timeout).toBe(15000);
    expect(api.defaults.headers['Content-Type']).toBe('application/json');
    expect(api.defaults.headers['Accept']).toBe('application/json');
  });

  it('Request interceptor attaches Authorization header when JWT token exists', async () => {
    setToken('mock-jwt-token-xyz');

    // Simulate request interceptor execution
    const requestInterceptor = api.interceptors.request.handlers[0];
    const initialConfig = { headers: {} };
    const modifiedConfig = await requestInterceptor.fulfilled(initialConfig);

    expect(modifiedConfig.headers.Authorization).toBe('Bearer mock-jwt-token-xyz');
  });

  it('Request interceptor does not attach Authorization header when token is absent', async () => {
    const requestInterceptor = api.interceptors.request.handlers[0];
    const initialConfig = { headers: {} };
    const modifiedConfig = await requestInterceptor.fulfilled(initialConfig);

    expect(modifiedConfig.headers.Authorization).toBeUndefined();
  });

  it('Response error interceptor normalizes Spring Boot ErrorResponse', async () => {
    const responseInterceptor = api.interceptors.response.handlers[0];
    const mockAxiosError = {
      message: 'Request failed with status code 400',
      config: { url: '/api/projects' },
      response: {
        status: 400,
        data: {
          timestamp: '2026-10-14T10:00:00Z',
          status: 400,
          message: 'Validation failed for request',
          path: '/api/projects',
          errors: {
            name: 'Project name is required',
          },
        },
      },
    };

    await expect(responseInterceptor.rejected(mockAxiosError)).rejects.toMatchObject({
      normalized: {
        status: 400,
        message: 'Validation failed for request',
        path: '/api/projects',
        errors: {
          name: 'Project name is required',
        },
        isNetworkError: false,
      },
    });
  });

  it('Response error interceptor clears auth and dispatches unauthorized event on 401', async () => {
    setToken('expired-token');
    const responseInterceptor = api.interceptors.response.handlers[0];

    const dispatchEventSpy = vi.spyOn(window, 'dispatchEvent');

    const mock401Error = {
      message: 'Request failed with status code 401',
      config: { url: '/api/users/me' },
      response: {
        status: 401,
        data: {
          status: 401,
          message: 'Full authentication is required to access this resource',
          path: '/api/users/me',
        },
      },
    };

    try {
      await responseInterceptor.rejected(mock401Error);
    } catch {
      // Expected rejection
    }

    expect(getToken()).toBeNull();
    expect(dispatchEventSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'devflow:unauthorized',
      })
    );
  });

  it('Response error interceptor sanitizes fallback errors without leaking stack traces', async () => {
    const responseInterceptor = api.interceptors.response.handlers[0];
    const mock500Error = {
      message: 'Internal server failure',
      config: { url: '/api/internal' },
      response: {
        status: 500,
        data: null, // No backend data
      },
    };

    await expect(responseInterceptor.rejected(mock500Error)).rejects.toMatchObject({
      normalized: {
        status: 500,
        message: 'An unexpected server error occurred. Please try again later.',
        errors: null,
      },
    });
  });
});
