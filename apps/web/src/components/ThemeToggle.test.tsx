import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import es from '@content/i18n/es.json';
import { ThemeToggle } from './ThemeToggle';

describe('ThemeToggle', () => {
  it('follows the system theme until the learner chooses one', () => {
    render(<ThemeToggle />);

    expect(screen.getByRole('button', { name: es.theme.switchToDark })).toBeInTheDocument();
    expect(document.documentElement.dataset.theme).toBeUndefined();
  });

  it('switches the theme and remembers the choice', async () => {
    render(<ThemeToggle />);

    await userEvent.click(screen.getByRole('button', { name: es.theme.switchToDark }));

    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(localStorage.getItem('ljbu.theme.v1')).toBe('dark');
    expect(screen.getByRole('button', { name: es.theme.switchToLight })).toBeInTheDocument();
  });

  it('starts from the remembered choice', () => {
    localStorage.setItem('ljbu.theme.v1', 'dark');

    render(<ThemeToggle />);

    expect(screen.getByRole('button', { name: es.theme.switchToLight })).toBeInTheDocument();
  });

  it('still switches the theme when storage is unavailable', async () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Storage disabled', 'SecurityError');
    });

    render(<ThemeToggle />);
    await userEvent.click(screen.getByRole('button', { name: es.theme.switchToDark }));

    expect(document.documentElement.dataset.theme).toBe('dark');
    setItem.mockRestore();
  });
});
