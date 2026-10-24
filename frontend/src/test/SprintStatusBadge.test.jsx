import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import SprintStatusBadge from '../components/projects/SprintStatusBadge';
import { SPRINT_STATUS } from '../constants/projectEnums';

describe('SprintStatusBadge Component', () => {
  it('renders Planned status badge with correct styling', () => {
    const { container } = render(<SprintStatusBadge status={SPRINT_STATUS.PLANNED} />);
    const badge = screen.getByText('Planned');
    expect(badge).toBeInTheDocument();
    expect(container.querySelector('.df-status-planned')).toBeInTheDocument();
  });

  it('renders Active status badge with correct styling', () => {
    const { container } = render(<SprintStatusBadge status={SPRINT_STATUS.ACTIVE} />);
    const badge = screen.getByText('Active');
    expect(badge).toBeInTheDocument();
    expect(container.querySelector('.df-status-active')).toBeInTheDocument();
  });

  it('renders Completed status badge with correct styling', () => {
    const { container } = render(<SprintStatusBadge status={SPRINT_STATUS.COMPLETED} />);
    const badge = screen.getByText('Completed');
    expect(badge).toBeInTheDocument();
    expect(container.querySelector('.df-status-completed')).toBeInTheDocument();
  });

  it('defaults gracefully to Planned for missing or unknown status', () => {
    render(<SprintStatusBadge status="UNKNOWN_STATUS" />);
    expect(screen.getByText('Planned')).toBeInTheDocument();

    render(<SprintStatusBadge status={null} />);
    expect(screen.getAllByText('Planned').length).toBe(2);
  });
});
