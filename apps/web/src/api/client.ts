import type {
  ChallengeView,
  Hint,
  ModuleList,
  RunRequest,
  RunResult,
  Solution,
} from '@ljbu/contracts';

/** An answer the api did not give: a problem+json error, or no answer at all. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(`${String(status)} ${code}`);
  }
}

const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}/api/v1${path}`, init);
  } catch {
    throw new ApiError(0, 'network');
  }
  if (!response.ok) {
    let code = 'unknown';
    try {
      const problem = (await response.json()) as { code?: unknown };
      if (typeof problem.code === 'string') {
        code = problem.code;
      }
    } catch {
      // Not a problem+json body: the status is all we know.
    }
    throw new ApiError(response.status, code);
  }
  return (await response.json()) as T;
}

export const api = {
  modules: () => request<ModuleList>('/modules'),
  challenge: (id: string) => request<ChallengeView>(`/challenges/${id}`),
  hint: (id: string, level: number) => request<Hint>(`/challenges/${id}/hints/${String(level)}`),
  solution: (id: string) => request<Solution>(`/challenges/${id}/solution`),
  run: (body: RunRequest) =>
    request<RunResult>('/runs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
};
