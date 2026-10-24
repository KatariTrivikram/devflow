import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import useDebounce from '../../hooks/useDebounce';
import searchService from '../../services/searchService';
import SearchDropdown from './SearchDropdown';

export const GlobalSearchInput = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const navigate = useNavigate();

  const debouncedQuery = useDebounce(query, 300);

  // Global Ctrl/Cmd + K shortcut to focus search input
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        inputRef.current?.focus();
        if (query.trim().length > 0) {
          setIsOpen(true);
        }
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => {
      window.removeEventListener('keydown', handleGlobalKeyDown);
    };
  }, [query]);

  // Click outside listener to close dropdown
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // Execute preview search when debounced query changes
  useEffect(() => {
    let ignore = false;
    const trimmed = debouncedQuery.trim();

    async function fetchPreview() {
      if (!trimmed) {
        if (!ignore) {
          setResults([]);
          setIsLoading(false);
        }
        return;
      }

      setIsLoading(true);
      try {
        const data = await searchService.search({
          q: trimmed,
          type: 'ALL',
          size: 6,
        });

        if (!ignore) {
          setResults(data?.content || []);
          setIsOpen(true);
        }
      } catch {
        if (!ignore) {
          setResults([]);
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    fetchPreview();

    return () => {
      ignore = true;
    };
  }, [debouncedQuery]);

  const handleInputChange = (e) => {
    const val = e.target.value;
    setQuery(val);
    if (!val.trim()) {
      setIsOpen(false);
      setResults([]);
    }
  };

  const handleInputKeyDown = (e) => {
    if (e.key === 'Escape') {
      setIsOpen(false);
      inputRef.current?.blur();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      handleViewAll();
    }
  };

  const handleClear = () => {
    setQuery('');
    setResults([]);
    setIsOpen(false);
    inputRef.current?.focus();
  };

  const handleViewAll = () => {
    const trimmed = query.trim();
    if (!trimmed) return;
    setIsOpen(false);
    navigate(`/search?q=${encodeURIComponent(trimmed)}`);
  };

  const handleSelectResult = (item) => {
    setIsOpen(false);
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

  const isMac = typeof window !== 'undefined' && /Mac|iPod|iPhone|iPad/.test(navigator.userAgent);

  return (
    <div
      ref={containerRef}
      className="df-search-input-container"
      role="search"
      aria-label="Global platform search"
    >
      <div className="df-search-input-wrapper">
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
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={() => {
            if (query.trim().length > 0) {
              setIsOpen(true);
            }
          }}
          onKeyDown={handleInputKeyDown}
          placeholder="Search projects, issues, sprints..."
          aria-label="Search projects, issues, sprints"
          aria-expanded={isOpen}
          aria-controls="global-search-dropdown"
          className="df-search-input"
          data-testid="global-search-input"
        />

        {query ? (
          <button
            type="button"
            onClick={handleClear}
            className="df-search-clear-btn"
            aria-label="Clear search input"
            data-testid="search-clear-btn"
          >
            ×
          </button>
        ) : (
          <kbd className="df-search-shortcut-badge" title="Focus search">
            {isMac ? '⌘K' : 'Ctrl+K'}
          </kbd>
        )}
      </div>

      <SearchDropdown
        isOpen={isOpen}
        isLoading={isLoading}
        query={query}
        results={results}
        onSelectResult={handleSelectResult}
        onViewAll={handleViewAll}
      />
    </div>
  );
};

export default GlobalSearchInput;
