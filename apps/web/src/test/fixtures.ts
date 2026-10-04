import type { ChallengeView, ModuleList, RunResult } from '@ljbu/contracts';
import { vi } from 'vitest';

export const STARTER_MAIN = `public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";
    resistencia.ciudad = "Resistencia";

    // Declará acá la variable miFacultad, que apunte a la misma facultad.

    // Usando solo miFacultad, cargale la provincia.
  }
}
`;

export const SOLVED_MAIN = `public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";
    resistencia.ciudad = "Resistencia";

    FacultadRegional miFacultad = resistencia;
    miFacultad.provincia = "Chaco";
  }
}
`;

export const FACULTAD = `public class FacultadRegional {
  String nombre;
  String ciudad;
  String provincia;
}
`;

export const modules: ModuleList = {
  modules: [
    {
      id: 'm1',
      order: 1,
      title: 'Clases, objetos y referencias',
      goal: 'Entender que una clase es un molde.',
      challenges: [
        {
          id: 'm1-01',
          order: 1,
          title: 'Abrí tu primera Facultad Regional',
          concept: 'classes-and-objects',
          pieces: [{ archetype: 'regional-faculty', label: 'FR Resistencia' }],
        },
        {
          id: 'm1-02',
          order: 2,
          title: 'Tres regionales, tres estados',
          concept: 'object-state',
          pieces: [
            { archetype: 'regional-faculty', label: 'FR Resistencia' },
            { archetype: 'regional-faculty', label: 'FR Córdoba' },
            { archetype: 'regional-faculty', label: 'FR Mendoza' },
          ],
        },
        {
          id: 'm1-03',
          order: 3,
          title: 'La facultad donde estudiás',
          concept: 'aliasing',
          pieces: [{ archetype: 'regional-faculty', label: 'FR Resistencia' }],
        },
      ],
    },
  ],
};

export const aliasing: ChallengeView = {
  id: 'm1-03',
  module: 'm1',
  order: 3,
  title: 'La facultad donde estudiás',
  concept: 'aliasing',
  brief: 'Declará una variable `miFacultad` que apunte a **la misma** facultad.',
  criteria: [
    { checks: ['alias-declared'], text: '`miFacultad` apunta a la FR Resistencia' },
    { checks: ['province-through-alias'], text: 'La provincia se cargó usando `miFacultad`' },
  ],
  files: [
    { path: 'FacultadRegional.java', content: FACULTAD, editable: false },
    { path: 'Main.java', content: STARTER_MAIN, editable: true },
  ],
  hintCount: 3,
  rules: [
    {
      id: 'R-UNI-02',
      statement: 'Hay 30 Facultades Regionales, cada una con nombre, ciudad y provincia',
      source: 'Sedes de la UTN',
      url: 'https://www.utn.edu.ar/es/la-universidad/sedes',
    },
  ],
  realReference: {
    regionalFaculties: [
      {
        id: 'resistencia',
        name: 'Facultad Regional Resistencia',
        city: 'Resistencia',
        province: 'Chaco',
      },
    ],
    governingBodies: [],
  },
  analogy: {
    passed: 'Dos etiquetas colgadas del mismo edificio.',
    incomplete: 'Tenés dos edificios con el mismo nombre; el pedido era una segunda etiqueta.',
    failed: 'La ejecución se cortó: una etiqueta apunta a nada.',
  },
  references: [
    {
      title: 'Variables',
      url: 'https://docs.oracle.com/javase/tutorial/java/nutsandbolts/variables.html',
      source: 'Tutoriales de Java (Oracle)',
    },
  ],
};

export const passedResult: RunResult = {
  runId: 'run-1',
  outcome: 'passed',
  progress: { passed: 2, total: 2 },
  pieces: [
    {
      id: 'fr-resistencia',
      archetype: 'regional-faculty',
      state: 'passed',
      built: true,
      label: 'Resistencia',
      sourceRef: { file: 'Main.java', line: 3 },
      type: 'FacultadRegional',
      fields: [
        { name: 'nombre', value: { string: 'Resistencia' } },
        { name: 'ciudad', value: { string: 'Resistencia' } },
        { name: 'provincia', value: { string: 'Chaco' } },
      ],
    },
    {
      id: 'var-resistencia',
      archetype: 'variable-sign',
      state: 'passed',
      built: true,
      label: 'resistencia',
      sourceRef: { file: 'Main.java', line: 3 },
      target: 'fr-resistencia',
    },
    {
      id: 'var-miFacultad',
      archetype: 'variable-sign',
      state: 'passed',
      built: true,
      label: 'miFacultad',
      sourceRef: { file: 'Main.java', line: 7 },
      target: 'fr-resistencia',
    },
  ],
  log: [
    {
      state: 'passed',
      title: '`miFacultad` y `resistencia` apuntan a la misma facultad.',
      why: 'Asignar una referencia no copia el objeto: ahora hay dos cables al mismo edificio.',
      sourceRef: { file: 'Main.java', line: 7 },
      pieceId: 'fr-resistencia',
      checkId: 'alias-declared',
    },
    {
      state: 'passed',
      title: 'La FR Resistencia ahora tiene su provincia: Chaco.',
      why: 'La cargaste a través de `miFacultad` y se ve también desde `resistencia`: es el mismo objeto.',
      sourceRef: { file: 'Main.java', line: 8 },
      pieceId: 'fr-resistencia',
      checkId: 'province-through-alias',
    },
  ],
  timeline: [
    { index: 0, sourceRef: { file: 'Main.java', line: 3 }, event: 'call', name: 'Main.main' },
    {
      index: 1,
      sourceRef: { file: 'Main.java', line: 3 },
      event: 'object_created',
      pieceId: 'fr-resistencia',
      name: 'FacultadRegional',
    },
    {
      index: 2,
      sourceRef: { file: 'Main.java', line: 7 },
      event: 'local_set',
      pieceId: 'var-miFacultad',
      targetPieceId: 'fr-resistencia',
      name: 'miFacultad',
      value: { ref: 'o1' },
    },
    {
      index: 3,
      sourceRef: { file: 'Main.java', line: 8 },
      event: 'field_set',
      pieceId: 'fr-resistencia',
      name: 'provincia',
      value: { string: 'Chaco' },
    },
  ],
  stdout: '',
  classes: [
    {
      name: 'FacultadRegional',
      kind: 'class',
      abstract: false,
      superclass: null,
      interfaces: [],
      file: 'FacultadRegional.java',
      line: 1,
      fields: [
        {
          name: 'nombre',
          type: 'String',
          visibility: 'package',
          final: false,
          static: false,
          line: 2,
        },
        {
          name: 'ciudad',
          type: 'String',
          visibility: 'package',
          final: false,
          static: false,
          line: 3,
        },
        {
          name: 'provincia',
          type: 'String',
          visibility: 'package',
          final: false,
          static: false,
          line: 4,
        },
      ],
      constructors: [{ parameterTypes: [], visibility: 'public', line: 1 }],
      methods: [],
    },
  ],
};

export const incompleteResult: RunResult = {
  ...passedResult,
  runId: 'run-0',
  outcome: 'incomplete',
  progress: { passed: 0, total: 2 },
  pieces: passedResult.pieces
    .slice(0, 1)
    .map((piece) => ({ ...piece, state: 'incomplete' as const })),
  log: [
    {
      state: 'incomplete',
      title: 'Creaste una segunda facultad en vez de apuntar a la que ya existía.',
      why: 'Cada `new` construye un objeto nuevo; para compartir uno que ya existe se asigna su referencia.',
      sourceRef: { file: 'Main.java', line: 7 },
      hint: '¿Qué tenés que poner a la derecha del `=` para no crear otra facultad?',
      pieceId: 'fr-resistencia',
      checkId: 'alias-declared',
    },
    {
      state: 'incomplete',
      title: 'La provincia de la FR Resistencia todavía no se cargó.',
      why: 'Un cambio hecho a través de cualquiera de las dos variables modifica el mismo objeto.',
      sourceRef: { file: 'Main.java', line: 3 },
      hint: 'Usá `miFacultad`, un punto y el atributo que querés cargar.',
      pieceId: 'fr-resistencia',
      checkId: 'province-through-alias',
    },
  ],
};

/** A fake api: answers by path, and remembers what was asked. */
export function fakeApi(answers: Record<string, unknown>, status = 200) {
  const calls: { url: string; init?: RequestInit }[] = [];
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    calls.push({ url, init });
    const path = url.replace(/^.*\/api\/v1/, '');
    const answer = answers[path];
    if (answer === undefined) {
      return Promise.resolve(
        new Response(JSON.stringify({ status: 404, code: 'challenge_not_found' }), {
          status: 404,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      );
    }
    return Promise.resolve(
      new Response(JSON.stringify(answer), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
  });
  vi.stubGlobal('fetch', fetchMock);
  return calls;
}
