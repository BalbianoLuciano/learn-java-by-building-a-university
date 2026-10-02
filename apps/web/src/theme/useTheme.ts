import { useSyncExternalStore } from 'react';
import { create } from 'zustand';
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

/** The manual choice, shared by every component: the toggle and the editor agree. */
interface ChoiceState {
  chosen: Theme | null;
  choose: (theme: Theme) => void;
}

const useChoice = create<ChoiceState>()((set) => ({
  chosen: readStoredTheme(),
  choose: (theme) => {
    document.documentElement.dataset.theme = theme;
    storeTheme(theme);
    set({ chosen: theme });
  },
}));

/** The active theme: the manual choice if there is one, the system preference otherwise. */
export function useTheme(): { theme: Theme; toggleTheme: () => void } {
  const systemTheme = useSyncExternalStore(subscribeToSystemTheme, getSystemTheme);
  const chosen = useChoice((state) => state.chosen);
  const choose = useChoice((state) => state.choose);
  const theme = chosen ?? systemTheme;
  return {
    theme,
    toggleTheme: () => {
      choose(theme === 'dark' ? 'light' : 'dark');
    },
  };
}

/** For tests: forget the manual choice. */
export function resetThemeChoice(): void {
  useChoice.setState({ chosen: null });
}
