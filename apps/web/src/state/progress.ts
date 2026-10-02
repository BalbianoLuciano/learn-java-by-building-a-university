import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { safeStorage } from './storage';

export type ChallengeStatus = 'pending' | 'completed' | 'completed-with-solution';

export interface ChallengeProgress {
  status: ChallengeStatus;
  /** Executions so far: hints and the solution unlock with them (docs/FEEDBACK.md §7). */
  runs: number;
  /** Highest hint level seen. */
  hintsSeen: number;
  solutionSeen: boolean;
}

interface ProgressState {
  challenges: Record<string, ChallengeProgress>;
  recordRun: (challengeId: string, passed: boolean) => void;
  recordHint: (challengeId: string, level: number) => void;
  recordSolution: (challengeId: string) => void;
}

export const EMPTY_PROGRESS: ChallengeProgress = {
  status: 'pending',
  runs: 0,
  hintsSeen: 0,
  solutionSeen: false,
};

/** Hint 1 is always available; 2 after one run; 3 after two runs. */
export function hintAvailable(progress: ChallengeProgress, level: number): boolean {
  return level <= 1 || progress.runs >= level - 1;
}

/** The solution unlocks after three runs or the three hints. */
export function solutionAvailable(progress: ChallengeProgress): boolean {
  return progress.runs >= 3 || progress.hintsSeen >= 3;
}

export const useProgress = create<ProgressState>()(
  persist(
    (set) => ({
      challenges: {},
      recordRun: (challengeId, passed) =>
        set((state) => {
          const current = state.challenges[challengeId] ?? EMPTY_PROGRESS;
          let status = current.status;
          if (passed && status === 'pending') {
            status = current.solutionSeen ? 'completed-with-solution' : 'completed';
          }
          return {
            challenges: {
              ...state.challenges,
              [challengeId]: { ...current, status, runs: current.runs + 1 },
            },
          };
        }),
      recordHint: (challengeId, level) =>
        set((state) => {
          const current = state.challenges[challengeId] ?? EMPTY_PROGRESS;
          return {
            challenges: {
              ...state.challenges,
              [challengeId]: { ...current, hintsSeen: Math.max(current.hintsSeen, level) },
            },
          };
        }),
      recordSolution: (challengeId) =>
        set((state) => {
          const current = state.challenges[challengeId] ?? EMPTY_PROGRESS;
          const status = current.status === 'completed' ? 'completed' : current.status;
          return {
            challenges: {
              ...state.challenges,
              [challengeId]: { ...current, status, solutionSeen: true },
            },
          };
        }),
    }),
    { name: 'ljbu.progress.v1', storage: createJSONStorage(() => safeStorage) },
  ),
);

export function progressOf(
  challenges: Record<string, ChallengeProgress>,
  id: string,
): ChallengeProgress {
  return challenges[id] ?? EMPTY_PROGRESS;
}
