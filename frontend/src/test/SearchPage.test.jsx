import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import SearchPage from '../pages/search/SearchPage';
import searchService from '../services/searchService';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('SearchPage Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockSearchResults = [
    {
      id: 'proj-1',
      title: 'Alpha Platform Project',
      description: 'Core backend platform',
      type: 'PROJECT',
      projectId: 'proj-1',
      projectName: 'Alpha Platform Project',
      status: 'ACTIVE',
      updatedAt: '2026-10-14T10:00:00Z',
    },
    {
      id: 'issue-101',
      title: 'Fix auth token expiration',
      description: 'Tokens expire prematurely on refresh',
      type: 'ISSUE',
      projectId: 'proj-1',
      projectName: 'Alpha Core Workspace',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      updatedAt: '2026-10-15T09:00:00Z',
    },
    {
      id: 'sprint-5',
      title: 'Sprint 5 - Auth & Security',
      description: 'Focus on identity & RBAC',
      type: 'SPRINT',
      projectId: 'proj-1',
      projectName: 'Alpha Core Workspace',
      status: 'ACTIVE',
      updatedAt: '2026-10-15T08:00:00Z',
    },
  ];

  it('renders prompt state when no query is provided in the URL', () => {
    render(
      <MemoryRouter initialEntries={['/search']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: 'Global Search' })).toBeInTheDocument();
    expect(screen.getByTestId('search-prompt')).toBeInTheDocument();
    expect(screen.getByText('Ready to search')).toBeInTheDocument();
  });

  it('executes search and displays results when query is in URL', async () => {
    const searchSpy = vi.spyOn(searchService, 'search').mockResolvedValueOnce({
      content: mockSearchResults,
      page: 0,
      size: 15,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
      query: 'auth',
      type: 'ALL',
    });

    render(
      <MemoryRouter initialEntries={['/search?q=auth&type=ALL']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(searchSpy).toHaveBeenCalledWith({
        q: 'auth',
        type: 'ALL',
        page: 0,
        size: 15,
        sort: 'updatedAt,desc',
      });
    });

    await waitFor(() => {
      expect(screen.getByText('Alpha Platform Project')).toBeInTheDocument();
      expect(screen.getByText('Fix auth token expiration')).toBeInTheDocument();
      expect(screen.getByText('Sprint 5 - Auth & Security')).toBeInTheDocument();
    });

    expect(screen.getByTestId('search-count-banner')).toHaveTextContent('Found 3 results for "auth"');
    expect(screen.getByText('PROJECT')).toBeInTheDocument();
    expect(screen.getByText('ISSUE')).toBeInTheDocument();
    expect(screen.getByText('SPRINT')).toBeInTheDocument();
  });

  it('filters results when switching tabs', async () => {
    const searchSpy = vi.spyOn(searchService, 'search')
      .mockResolvedValueOnce({
        content: mockSearchResults,
        page: 0,
        size: 15,
        totalElements: 3,
        totalPages: 1,
        first: true,
        last: true,
      })
      .mockResolvedValueOnce({
        content: [mockSearchResults[1]],
        page: 0,
        size: 15,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(
      <MemoryRouter initialEntries={['/search?q=auth&type=ALL']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Fix auth token expiration')).toBeInTheDocument();
    });

    const issuesTab = screen.getByTestId('search-tab-issue');
    fireEvent.click(issuesTab);

    await waitFor(() => {
      expect(searchSpy).toHaveBeenLastCalledWith({
        q: 'auth',
        type: 'ISSUE',
        page: 0,
        size: 15,
        sort: 'updatedAt,desc',
      });
    });
  });

  it('submits search from page search bar and triggers new search', async () => {
    const searchSpy = vi.spyOn(searchService, 'search').mockResolvedValueOnce({
      content: [mockSearchResults[0]],
      page: 0,
      size: 15,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter initialEntries={['/search']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    const input = screen.getByTestId('search-page-input');
    const submitBtn = screen.getByTestId('search-page-submit-btn');

    fireEvent.change(input, { target: { value: 'Alpha' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(searchSpy).toHaveBeenCalledWith({
        q: 'Alpha',
        type: 'ALL',
        page: 0,
        size: 15,
        sort: 'updatedAt,desc',
      });
    });
  });

  it('navigates to proper route when clicking a project, issue, or sprint card', async () => {
    vi.spyOn(searchService, 'search').mockResolvedValueOnce({
      content: mockSearchResults,
      page: 0,
      size: 15,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter initialEntries={['/search?q=auth']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Platform Project')).toBeInTheDocument();
    });

    // Click Project card
    fireEvent.click(screen.getByTestId('search-result-proj-1'));
    expect(mockNavigate).toHaveBeenCalledWith('/projects/proj-1');

    // Click Issue card
    fireEvent.click(screen.getByTestId('search-result-issue-101'));
    expect(mockNavigate).toHaveBeenCalledWith('/issues/issue-101');

    // Click Sprint card
    fireEvent.click(screen.getByTestId('search-result-sprint-5'));
    expect(mockNavigate).toHaveBeenCalledWith('/projects/proj-1');
  });

  it('displays empty state when query returns no items', async () => {
    vi.spyOn(searchService, 'search').mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 15,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    render(
      <MemoryRouter initialEntries={['/search?q=nonexistent']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('search-no-results')).toBeInTheDocument();
      expect(screen.getByText('No matching results found')).toBeInTheDocument();
    });
  });

  it('displays error state and retries on failure', async () => {
    const searchSpy = vi.spyOn(searchService, 'search')
      .mockRejectedValueOnce({
        normalized: { message: 'Network error occurred' },
      })
      .mockResolvedValueOnce({
        content: [mockSearchResults[0]],
        page: 0,
        size: 15,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });

    render(
      <MemoryRouter initialEntries={['/search?q=alpha']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Search failed')).toBeInTheDocument();
      expect(screen.getByText('Network error occurred')).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Alpha Platform Project')).toBeInTheDocument();
    });

    expect(searchSpy).toHaveBeenCalledTimes(2);
  });

  it('handles pagination next and previous clicks', async () => {
    const searchSpy = vi.spyOn(searchService, 'search')
      .mockResolvedValueOnce({
        content: [mockSearchResults[0]],
        page: 0,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: true,
        last: false,
      })
      .mockResolvedValueOnce({
        content: [mockSearchResults[1]],
        page: 1,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true,
      });

    render(
      <MemoryRouter initialEntries={['/search?q=test']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Platform Project')).toBeInTheDocument();
    });

    const nextBtn = screen.getByTestId('search-next-btn');
    expect(nextBtn).toBeEnabled();

    const prevBtn = screen.getByTestId('search-prev-btn');
    expect(prevBtn).toBeDisabled();

    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(searchSpy).toHaveBeenLastCalledWith({
        q: 'test',
        type: 'ALL',
        page: 1,
        size: 15,
        sort: 'updatedAt,desc',
      });
    });
  });
});
