import { render, screen } from '@testing-library/react';
import es from '@content/i18n/es.json';
import { HomePage } from './HomePage';

describe('HomePage', () => {
  it('shows the title from the translation file', () => {
    render(<HomePage />);

    expect(screen.getByRole('heading', { level: 1, name: es.home.title })).toBeInTheDocument();
  });

  it('explains the three result states', () => {
    render(<HomePage />);

    const titles = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(titles).toEqual([
      es.state.passed.title,
      es.state.incomplete.title,
      es.state.failed.title,
    ]);
  });
});
