/**
 * Date and time formatting utilities for DevFlow.
 * Formats backend ISO-8601 timestamps gracefully with fallback handling.
 */

/**
 * Format an ISO date string to a localized human-readable date.
 * E.g., "Oct 14, 2026"
 *
 * @param {string|Date|null|undefined} dateInput
 * @param {Intl.DateTimeFormatOptions} [options]
 * @returns {string} Formatted date string or fallback
 */
export function formatDate(dateInput, options = {}) {
  if (!dateInput) return '—';

  try {
    let date;
    if (typeof dateInput === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateInput)) {
      const [year, month, day] = dateInput.split('-').map(Number);
      date = new Date(year, month - 1, day);
    } else {
      date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    }
    if (!date || Number.isNaN(date.getTime())) return '—';

    const defaultOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      ...options,
    };

    return new Intl.DateTimeFormat('en-US', defaultOptions).format(date);
  } catch {
    return '—';
  }
}

/**
 * Format an ISO date string to a localized date with time.
 * E.g., "Oct 14, 2026, 10:30 AM"
 *
 * @param {string|Date|null|undefined} dateInput
 * @returns {string} Formatted date and time or fallback
 */
export function formatDateTime(dateInput) {
  return formatDate(dateInput, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * Format an ISO date string to HTML input date format (YYYY-MM-DD).
 *
 * @param {string|Date|null|undefined} dateInput
 * @returns {string} YYYY-MM-DD or empty string
 */
export function toInputDateFormat(dateInput) {
  if (!dateInput) return '';
  try {
    const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    if (Number.isNaN(date.getTime())) return '';
    return date.toISOString().substring(0, 10);
  } catch {
    return '';
  }
}

/**
 * Format an ISO date string to a human-friendly relative time string (e.g. "5m ago", "2h ago", "yesterday").
 *
 * @param {string|Date|null|undefined} dateInput
 * @returns {string} Relative time string or fallback
 */
export function formatRelativeTime(dateInput) {
  if (!dateInput) return '—';

  try {
    const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    if (Number.isNaN(date.getTime())) return '—';

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSecs = Math.floor(diffMs / 1000);

    if (diffSecs < 60) {
      return 'just now';
    }
    const diffMins = Math.floor(diffSecs / 60);
    if (diffMins < 60) {
      return `${diffMins}m ago`;
    }
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) {
      return `${diffHours}h ago`;
    }
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays === 1) {
      return 'yesterday';
    }
    if (diffDays < 7) {
      return `${diffDays}d ago`;
    }

    return formatDate(date);
  } catch {
    return '—';
  }
}

