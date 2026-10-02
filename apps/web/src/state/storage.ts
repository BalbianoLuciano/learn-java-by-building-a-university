import type { StateStorage } from 'zustand/middleware';

/**
 * localStorage that never throws: without storage (private window, blocked data) the app
 * works and just forgets everything when the page closes (docs/ARCHITECTURE.md §8).
 */
export const safeStorage: StateStorage = {
  getItem: (name) => {
    try {
      return localStorage.getItem(name);
    } catch {
      return null;
    }
  },
  setItem: (name, value) => {
    try {
      localStorage.setItem(name, value);
    } catch {
      // Nothing to do: the state stays in memory.
    }
  },
  removeItem: (name) => {
    try {
      localStorage.removeItem(name);
    } catch {
      // Same as above.
    }
  },
};
