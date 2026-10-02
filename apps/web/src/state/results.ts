import type { RunResult } from '@ljbu/contracts';
import { create } from 'zustand';

/** The code that was run, by file name, to show the lines the log points to. */
export type Sources = Record<string, string>;

export interface LastRun {
  result: RunResult;
  sources: Sources;
}

/** The last result of each challenge. Only in memory: a reload goes back to the editor. */
interface ResultsState {
  runs: Record<string, LastRun>;
  setRun: (challengeId: string, run: LastRun) => void;
}

export const useResults = create<ResultsState>()((set) => ({
  runs: {},
  setRun: (challengeId, run) => {
    set((state) => ({ runs: { ...state.runs, [challengeId]: run } }));
  },
}));
