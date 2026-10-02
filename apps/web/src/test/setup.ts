import '@testing-library/jest-dom/vitest';
import '../i18n';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach, expect, vi } from 'vitest';
import * as axeMatchers from 'vitest-axe/matchers';
import { useAttempts } from '../state/attempts';
import { useProgress } from '../state/progress';
import { useResults } from '../state/results';
import { resetThemeChoice } from '../theme/useTheme';

expect.extend(axeMatchers);

// jsdom does not implement matchMedia; tests run with a light system theme.
beforeEach(() => {
  vi.stubGlobal('matchMedia', (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
});

afterEach(() => {
  cleanup();
  localStorage.clear();
  delete document.documentElement.dataset.theme;
  useProgress.setState({ challenges: {} });
  useAttempts.setState({ files: {} });
  useResults.setState({ runs: {} });
  resetThemeChoice();
  vi.unstubAllGlobals();
});
