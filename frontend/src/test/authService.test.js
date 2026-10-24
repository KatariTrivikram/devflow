import { describe, it, expect, vi, afterEach } from 'vitest';
import api from '../services/api';
import authService from '../services/authService';
import { USER_ROLES } from '../constants/roles';

describe('authService', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('login sends credentials to POST /api/auth/login and returns response data', async () => {
    const mockResponse = {
      accessToken: 'jwt-login-token',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'user-uuid-1',
        name: 'Jane Doe',
        email: 'jane@example.com',
        role: USER_ROLES.DEVELOPER,
      },
    };

    const postSpy = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: mockResponse });

    const result = await authService.login({
      email: 'jane@example.com',
      password: 'SecurePassword123!',
    });

    expect(postSpy).toHaveBeenCalledWith('/api/auth/login', {
      email: 'jane@example.com',
      password: 'SecurePassword123!',
    });
    expect(result).toEqual(mockResponse);
  });

  it('register sends registration payload to POST /api/auth/register and returns response data', async () => {
    const mockResponse = {
      accessToken: 'jwt-register-token',
      tokenType: 'Bearer',
      expiresIn: 3600000,
      user: {
        id: 'user-uuid-2',
        name: 'Alex Smith',
        email: 'alex@example.com',
        role: USER_ROLES.PROJECT_MANAGER,
      },
    };

    const postSpy = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: mockResponse });

    const result = await authService.register({
      name: 'Alex Smith',
      email: 'alex@example.com',
      password: 'SecurePassword123!',
      role: USER_ROLES.PROJECT_MANAGER,
    });

    expect(postSpy).toHaveBeenCalledWith('/api/auth/register', {
      name: 'Alex Smith',
      email: 'alex@example.com',
      password: 'SecurePassword123!',
      role: USER_ROLES.PROJECT_MANAGER,
    });
    expect(result).toEqual(mockResponse);
  });
});
