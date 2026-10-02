import { useCallback, useEffect, useState } from 'react';
import { ApiError } from './client';

export type ApiState<T> =
  { status: 'loading' } | { status: 'error'; error: ApiError } | { status: 'ready'; data: T };

type Settled<T> = { key: string } & ({ data: T } | { error: ApiError });

/** Loads something from the api when the key changes; the caller can ask to retry. */
export function useApi<T>(load: () => Promise<T>, key: string): [ApiState<T>, () => void] {
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<Settled<T> | null>(null);
  const requestKey = `${key}#${String(attempt)}`;

  useEffect(() => {
    let current = true;
    load().then(
      (data) => {
        if (current) {
          setSettled({ key: requestKey, data });
        }
      },
      (failure: unknown) => {
        if (current) {
          setSettled({
            key: requestKey,
            error: failure instanceof ApiError ? failure : new ApiError(0, 'unknown'),
          });
        }
      },
    );
    return () => {
      current = false;
    };
    // The key says when the load changes; the function itself is recreated on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  const retry = useCallback(() => {
    setAttempt((n) => n + 1);
  }, []);

  // An answer for another key is stale: the new one is still loading.
  if (settled === null || settled.key !== requestKey) {
    return [{ status: 'loading' }, retry];
  }
  return [
    'data' in settled
      ? { status: 'ready', data: settled.data }
      : { status: 'error', error: settled.error },
    retry,
  ];
}
