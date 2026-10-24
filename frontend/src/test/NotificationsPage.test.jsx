import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import NotificationsPage from '../pages/notifications/NotificationsPage';
import notificationService from '../services/notificationService';

vi.mock('../services/notificationService', () => ({
  default: {
    getNotifications: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}));

describe('NotificationsPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockNotifications = [
    {
      id: 'notif-1',
      type: 'ISSUE_ASSIGNED',
      title: 'Assigned to issue DEV-10',
      message: 'You were assigned to DEV-10: Fix memory leak',
      referenceType: 'ISSUE',
      referenceId: 'issue-10',
      read: false,
      createdAt: '2026-10-15T12:00:00Z',
    },
    {
      id: 'notif-2',
      type: 'COMMENT_ADDED',
      title: 'New comment on DEV-10',
      message: 'Bob commented: "Looks good to me"',
      referenceType: 'ISSUE',
      referenceId: 'issue-10',
      read: true,
      createdAt: '2026-10-15T11:00:00Z',
    },
  ];

  it('renders page header, tabs, and fetches notifications on mount', async () => {
    notificationService.getNotifications.mockResolvedValueOnce({
      content: mockNotifications,
      page: 0,
      size: 15,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    expect(screen.getByText('Notifications')).toBeInTheDocument();
    expect(screen.getByTestId('tab-all')).toBeInTheDocument();
    expect(screen.getByTestId('tab-unread')).toBeInTheDocument();
    expect(screen.getByTestId('mark-all-read-btn')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Assigned to issue DEV-10')).toBeInTheDocument();
      expect(screen.getByText('New comment on DEV-10')).toBeInTheDocument();
    });
  });

  it('switches between tabs and filters unread notifications', async () => {
    notificationService.getNotifications
      .mockResolvedValueOnce({
        content: mockNotifications,
        page: 0,
        size: 15,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
      })
      .mockResolvedValueOnce({
        content: [mockNotifications[0]],
        page: 0,
        size: 15,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Assigned to issue DEV-10')).toBeInTheDocument();
    });

    const unreadTab = screen.getByTestId('tab-unread');
    fireEvent.click(unreadTab);

    await waitFor(() => {
      expect(notificationService.getNotifications).toHaveBeenCalledWith({
        unreadOnly: true,
        page: 0,
        size: 15,
        sort: 'createdAt,desc',
      });
    });
  });

  it('marks all as read when button is clicked', async () => {
    notificationService.getNotifications.mockResolvedValueOnce({
      content: mockNotifications,
      page: 0,
      size: 15,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });
    notificationService.markAllAsRead.mockResolvedValueOnce({
      message: 'All notifications marked as read',
      updatedCount: 1,
    });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Assigned to issue DEV-10')).toBeInTheDocument();
    });

    const markAllBtn = screen.getByTestId('mark-all-read-btn');
    fireEvent.click(markAllBtn);

    await waitFor(() => {
      expect(notificationService.markAllAsRead).toHaveBeenCalled();
    });
  });

  it('renders error state and retries on failure', async () => {
    notificationService.getNotifications
      .mockRejectedValueOnce(new Error('Server unavailable'))
      .mockResolvedValueOnce({
        content: [mockNotifications[0]],
        page: 0,
        size: 15,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Unable to load notifications')).toBeInTheDocument();
    });

    const retryBtn = screen.getByText('Try again');
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Assigned to issue DEV-10')).toBeInTheDocument();
    });
  });

  it('renders pagination controls when totalPages > 1', async () => {
    notificationService.getNotifications
      .mockResolvedValueOnce({
        content: [mockNotifications[0]],
        page: 0,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: true,
        last: false,
      })
      .mockResolvedValueOnce({
        content: [mockNotifications[1]],
        page: 1,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true,
      });

    render(
      <BrowserRouter>
        <NotificationsPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Showing page 1 of 2/)).toBeInTheDocument();
    });

    const nextBtn = screen.getByTestId('pagination-next');
    expect(nextBtn).toBeEnabled();

    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(notificationService.getNotifications).toHaveBeenCalledWith({
        unreadOnly: undefined,
        page: 1,
        size: 15,
        sort: 'createdAt,desc',
      });
    });
  });
});
