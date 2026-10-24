import { describe, it, expect } from 'vitest';
import { formatDate, formatDateTime } from '../utils/date';

describe('Date Utilities', () => {
  it('formatDate formats valid ISO string to localized date', () => {
    const formatted = formatDate('2026-10-14T10:00:00Z');
    expect(formatted).toContain('2026');
    expect(formatted).toMatch(/Oct/i);
  });

  it('formatDate handles null, undefined, or empty string gracefully', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');
    expect(formatDate('')).toBe('—');
  });

  it('formatDate handles invalid date strings gracefully', () => {
    expect(formatDate('not-a-valid-date')).toBe('—');
  });

  it('formatDateTime formats ISO string with time component', () => {
    const formatted = formatDateTime('2026-10-14T10:30:00Z');
    expect(formatted).toContain('2026');
    expect(formatted).toMatch(/:\d{2}/);
  });
});
