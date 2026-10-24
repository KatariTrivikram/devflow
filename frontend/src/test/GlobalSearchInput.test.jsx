import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import GlobalSearchInput from '../components/search/GlobalSearchInput';
import searchService from '../services/searchService';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../services/searchService', () => ({
  default: {
    search: vi.fn(),
  },
}));

describe('GlobalSearchInput Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockResults = [
    {
      id: 'proj-1',
      type: 'PROJECT',
      title: 'DevFlow Platform',
      description: 'Core project management platform',
      status: 'ACTIVE',
    },
    {
      id: 'issue-10',
      type: 'ISSUE',
      title: 'Fix login redirection bug',
      projectName: 'DevFlow Platform',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
    },
    {
      id: 'sprint-5',
      type: 'SPRINT',
      title: 'Sprint 5 - Polish',
      projectId: 'proj-1',
      projectName: 'DevFlow Platform',
      status: 'ACTIVE',
    },
  ];

  it('renders input with placeholder and shortcut badge', () => {
    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('placeholder', 'Search projects, issues, sprints...');
  });

  it('triggers debounced search preview and displays grouped dropdown results', async () => {
    searchService.search.mockResolvedValueOnce({ content: mockResults });

    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    fireEvent.change(input, { target: { value: 'DevFlow' } });

    await waitFor(
      () => {
        expect(searchService.search).toHaveBeenCalledWith({
          q: 'DevFlow',
          type: 'ALL',
          size: 6,
        });
      },
      { timeout: 1000 }
    );

    await waitFor(() => {
      expect(screen.getByTestId('search-dropdown')).toBeInTheDocument();
      expect(screen.getByText('Projects (1)')).toBeInTheDocument();
      expect(screen.getByText('Issues (1)')).toBeInTheDocument();
      expect(screen.getByText('Sprints (1)')).toBeInTheDocument();
      expect(screen.getAllByText('DevFlow Platform').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Fix login redirection bug')).toBeInTheDocument();
      expect(screen.getByText('Sprint 5 - Polish')).toBeInTheDocument();
    });
  });

  it('navigates to issue details when issue result is clicked', async () => {
    searchService.search.mockResolvedValueOnce({ content: mockResults });

    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    fireEvent.change(input, { target: { value: 'login' } });

    await waitFor(() => {
      expect(screen.getByTestId('search-item-issue-10')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('search-item-issue-10'));
    expect(mockNavigate).toHaveBeenCalledWith('/issues/issue-10');
  });

  it('clears query and closes dropdown when clear button is clicked', async () => {
    searchService.search.mockResolvedValueOnce({ content: mockResults });

    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    fireEvent.change(input, { target: { value: 'test' } });

    await waitFor(() => {
      expect(screen.getByTestId('search-clear-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('search-clear-btn'));
    expect(input.value).toBe('');
  });

  it('navigates to full search page on Enter or View All click', async () => {
    searchService.search.mockResolvedValueOnce({ content: mockResults });

    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    fireEvent.change(input, { target: { value: 'auth' } });

    await waitFor(() => {
      expect(screen.getByTestId('search-view-all-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('search-view-all-btn'));
    expect(mockNavigate).toHaveBeenCalledWith('/search?q=auth');
  });

  it('closes dropdown on Escape key', async () => {
    searchService.search.mockResolvedValueOnce({ content: mockResults });

    render(
      <BrowserRouter>
        <GlobalSearchInput />
      </BrowserRouter>
    );

    const input = screen.getByTestId('global-search-input');
    fireEvent.change(input, { target: { value: 'auth' } });

    await waitFor(() => {
      expect(screen.getByTestId('search-dropdown')).toBeInTheDocument();
    });

    fireEvent.keyDown(input, { key: 'Escape' });
    expect(screen.queryByTestId('search-dropdown')).not.toBeInTheDocument();
  });
});
