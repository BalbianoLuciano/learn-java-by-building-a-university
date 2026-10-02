import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { Ajv2020 } from 'ajv/dist/2020.js';
import { parse as parseYaml } from 'yaml';
import { describe, expect, it } from 'vitest';

const root = fileURLToPath(new URL('..', import.meta.url));

function readJson(path: string): unknown {
  return JSON.parse(readFileSync(`${root}${path}`, 'utf8'));
}

function readYaml(path: string): unknown {
  return parseYaml(readFileSync(`${root}${path}`, 'utf8'));
}

function validatorFor(schemaFile: string) {
  const ajv = new Ajv2020({
    allErrors: true,
    strict: true,
    // The if/then blocks of the step schema require properties declared one level up.
    strictRequired: false,
    allowUnionTypes: true,
  });
  const validate = ajv.compile(readJson(schemaFile) as object);
  return (data: unknown) => {
    const valid = validate(data);
    return { valid, errors: validate.errors ?? [] };
  };
}

function clone<T>(data: T): T {
  return structuredClone(data);
}

describe('trace.schema.json', () => {
  const validate = validatorFor('trace.schema.json');
  const completed = readJson('examples/trace.completed.json') as Record<string, unknown>;

  it('accepts the completed example', () => {
    expect(validate(completed)).toEqual({ valid: true, errors: [] });
  });

  it('accepts the compile error example', () => {
    expect(validate(readJson('examples/trace.compile-error.json')).valid).toBe(true);
  });

  it('rejects an unknown status', () => {
    expect(validate({ ...clone(completed), status: 'exploded' }).valid).toBe(false);
  });

  it('rejects a compile error without diagnostics', () => {
    expect(validate({ status: 'compile_error' }).valid).toBe(false);
  });

  it('rejects a value with two kinds', () => {
    const trace = clone(completed) as { heap: Record<string, { fields: Record<string, unknown> }> };
    trace.heap['o1']!.fields['nombre'] = { string: 'Resistencia', int: 1 };
    expect(validate(trace).valid).toBe(false);
  });

  it('rejects a field set step without its target', () => {
    const trace = clone(completed) as { steps: Record<string, unknown>[] };
    delete trace.steps[1]!['target'];
    expect(validate(trace).valid).toBe(false);
  });
});

describe('result.schema.json', () => {
  const validate = validatorFor('result.schema.json');
  const incomplete = readJson('examples/result.incomplete.json') as Record<string, unknown>;

  it('accepts the incomplete example', () => {
    expect(validate(incomplete)).toEqual({ valid: true, errors: [] });
  });

  it('rejects an unknown outcome', () => {
    expect(validate({ ...clone(incomplete), outcome: 'almost' }).valid).toBe(false);
  });

  it('rejects a piece with an unknown archetype', () => {
    const result = clone(incomplete) as { pieces: Record<string, unknown>[] };
    result.pieces[0]!['archetype'] = 'castle';
    expect(validate(result).valid).toBe(false);
  });
});

describe('challenge.schema.json', () => {
  const validate = validatorFor('challenge.schema.json');
  const challenge = readYaml('examples/challenge.m1-03.yaml') as Record<string, unknown>;

  it('accepts the example of the challenge format spec', () => {
    expect(validate(challenge)).toEqual({ valid: true, errors: [] });
  });

  it('keeps the example in sync with the spec', () => {
    const spec = readFileSync(`${root}../../docs/specs/challenge-format.md`, 'utf8');
    const example = readFileSync(`${root}examples/challenge.m1-03.yaml`, 'utf8');
    expect(spec).toContain(example);
  });

  it('rejects a challenge without exactly three hints', () => {
    const hints = challenge['hints'] as unknown[];
    expect(validate({ ...clone(challenge), hints: hints.slice(0, 2) }).valid).toBe(false);
  });

  it('rejects a text without spanish', () => {
    expect(validate({ ...clone(challenge), title: { en: 'Your faculty' } }).valid).toBe(false);
  });

  it('rejects an unknown check type', () => {
    const invalid = clone(challenge) as { checks: Record<string, unknown>[] };
    invalid.checks[0]!['type'] = 'runs_fast';
    expect(validate(invalid).valid).toBe(false);
  });
});

describe('shared definitions', () => {
  it('declares the same archetypes in result and challenge', () => {
    type WithArchetypes = { $defs: { archetype: { enum: string[] } } };
    const result = readJson('result.schema.json') as WithArchetypes;
    const challenge = readJson('challenge.schema.json') as WithArchetypes;
    expect(challenge.$defs.archetype.enum).toEqual(result.$defs.archetype.enum);
  });
});
