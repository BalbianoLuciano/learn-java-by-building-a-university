import { useCallback, useState, useSyncExternalStore } from 'react';
import { readStoredTheme, storeTheme, type Theme } from './theme';

const DARK_QUERY = '(prefers-color-scheme: dark)';

function subscribeToSystemTheme(onChange: () => void): () => void {
  const query = window.matchMedia(DARK_QUERY);
  query.addEventListener('change', onChange);
  return () => {
    query.removeEventListener('change', onChange);
  };
}

function getSystemTheme(): Theme {
  return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light';
}

/** The active theme: the manual choice if there is one, the system preference otherwise. */
export function useTheme(): { theme: Theme; toggleTheme: () => void } {
  const systemTheme = useSyncExternalStore(subscribeToSystemTheme, getSystemTheme);
  const [chosenTheme, setChosenTheme] = useState(readStoredTheme);
  const theme = chosenTheme ?? systemTheme;

  const toggleTheme = useCallback(() => {
    const next = theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    storeTheme(next);
    setChosenTheme(next);
  }, [theme]);

  return { theme, toggleTheme };
}
