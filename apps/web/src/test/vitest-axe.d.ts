import type { AxeMatchers } from 'vitest-axe/matchers';

// vitest-axe declares its matchers for an older vitest; this registers them for this one.
declare module 'vitest' {
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface Assertion extends AxeMatchers {}
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface AsymmetricMatchersContaining extends AxeMatchers {}
}
