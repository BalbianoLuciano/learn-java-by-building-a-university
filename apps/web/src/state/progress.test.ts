import { EMPTY_PROGRESS, hintAvailable, solutionAvailable, useProgress } from './progress';

describe('progress', () => {
  it('unlocks hints one run at a time', () => {
    expect(hintAvailable(EMPTY_PROGRESS, 1)).toBe(true);
    expect(hintAvailable(EMPTY_PROGRESS, 2)).toBe(false);
    expect(hintAvailable({ ...EMPTY_PROGRESS, runs: 1 }, 2)).toBe(true);
    expect(hintAvailable({ ...EMPTY_PROGRESS, runs: 1 }, 3)).toBe(false);
    expect(hintAvailable({ ...EMPTY_PROGRESS, runs: 2 }, 3)).toBe(true);
  });

  it('unlocks the solution after three runs or three hints', () => {
    expect(solutionAvailable(EMPTY_PROGRESS)).toBe(false);
    expect(solutionAvailable({ ...EMPTY_PROGRESS, runs: 3 })).toBe(true);
    expect(solutionAvailable({ ...EMPTY_PROGRESS, hintsSeen: 3 })).toBe(true);
  });

  it('marks a challenge completed when a run passes', () => {
    useProgress.getState().recordRun('m1-03', false);
    useProgress.getState().recordRun('m1-03', true);

    expect(useProgress.getState().challenges['m1-03']).toEqual({
      status: 'completed',
      runs: 2,
      hintsSeen: 0,
      solutionSeen: false,
    });
  });

  it('remembers that the solution was seen before completing', () => {
    useProgress.getState().recordSolution('m1-03');
    useProgress.getState().recordRun('m1-03', true);

    expect(useProgress.getState().challenges['m1-03']?.status).toBe('completed-with-solution');
  });

  it('keeps the highest hint level seen', () => {
    useProgress.getState().recordHint('m1-03', 2);
    useProgress.getState().recordHint('m1-03', 1);

    expect(useProgress.getState().challenges['m1-03']?.hintsSeen).toBe(2);
  });

  it('persists in the browser', () => {
    useProgress.getState().recordRun('m1-01', true);

    expect(localStorage.getItem('ljbu.progress.v1')).toContain('"m1-01"');
  });

  it('keeps working when the browser has no storage', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Storage disabled', 'SecurityError');
    });

    useProgress.getState().recordRun('m1-01', true);

    expect(useProgress.getState().challenges['m1-01']?.status).toBe('completed');
    setItem.mockRestore();
  });
});
