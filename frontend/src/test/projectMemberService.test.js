import { describe, it, expect, vi, afterEach } from 'vitest';
import api from '../services/api';
import projectMemberService from '../services/projectMemberService';

describe('projectMemberService', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('getProjectMembers requests GET /api/project-members/project/:projectId', async () => {
    const mockMembers = [
      { id: 'm1', projectId: 'p1', userId: 'u1', joinedAt: '2026-10-14T10:00:00Z' },
    ];

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockMembers });

    const result = await projectMemberService.getProjectMembers('p1');

    expect(getSpy).toHaveBeenCalledWith('/api/project-members/project/p1');
    expect(result).toEqual(mockMembers);
  });

  it('addMember requests POST /api/project-members with projectId and userId', async () => {
    const payload = { projectId: 'p1', userId: 'u2' };
    const mockResponse = { id: 'm2', ...payload, joinedAt: '2026-10-14T11:00:00Z' };

    const postSpy = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: mockResponse });

    const result = await projectMemberService.addMember(payload);

    expect(postSpy).toHaveBeenCalledWith('/api/project-members', payload);
    expect(result).toEqual(mockResponse);
  });

  it('getMembership requests GET /api/project-members/project/:projectId/user/:userId', async () => {
    const mockResponse = { id: 'm1', projectId: 'p1', userId: 'u1', joinedAt: '2026-10-14T10:00:00Z' };

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockResponse });

    const result = await projectMemberService.getMembership('p1', 'u1');

    expect(getSpy).toHaveBeenCalledWith('/api/project-members/project/p1/user/u1');
    expect(result).toEqual(mockResponse);
  });

  it('getUserMemberships requests GET /api/project-members/user/:userId', async () => {
    const mockResponse = [{ id: 'm1', projectId: 'p1', userId: 'u1', joinedAt: '2026-10-14T10:00:00Z' }];

    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({ data: mockResponse });

    const result = await projectMemberService.getUserMemberships('u1');

    expect(getSpy).toHaveBeenCalledWith('/api/project-members/user/u1');
    expect(result).toEqual(mockResponse);
  });

  it('removeMember requests DELETE /api/project-members/project/:projectId/user/:userId', async () => {
    const deleteSpy = vi.spyOn(api, 'delete').mockResolvedValueOnce({ data: null });

    await projectMemberService.removeMember('p1', 'u1');

    expect(deleteSpy).toHaveBeenCalledWith('/api/project-members/project/p1/user/u1');
  });
});
