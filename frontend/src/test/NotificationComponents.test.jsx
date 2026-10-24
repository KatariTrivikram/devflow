import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import NotificationItem from '../components/notifications/NotificationItem';
import NotificationEmptyState from '../components/notifications/NotificationEmptyState';
import NotificationDropdown from '../components/notifications/NotificationDropdown';
import NotificationBell from '../components/notifications/NotificationBell';
import notificationService from '../services/notificationService';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../services/notificationService', () => ({
  default: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}));

describe('Notification Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const sampleNotification = {
    id: 'n-123',
    type: 'ISSUE_ASSIGNED',
    title: 'New Issue Assignment',
    message: 'You were assigned to DEV-42',
    referenceType: 'ISSUE',
    referenceId: 'issue-42',
    actorName: 'Alice Dev',
    read: false,
    createdAt: new Date().toISOString(),
  };

  describe('NotificationItem', () => {
    it('renders unread notification with title, message, and actor', () => {
      render(
        <BrowserRouter>
          <NotificationItem notification={sampleNotification} onMarkRead={vi.fn()} />
        </BrowserRouter>
      );

      expect(screen.getByText('New Issue Assignment')).toBeInTheDocument();
      expect(screen.getByText('You were assigned to DEV-42')).toBeInTheDocument();
      expect(screen.getByText('Alice Dev')).toBeInTheDocument();
      expect(screen.getByTitle('Unread')).toBeInTheDocument();
    });

    it('triggers onMarkRead when quick mark button is clicked', () => {
      const handleMarkRead = vi.fn();
      render(
        <BrowserRouter>
          <NotificationItem notification={sampleNotification} onMarkRead={handleMarkRead} />
        </BrowserRouter>
      );

      const quickBtn = screen.getByTitle('Mark as read');
      fireEvent.click(quickBtn);

      expect(handleMarkRead).toHaveBeenCalledWith('n-123');
    });

    it('navigates to issue details when clicked', () => {
      const handleMarkRead = vi.fn();
      render(
        <BrowserRouter>
          <NotificationItem notification={sampleNotification} onMarkRead={handleMarkRead} />
        </BrowserRouter>
      );

      const item = screen.getByTestId('notification-item-n-123');
      fireEvent.click(item);

      expect(handleMarkRead).toHaveBeenCalledWith('n-123');
      expect(mockNavigate).toHaveBeenCalledWith('/issues/issue-42');
    });

    it('navigates to project when referenceType is PROJECT', () => {
      const projectNotification = {
        ...sampleNotification,
        id: 'n-proj',
        referenceType: 'PROJECT',
        referenceId: 'proj-99',
        read: true,
      };

      render(
        <BrowserRouter>
          <NotificationItem notification={projectNotification} onMarkRead={vi.fn()} />
        </BrowserRouter>
      );

      fireEvent.click(screen.getByTestId('notification-item-n-proj'));
      expect(mockNavigate).toHaveBeenCalledWith('/projects/proj-99');
    });
  });

  describe('NotificationEmptyState', () => {
    it('renders custom empty state title and message', () => {
      render(
        <NotificationEmptyState
          title="Custom empty title"
          message="Custom empty message description"
        />
      );

      expect(screen.getByText('Custom empty title')).toBeInTheDocument();
      expect(screen.getByText('Custom empty message description')).toBeInTheDocument();
    });
  });

  describe('NotificationDropdown', () => {
    it('renders null when isOpen is false', () => {
      const { container } = render(
        <BrowserRouter>
          <NotificationDropdown isOpen={false} onClose={vi.fn()} />
        </BrowserRouter>
      );

      expect(container.firstChild).toBeNull();
    });

    it('renders notification items and unread count badge when open', () => {
      const handleMarkAllRead = vi.fn();
      render(
        <BrowserRouter>
          <NotificationDropdown
            isOpen={true}
            onClose={vi.fn()}
            notifications={[sampleNotification]}
            unreadCount={1}
            onMarkAllRead={handleMarkAllRead}
          />
        </BrowserRouter>
      );

      expect(screen.getByText('Notifications')).toBeInTheDocument();
      expect(screen.getByTestId('dropdown-unread-pill')).toHaveTextContent('1 new');
      expect(screen.getByText('New Issue Assignment')).toBeInTheDocument();

      const markAllBtn = screen.getByTestId('dropdown-mark-all-read');
      fireEvent.click(markAllBtn);
      expect(handleMarkAllRead).toHaveBeenCalled();
    });

    it('navigates to /notifications and closes on View All click', () => {
      const handleClose = vi.fn();
      render(
        <BrowserRouter>
          <NotificationDropdown
            isOpen={true}
            onClose={handleClose}
            notifications={[]}
          />
        </BrowserRouter>
      );

      const viewAllBtn = screen.getByText('View all notifications');
      fireEvent.click(viewAllBtn);

      expect(handleClose).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/notifications');
    });

    it('closes on Escape key press', () => {
      const handleClose = vi.fn();
      render(
        <BrowserRouter>
          <NotificationDropdown
            isOpen={true}
            onClose={handleClose}
            notifications={[]}
          />
        </BrowserRouter>
      );

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(handleClose).toHaveBeenCalled();
    });
  });

  describe('NotificationBell', () => {
    it('fetches unread count on mount and displays badge', async () => {
      notificationService.getUnreadCount.mockResolvedValueOnce({ count: 5 });

      render(
        <BrowserRouter>
          <NotificationBell />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(notificationService.getUnreadCount).toHaveBeenCalled();
        expect(screen.getByTestId('notification-badge')).toHaveTextContent('5');
      });
    });

    it('toggles dropdown and fetches notifications on click', async () => {
      notificationService.getUnreadCount.mockResolvedValueOnce({ count: 2 });
      notificationService.getNotifications.mockResolvedValueOnce({
        content: [sampleNotification],
      });

      render(
        <BrowserRouter>
          <NotificationBell />
        </BrowserRouter>
      );

      const bellBtn = screen.getByTestId('notification-bell-button');
      fireEvent.click(bellBtn);

      await waitFor(() => {
        expect(notificationService.getNotifications).toHaveBeenCalledWith({ page: 0, size: 7 });
        expect(screen.getByTestId('notification-dropdown')).toBeInTheDocument();
      });
    });
  });
});
