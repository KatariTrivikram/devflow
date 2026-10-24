import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ProjectStatusBadge from '../components/projects/ProjectStatusBadge';
import { PROJECT_STATUS } from '../constants/projectEnums';

describe('ProjectStatusBadge Component', () => {
  it('renders Active status pill correctly', () => {
    render(<ProjectStatusBadge status={PROJECT_STATUS.ACTIVE} />);
    const badge = screen.getByRole('status');
    expect(badge).toHaveTextContent('Active');
    expect(badge).toHaveClass('df-status-active');
  });

  it('renders Archived status pill correctly', () => {
    render(<ProjectStatusBadge status={PROJECT_STATUS.ARCHIVED} />);
    const badge = screen.getByRole('status');
    expect(badge).toHaveTextContent('Archived');
    expect(badge).toHaveClass('df-status-archived');
  });

  it('renders Completed status pill correctly', () => {
    render(<ProjectStatusBadge status={PROJECT_STATUS.COMPLETED} />);
    const badge = screen.getByRole('status');
    expect(badge).toHaveTextContent('Completed');
    expect(badge).toHaveClass('df-status-completed');
  });

  it('falls back to Active for unknown or missing status', () => {
    render(<ProjectStatusBadge status={null} />);
    const badge = screen.getByRole('status');
    expect(badge).toHaveTextContent('Active');
  });
});
