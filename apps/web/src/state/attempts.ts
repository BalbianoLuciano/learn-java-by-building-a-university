import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { safeStorage } from './storage';

/** The code of each challenge as the learner left it, so nothing is lost on reload. */
interface AttemptsState {
  files: Record<string, Record<string, string>>;
  setFile: (challengeId: string, path: string, content: string) => void;
  reset: (challengeId: string) => void;
}

export const useAttempts = create<AttemptsState>()(
  persist(
    (set) => ({
      files: {},
      setFile: (challengeId, path, content) =>
        set((state) => ({
          files: {
            ...state.files,
            [challengeId]: { ...state.files[challengeId], [path]: content },
          },
        })),
      reset: (challengeId) =>
        set((state) => ({
          files: Object.fromEntries(
            Object.entries(state.files).filter(([id]) => id !== challengeId),
          ),
        })),
    }),
    { name: 'ljbu.code.v1', storage: createJSONStorage(() => safeStorage) },
  ),
);
