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
  const ajv = new Ajv2020({ allErrors: true, strict: true, allowUnionTypes: true });
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

  it('rejects a field set step without its value', () => {
    const trace = clone(completed) as { steps: Record<string, unknown>[] };
    const fieldSet = trace.steps.find((step) => step['event'] === 'field_set')!;
    delete fieldSet['value'];
    expect(validate(trace).valid).toBe(false);
  });

  it('rejects a step with a property of another event', () => {
    const trace = clone(completed) as { steps: Record<string, unknown>[] };
    const returned = trace.steps.find((step) => step['event'] === 'return')!;
    returned['text'] = 'Resistencia';
    expect(validate(trace).valid).toBe(false);
  });

  it('accepts lists, maps and non-finite doubles in the heap', () => {
    const trace = clone(completed) as { heap: Record<string, unknown> };
    trace.heap['o2'] = { type: 'ArrayList', size: 1, elements: [{ ref: 'o1' }] };
    trace.heap['o3'] = {
      type: 'HashMap',
      size: 1,
      entries: [{ key: { string: 'promedio' }, value: { double: 'NaN' } }],
    };
    expect(validate(trace)).toEqual({ valid: true, errors: [] });
  });
});

describe('execution-request.schema.json', () => {
  const validate = validatorFor('execution-request.schema.json');
  const request = readJson('examples/execution-request.json') as Record<string, unknown>;

  it('accepts the example', () => {
    expect(validate(request)).toEqual({ valid: true, errors: [] });
  });

  it('rejects a file name with a path', () => {
    const files = [{ path: '../Main.java', content: '' }];
    expect(validate({ ...clone(request), files }).valid).toBe(false);
  });

  it('rejects a timeout above the sandbox limit', () => {
    expect(validate({ ...clone(request), limits: { timeoutMs: 60000 } }).valid).toBe(false);
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
  // The example of the spec is a real challenge: the published one, not a copy.
  const published =
    '../../content/challenges/m1-clases-y-objetos/03-la-facultad-donde-estudias/challenge.yaml';
  const challenge = readYaml(published) as Record<string, unknown>;

  it('accepts the challenge that the format spec shows as its example', () => {
    expect(validate(challenge)).toEqual({ valid: true, errors: [] });
  });

  it('keeps the example of the spec in sync with the published challenge', () => {
    const spec = readFileSync(`${root}../../docs/specs/challenge-format.md`, 'utf8');
    expect(spec).toContain(readFileSync(`${root}${published}`, 'utf8'));
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

  it('rejects a scene that does not say which pieces the brief asks for', () => {
    const invalid = clone(challenge) as { scene: Record<string, unknown> };
    delete invalid.scene['pieces'];
    expect(validate(invalid).valid).toBe(false);
  });
});

describe('module.schema.json', () => {
  const validate = validatorFor('module.schema.json');

  it('accepts the published module', () => {
    const module = readYaml('../../content/challenges/m1-clases-y-objetos/module.yaml');
    expect(validate(module)).toEqual({ valid: true, errors: [] });
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
