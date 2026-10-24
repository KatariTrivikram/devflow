import { describe, it, expect, vi, afterEach } from 'vitest';
import api from '../services/api';
import userService from '../services/userService';
import { USER_ROLES } from '../constants/roles';

describe('userService', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('getUserById requests GET /api/users/:id and returns user data', async () => {
    const mockUser = {
      id: 'user-uuid-1',
      name: 'Alice Developer',
      email: 'alice@example.com',
      role: USER_ROLES.DEVELOPER,
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockUser });

    const result = await userService.getUserById('user-uuid-1');

    expect(getSpy).toHaveBeenCalledWith('/api/users/user-uuid-1');
    expect(result).toEqual(mockUser);
  });

  it('getUserByEmail requests GET /api/users/email with query parameter and returns user data', async () => {
    const mockUser = {
      id: 'user-uuid-2',
      name: 'Bob Manager',
      email: 'bob@example.com',
      role: USER_ROLES.PROJECT_MANAGER,
    };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockUser });

    const result = await userService.getUserByEmail('  bob@example.com  ');

    expect(getSpy).toHaveBeenCalledWith('/api/users/email', {
      params: { email: 'bob@example.com' },
    });
    expect(result).toEqual(mockUser);
  });
});
