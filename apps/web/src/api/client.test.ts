import { ApiError, api } from './client';
import { fakeApi, modules } from '../test/fixtures';

describe('api client', () => {
  it('reads the answer of the api', async () => {
    fakeApi({ '/modules': modules });

    await expect(api.modules()).resolves.toEqual(modules);
  });

  it('turns a problem+json answer into an error with its code', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve(
          new Response(JSON.stringify({ status: 429, code: 'rate_limited' }), {
            status: 429,
            headers: { 'Content-Type': 'application/problem+json' },
          }),
        ),
      ),
    );

    await expect(api.run({ challengeId: 'm1-03', files: [] })).rejects.toMatchObject({
      status: 429,
      code: 'rate_limited',
    });
  });

  it('reports a network failure as such', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('offline'))),
    );

    await expect(api.modules()).rejects.toEqual(new ApiError(0, 'network'));
  });

  it('posts a run as JSON', async () => {
    const calls = fakeApi({ '/runs': { runId: 'x' } });

    await api.run({ challengeId: 'm1-03', files: [{ path: 'Main.java', content: 'x' }] });

    expect(calls[0]?.url).toMatch(/\/api\/v1\/runs$/);
    expect(calls[0]?.init?.method).toBe('POST');
    expect(JSON.parse(calls[0]?.init?.body as string)).toEqual({
      challengeId: 'm1-03',
      files: [{ path: 'Main.java', content: 'x' }],
    });
  });
});
