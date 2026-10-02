export type Theme = 'light' | 'dark';

// Keep the key and values in sync with public/theme-init.js.
const STORAGE_KEY = 'ljbu.theme.v1';

export function readStoredTheme(): Theme | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'light' || stored === 'dark' ? stored : null;
  } catch {
    return null;
  }
}

export function storeTheme(theme: Theme): void {
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // Storage unavailable: the choice lasts until the page is closed.
  }
}
