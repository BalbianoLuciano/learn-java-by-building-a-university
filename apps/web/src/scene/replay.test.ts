import type { Piece, TimelineStep } from '@ljbu/contracts';
import { passedResult } from '../test/fixtures';
import { sceneStateAt, slotKey } from './replay';

const withGhost: Piece[] = [
  ...passedResult.pieces,
  {
    id: 'fr-ghost',
    archetype: 'regional-faculty',
    state: 'incomplete',
    built: false,
    label: 'Fantasma',
  },
];

describe('sceneStateAt', () => {
  it('shows every built piece and where each sign points at the end of the run', () => {
    const state = sceneStateAt(withGhost, passedResult.timeline);

    expect([...state.visible]).toEqual(['fr-resistencia', 'var-resistencia', 'var-miFacultad']);
    expect(state.targets.get('var-miFacultad')).toBe('fr-resistencia');
    expect(state.visible.has('fr-ghost')).toBe(false);
  });

  it('shows only what the timeline built up to the chosen step', () => {
    const pieces = withGhost;

    expect([...sceneStateAt(pieces, passedResult.timeline, 0).visible]).toEqual([]);
    expect([...sceneStateAt(pieces, passedResult.timeline, 1).visible]).toEqual(['fr-resistencia']);
    const afterAlias = sceneStateAt(pieces, passedResult.timeline, 2);
    expect(afterAlias.targets.get('var-miFacultad')).toBe('fr-resistencia');
    expect(afterAlias.visible.has('var-resistencia')).toBe(false);
  });

  it('fills a slot when its write is seen, or when its occupant exists if nothing wrote it', () => {
    const pieces: Piece[] = [
      {
        id: 'fr',
        archetype: 'regional-faculty',
        state: 'passed',
        built: true,
        label: 'FR',
        slots: {
          dean: { state: 'filled', pieceIds: ['decano'] },
          departments: { state: 'filled', pieceIds: ['dep'] },
        },
      },
      { id: 'decano', archetype: 'person', state: 'passed', built: true, label: 'Decano' },
      { id: 'dep', archetype: 'department', state: 'passed', built: true, label: 'Dep' },
    ];
    const timeline: TimelineStep[] = [
      {
        index: 0,
        sourceRef: { file: 'Main.java', line: 1 },
        event: 'object_created',
        pieceId: 'fr',
      },
      {
        index: 1,
        sourceRef: { file: 'Main.java', line: 2 },
        event: 'object_created',
        pieceId: 'decano',
      },
      {
        index: 2,
        sourceRef: { file: 'Main.java', line: 3 },
        event: 'object_created',
        pieceId: 'dep',
      },
      {
        index: 3,
        sourceRef: { file: 'Main.java', line: 4 },
        event: 'field_set',
        pieceId: 'fr',
        targetPieceId: 'decano',
        name: 'decano',
      },
    ];

    const beforeWrite = sceneStateAt(pieces, timeline, 2);
    expect(beforeWrite.filled.has(slotKey('fr', 'decano'))).toBe(false);
    expect(beforeWrite.filled.has(slotKey('fr', 'dep'))).toBe(true);
    const afterWrite = sceneStateAt(pieces, timeline, 3);
    expect(afterWrite.filled.has(slotKey('fr', 'decano'))).toBe(true);
    expect([...sceneStateAt(pieces, timeline).filled]).toEqual(['fr/decano', 'fr/dep']);
  });

  it('builds the floors of an inherited object as its constructors run, super() first', () => {
    const pieces: Piece[] = [
      {
        id: 'fr',
        archetype: 'inheritance-floors',
        state: 'passed',
        built: true,
        label: 'FR',
        floors: ['UnidadAcademica', 'FacultadRegional'],
      },
    ];
    const at = (line: number) => ({ file: 'Main.java', line });
    const timeline: TimelineStep[] = [
      { index: 0, sourceRef: at(3), event: 'object_created', pieceId: 'fr' },
      { index: 1, sourceRef: at(3), event: 'call', pieceId: 'fr', name: 'FacultadRegional.<init>' },
      { index: 2, sourceRef: at(5), event: 'call', pieceId: 'fr', name: 'UnidadAcademica.<init>' },
      { index: 3, sourceRef: at(6), event: 'return', name: 'UnidadAcademica.<init>' },
      { index: 4, sourceRef: at(5), event: 'return', name: 'FacultadRegional.<init>' },
    ];

    expect(sceneStateAt(pieces, timeline, 0).floors.get('fr')).toBe(1);
    expect(sceneStateAt(pieces, timeline, 1).floors.get('fr')).toBe(1);
    expect(sceneStateAt(pieces, timeline, 2).floors.get('fr')).toBe(2);
    expect(sceneStateAt(pieces, timeline).floors.get('fr')).toBe(2);
  });
});
