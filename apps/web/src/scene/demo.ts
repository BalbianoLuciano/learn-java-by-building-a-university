import type { ClassInfo, Piece, RunResult, TimelineStep } from '@ljbu/contracts';

/**
 * The scene behind the landing page: a program that builds the Rectorado and three
 * faculties, names them and points a second variable at one of them. It is the same model
 * the challenges show, replayed in a loop.
 */
const FACULTIES = [
  ['resistencia', 'Resistencia', 'Resistencia', 'Chaco'],
  ['cordoba', 'Córdoba', 'Córdoba', 'Córdoba'],
  ['mendoza', 'Mendoza', 'Mendoza', 'Mendoza'],
] as const;

function faculty(): ClassInfo {
  return {
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
        visibility: 'private',
        final: false,
        static: false,
        line: 2,
      },
      {
        name: 'ciudad',
        type: 'String',
        visibility: 'private',
        final: false,
        static: false,
        line: 3,
      },
      {
        name: 'provincia',
        type: 'String',
        visibility: 'private',
        final: false,
        static: false,
        line: 4,
      },
    ],
    constructors: [
      { parameterTypes: ['String', 'String', 'String'], visibility: 'public', line: 6 },
    ],
    methods: [
      {
        name: 'describir',
        returnType: 'String',
        parameterTypes: [],
        visibility: 'public',
        static: false,
        abstract: false,
        override: false,
        line: 12,
      },
    ],
  };
}

function rectorate(): ClassInfo {
  return {
    name: 'Rectorado',
    kind: 'class',
    abstract: false,
    superclass: null,
    interfaces: [],
    file: 'Rectorado.java',
    line: 1,
    fields: [
      {
        name: 'direccion',
        type: 'String',
        visibility: 'private',
        final: false,
        static: false,
        line: 2,
      },
    ],
    constructors: [{ parameterTypes: ['String'], visibility: 'public', line: 4 }],
    methods: [],
  };
}

export function demoResult(): RunResult {
  const at = (line: number) => ({ file: 'Main.java', line });
  const pieces: Piece[] = [
    {
      id: 'rectorado',
      archetype: 'rectorate',
      state: 'passed',
      built: true,
      label: 'Rectorado',
      sourceRef: at(3),
      type: 'Rectorado',
      fields: [{ name: 'direccion', value: { string: 'Sarmiento 440' } }],
    },
    ...FACULTIES.map(([id, name, city, province], index) => ({
      id: `fr-${id}`,
      archetype: 'regional-faculty' as const,
      state: 'passed' as const,
      built: true,
      label: name,
      sourceRef: at(5 + index * 2),
      type: 'FacultadRegional',
      fields: [
        { name: 'nombre', value: { string: name } },
        { name: 'ciudad', value: { string: city } },
        { name: 'provincia', value: { string: province } },
      ],
    })),
    ...FACULTIES.map(([id], index) => ({
      id: `var-${id}`,
      archetype: 'variable-sign' as const,
      state: 'passed' as const,
      built: true,
      label: id,
      sourceRef: at(5 + index * 2),
      target: `fr-${id}`,
    })),
    {
      id: 'var-miFacultad',
      archetype: 'variable-sign',
      state: 'passed',
      built: true,
      label: 'miFacultad',
      sourceRef: at(11),
      target: 'fr-resistencia',
    },
  ];
  const timeline: TimelineStep[] = [];
  let index = 0;
  const step = (partial: Omit<TimelineStep, 'index'>) => {
    timeline.push({ index: index++, ...partial });
  };
  step({ sourceRef: at(3), event: 'object_created', pieceId: 'rectorado', name: 'Rectorado' });
  step({ sourceRef: at(3), event: 'call', pieceId: 'rectorado', name: 'Rectorado.<init>' });
  step({
    sourceRef: at(4),
    event: 'field_set',
    pieceId: 'rectorado',
    name: 'direccion',
    value: { string: 'Sarmiento 440' },
  });
  step({ sourceRef: at(4), event: 'return', name: 'Rectorado.<init>' });
  step({
    sourceRef: at(3),
    event: 'local_set',
    pieceId: 'var-rectorado',
    targetPieceId: 'rectorado',
    name: 'rectorado',
  });
  FACULTIES.forEach(([id, name, city, province], i) => {
    const line = 5 + i * 2;
    step({
      sourceRef: at(line),
      event: 'object_created',
      pieceId: `fr-${id}`,
      name: 'FacultadRegional',
    });
    step({
      sourceRef: at(line),
      event: 'call',
      pieceId: `fr-${id}`,
      name: 'FacultadRegional.<init>',
    });
    step({
      sourceRef: at(7),
      event: 'field_set',
      pieceId: `fr-${id}`,
      name: 'nombre',
      value: { string: name },
    });
    step({
      sourceRef: at(8),
      event: 'field_set',
      pieceId: `fr-${id}`,
      name: 'ciudad',
      value: { string: city },
    });
    step({
      sourceRef: at(9),
      event: 'field_set',
      pieceId: `fr-${id}`,
      name: 'provincia',
      value: { string: province },
    });
    step({ sourceRef: at(10), event: 'return', name: 'FacultadRegional.<init>' });
    step({
      sourceRef: at(line),
      event: 'local_set',
      pieceId: `var-${id}`,
      targetPieceId: `fr-${id}`,
      name: id,
    });
  });
  step({
    sourceRef: at(11),
    event: 'local_set',
    pieceId: 'var-miFacultad',
    targetPieceId: 'fr-resistencia',
    name: 'miFacultad',
  });
  FACULTIES.forEach(([id]) => {
    step({
      sourceRef: at(13),
      event: 'call',
      pieceId: `fr-${id}`,
      name: 'FacultadRegional.describir',
    });
    step({ sourceRef: at(13), event: 'return', name: 'FacultadRegional.describir' });
  });
  return {
    runId: 'demo',
    outcome: 'passed',
    progress: { passed: 3, total: 3 },
    pieces,
    log: [],
    timeline,
    stdout: '',
    classes: [rectorate(), faculty()],
  };
}
