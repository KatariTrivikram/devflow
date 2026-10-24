import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import searchService from '../../services/searchService';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import { formatRelativeTime } from '../../utils/date';

const PAGE_SIZE = 15;

const SEARCH_TABS = [
  { key: 'ALL', label: 'All Results' },
  { key: 'PROJECT', label: 'Projects' },
  { key: 'ISSUE', label: 'Issues' },
  { key: 'SPRINT', label: 'Sprints' },
];

function getResourceBadgeClass(type) {
  switch (type) {
    case 'PROJECT':
      return 'df-search-type-badge df-type-project';
    case 'ISSUE':
      return 'df-search-type-badge df-type-issue';
    case 'SPRINT':
      return 'df-search-type-badge df-type-sprint';
    default:
      return 'df-search-type-badge';
  }
}

export const SearchPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const urlQuery = searchParams.get('q') || '';
  const urlType = searchParams.get('type') || 'ALL';

  const [inputQuery, setInputQuery] = useState(urlQuery);
  const [results, setResults] = useState([]);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // Sync local input state when URL query changed externally
  const [prevUrlQuery, setPrevUrlQuery] = useState(urlQuery);
  if (urlQuery !== prevUrlQuery) {
    setPrevUrlQuery(urlQuery);
    setInputQuery(urlQuery);
    setPage(0);
  }

  // Fetch search results
  useEffect(() => {
    let ignore = false;
    const trimmed = urlQuery.trim();

    async function executeSearch() {
      if (!trimmed) {
        if (!ignore) {
          setResults([]);
          setPagination({
            page: 0,
            size: PAGE_SIZE,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
          });
          setIsLoading(false);
          setError(null);
        }
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const data = await searchService.search({
          q: trimmed,
          type: urlType,
          page,
          size: PAGE_SIZE,
          sort: 'updatedAt,desc',
        });

        if (!ignore) {
          setResults(data?.content || []);
          setPagination({
            page: data?.page ?? 0,
            size: data?.size ?? PAGE_SIZE,
            totalElements: data?.totalElements ?? 0,
            totalPages: data?.totalPages ?? 0,
            first: data?.first ?? true,
            last: data?.last ?? true,
          });
          setError(null);
        }
      } catch (err) {
        if (!ignore) {
          setError(err.normalized?.message || err.message || 'Failed to complete search');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    executeSearch();

    return () => {
      ignore = true;
    };
  }, [urlQuery, urlType, page, refreshKey]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const trimmed = inputQuery.trim();
    if (!trimmed) return;
    setSearchParams({ q: trimmed, type: urlType });
    setPage(0);
  };

  const handleTabChange = (newType) => {
    setSearchParams({ q: urlQuery, type: newType });
    setPage(0);
  };

  const handleResultClick = (item) => {
    if (item.type === 'PROJECT') {
      navigate(`/projects/${item.projectId || item.id}`);
    } else if (item.type === 'ISSUE') {
      navigate(`/issues/${item.id}`);
    } else if (item.type === 'SPRINT') {
      if (item.projectId) {
        navigate(`/projects/${item.projectId}`);
      } else {
        navigate('/projects');
      }
    }
  };

  const handleRetry = () => {
    setRefreshKey((k) => k + 1);
  };

  return (
    <div className="df-search-page" data-testid="search-page">
      {/* Search Page Header */}
      <div className="df-search-page-header">
        <div>
          <h1 className="df-page-title">Global Search</h1>
          <p className="df-page-subtitle">
            Find projects, issues, and sprints across all workspaces accessible to you.
          </p>
        </div>

        {/* Search Bar Form */}
        <form onSubmit={handleSearchSubmit} className="df-search-page-form">
          <div className="df-search-page-input-wrap">
            <svg
              className="df-search-input-icon"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                clipRule="evenodd"
              />
            </svg>
            <input
              type="text"
              value={inputQuery}
              onChange={(e) => setInputQuery(e.target.value)}
              placeholder="Search by name, title, or description..."
              className="df-input df-search-page-input"
              aria-label="Search term"
              data-testid="search-page-input"
            />
          </div>
          <button
            type="submit"
            className="df-btn df-btn-primary"
            data-testid="search-page-submit-btn"
          >
            Search
          </button>
        </form>
      </div>

      {/* Filter Tabs */}
      <div className="df-search-tabs" role="tablist" aria-label="Filter resource type">
        {SEARCH_TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={urlType === tab.key}
            onClick={() => handleTabChange(tab.key)}
            className={`df-search-tab ${urlType === tab.key ? 'df-search-tab-active' : ''}`}
            data-testid={`search-tab-${tab.key.toLowerCase()}`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Results Content Area */}
      <div className="df-search-results-container">
        {isLoading ? (
          <LoadingState message="Searching DevFlow resources..." />
        ) : error ? (
          <ErrorState
            title="Search failed"
            message={error}
            onRetry={handleRetry}
          />
        ) : !urlQuery.trim() ? (
          <div className="df-search-prompt" data-testid="search-prompt">
            <svg
              className="df-icon-lg df-text-muted"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
              />
            </svg>
            <h3 className="df-search-prompt-title">Ready to search</h3>
            <p className="df-search-prompt-desc">
              Type a keyword above to find projects, issues, and sprints.
            </p>
          </div>
        ) : results.length === 0 ? (
          <div className="df-search-no-results" data-testid="search-no-results">
            <h3 className="df-search-no-results-title">No matching results found</h3>
            <p className="df-search-no-results-desc">
              We couldn&apos;t find any records matching &quot;{urlQuery}&quot;. Please verify the
              spelling or broaden your search keywords.
            </p>
          </div>
        ) : (
          <div>
            <div className="df-search-count-banner" data-testid="search-count-banner">
              Found <strong>{pagination.totalElements}</strong> result
              {pagination.totalElements === 1 ? '' : 's'} for &quot;{urlQuery}&quot;
            </div>

            <div className="df-search-results-list">
              {results.map((item) => (
                <div
                  key={`${item.type}-${item.id}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => handleResultClick(item)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleResultClick(item);
                    }
                  }}
                  className="df-search-result-card"
                  data-testid={`search-result-${item.id}`}
                >
                  <div className="df-search-result-header">
                    <span className={getResourceBadgeClass(item.type)}>
                      {item.type}
                    </span>
                    <h3 className="df-search-result-title">{item.title}</h3>
                  </div>

                  {item.description && (
                    <p className="df-search-result-desc">{item.description}</p>
                  )}

                  <div className="df-search-result-footer">
                    <div className="df-search-result-meta">
                      {item.projectName && item.type !== 'PROJECT' && (
                        <span className="df-search-result-project">
                          Project: <strong>{item.projectName}</strong>
                        </span>
                      )}
                      {item.updatedAt && (
                        <span className="df-search-result-time">
                          Updated {formatRelativeTime(item.updatedAt)}
                        </span>
                      )}
                    </div>

                    <div className="df-search-result-pills">
                      {item.priority && (
                        <span className="df-search-pill df-search-pill-priority">
                          {item.priority}
                        </span>
                      )}
                      {item.status && (
                        <span className="df-search-pill df-search-pill-status">
                          {item.status}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination Controls */}
            {pagination.totalPages > 1 && (
              <div className="df-pagination-bar">
                <span className="df-pagination-info">
                  Showing page {pagination.page + 1} of {pagination.totalPages} ({pagination.totalElements} total)
                </span>

                <div className="df-pagination-actions">
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={pagination.first || page === 0}
                    className="df-btn df-btn-secondary df-btn-sm"
                    data-testid="search-prev-btn"
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={pagination.last || page >= pagination.totalPages - 1}
                    className="df-btn df-btn-secondary df-btn-sm"
                    data-testid="search-next-btn"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchPage;
