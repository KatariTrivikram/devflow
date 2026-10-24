import React from 'react';

/**
 * Returns an SVG icon corresponding to the resource type.
 */
function getResourceIcon(type) {
  switch (type) {
    case 'PROJECT':
      return (
        <svg className="df-search-item-icon df-search-icon-project" viewBox="0 0 20 20" fill="currentColor">
          <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
        </svg>
      );
    case 'ISSUE':
      return (
        <svg className="df-search-item-icon df-search-icon-issue" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
      );
    case 'SPRINT':
      return (
        <svg className="df-search-item-icon df-search-icon-sprint" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
        </svg>
      );
    default:
      return (
        <svg className="df-search-item-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd" />
        </svg>
      );
  }
}

export const SearchDropdown = ({
  isOpen,
  isLoading,
  query,
  results = [],
  onSelectResult,
  onViewAll,
  dropdownRef,
}) => {
  if (!isOpen) {
    return null;
  }

  const projects = results.filter((r) => r.type === 'PROJECT');
  const issues = results.filter((r) => r.type === 'ISSUE');
  const sprints = results.filter((r) => r.type === 'SPRINT');
  const hasResults = results.length > 0;

  return (
    <div
      ref={dropdownRef}
      className="df-search-dropdown"
      role="listbox"
      aria-label="Search suggestions"
      data-testid="search-dropdown"
    >
      {isLoading ? (
        <div className="df-search-dropdown-loading">
          <span className="df-spinner df-spinner-sm" />
          <span>Searching for &quot;{query}&quot;...</span>
        </div>
      ) : !hasResults ? (
        <div className="df-search-dropdown-empty" data-testid="search-empty">
          <p className="df-search-empty-text">No results found for &quot;{query}&quot;</p>
          <span className="df-search-empty-hint">Check spelling or try a different term</span>
        </div>
      ) : (
        <div className="df-search-dropdown-groups">
          {projects.length > 0 && (
            <div className="df-search-group" data-testid="group-projects">
              <div className="df-search-group-header">Projects ({projects.length})</div>
              {projects.map((item) => (
                <div
                  key={`proj-${item.id}`}
                  role="option"
                  aria-selected="false"
                  tabIndex={0}
                  onClick={() => onSelectResult(item)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectResult(item);
                    }
                  }}
                  className="df-search-dropdown-item"
                  data-testid={`search-item-${item.id}`}
                >
                  <div className="df-search-item-left">
                    {getResourceIcon('PROJECT')}
                    <div className="df-search-item-meta">
                      <span className="df-search-item-title">{item.title}</span>
                      {item.description && (
                        <span className="df-search-item-desc">{item.description}</span>
                      )}
                    </div>
                  </div>
                  {item.status && (
                    <span className="df-search-pill df-search-pill-status">
                      {item.status}
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}

          {issues.length > 0 && (
            <div className="df-search-group" data-testid="group-issues">
              <div className="df-search-group-header">Issues ({issues.length})</div>
              {issues.map((item) => (
                <div
                  key={`issue-${item.id}`}
                  role="option"
                  aria-selected="false"
                  tabIndex={0}
                  onClick={() => onSelectResult(item)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectResult(item);
                    }
                  }}
                  className="df-search-dropdown-item"
                  data-testid={`search-item-${item.id}`}
                >
                  <div className="df-search-item-left">
                    {getResourceIcon('ISSUE')}
                    <div className="df-search-item-meta">
                      <span className="df-search-item-title">{item.title}</span>
                      {item.projectName && (
                        <span className="df-search-item-project">{item.projectName}</span>
                      )}
                    </div>
                  </div>
                  <div className="df-search-item-right-pills">
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
              ))}
            </div>
          )}

          {sprints.length > 0 && (
            <div className="df-search-group" data-testid="group-sprints">
              <div className="df-search-group-header">Sprints ({sprints.length})</div>
              {sprints.map((item) => (
                <div
                  key={`sprint-${item.id}`}
                  role="option"
                  aria-selected="false"
                  tabIndex={0}
                  onClick={() => onSelectResult(item)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectResult(item);
                    }
                  }}
                  className="df-search-dropdown-item"
                  data-testid={`search-item-${item.id}`}
                >
                  <div className="df-search-item-left">
                    {getResourceIcon('SPRINT')}
                    <div className="df-search-item-meta">
                      <span className="df-search-item-title">{item.title}</span>
                      {item.projectName && (
                        <span className="df-search-item-project">{item.projectName}</span>
                      )}
                    </div>
                  </div>
                  {item.status && (
                    <span className="df-search-pill df-search-pill-status">
                      {item.status}
                    </span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {query && query.trim().length > 0 && (
        <div className="df-search-dropdown-footer">
          <button
            type="button"
            onClick={onViewAll}
            className="df-search-view-all-btn"
            data-testid="search-view-all-btn"
          >
            View all results for &quot;{query}&quot; →
          </button>
        </div>
      )}
    </div>
  );
};

export default SearchDropdown;
