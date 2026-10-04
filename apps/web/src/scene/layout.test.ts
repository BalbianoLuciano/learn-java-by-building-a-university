import type { ClassInfo, Piece } from '@ljbu/contracts';
import { boundsOf, containedBy, layoutScene } from './layout';

type Archetype = Piece['archetype'];

function piece(id: string, archetype: Archetype, extra: Partial<Piece> = {}): Piece {
  return { id, archetype, state: 'passed', built: true, label: id, type: 'T', ...extra };
}

function clazz(name: string, extra: Partial<ClassInfo> = {}): ClassInfo {
  return {
    name,
    kind: 'class',
    abstract: false,
    superclass: null,
    interfaces: [],
    file: `${name}.java`,
    line: 1,
    fields: [],
    constructors: [],
    methods: [],
    ...extra,
  };
}

describe('layoutScene', () => {
  it('puts the blueprint of a class on the board, with its instances in a row in front of it', () => {
    const { placements, blueprints, board } = layoutScene(
      [piece('a', 'regional-faculty'), piece('b', 'regional-faculty')],
      [clazz('T')],
    );

    const blueprint = blueprints.get('T');
    expect(blueprint?.position[2]).toBeLessThan(0);
    expect(board?.z).toBe(blueprint?.position[2]);
    const a = placements.get('a');
    const b = placements.get('b');
    expect(a?.position[0]).toBe(b?.position[0]);
    expect(a?.position[0]).toBeCloseTo(blueprint?.position[0] ?? NaN, 5);
    expect((b?.position[2] ?? 0) - (a?.position[2] ?? 0)).toBeCloseTo(2.4 + 1.4, 5);
  });

  it('gives every class a column, left to right, in the order of the program', () => {
    const { blueprints, placements } = layoutScene(
      [piece('fr', 'regional-faculty', { type: 'FR' }), piece('p', 'person', { type: 'P' })],
      [clazz('FR'), clazz('P'), clazz('Vacia')],
    );

    const xs = ['FR', 'P', 'Vacia'].map((name) => blueprints.get(name)?.position[0] ?? NaN);
    expect(xs[0]).toBeLessThan(xs[1] ?? NaN);
    expect(xs[1]).toBeLessThan(xs[2] ?? NaN);
    expect(placements.get('p')?.position[0]).toBeCloseTo(xs[1] ?? NaN, 5);
  });

  it('puts what a piece holds on small islands attached to its right, with a walkway', () => {
    const { placements, bridges } = layoutScene(
      [
        piece('fr', 'regional-faculty', {
          fields: [{ name: 'decano', value: { ref: 'o2' }, pieceId: 'decano' }],
        }),
        piece('decano', 'person'),
      ],
      [clazz('T')],
    );

    const owner = placements.get('fr');
    const held = placements.get('decano');
    expect(held?.owner).toBe('fr');
    expect(held?.position[2]).toBe(owner?.position[2]);
    expect((held?.position[0] ?? 0) - (owner?.position[0] ?? 0)).toBeCloseTo(
      2.4 / 2 + 0.9 + 1.3 / 2,
      5,
    );
    expect(bridges).toHaveLength(1);
  });

  it('hangs a variable from the piece it points to, and lays a null one at the front', () => {
    const { placements } = layoutScene(
      [
        piece('fr', 'regional-faculty'),
        piece('var-a', 'variable-sign', { target: 'fr', type: undefined }),
        piece('var-b', 'variable-sign', { target: 'fr', type: undefined }),
        piece('var-nada', 'variable-sign', { type: undefined }),
      ],
      [clazz('T')],
    );

    expect(placements.get('var-a')).toMatchObject({ hangsFrom: 'fr', tagIndex: 0 });
    expect(placements.get('var-b')).toMatchObject({ hangsFrom: 'fr', tagIndex: 1 });
    const nothing = placements.get('var-nada');
    expect(nothing?.hangsFrom).toBeUndefined();
    expect(nothing?.position[2]).toBeGreaterThan(placements.get('fr')?.position[2] ?? 0);
    expect(nothing?.island).toBeUndefined();
  });

  it('keeps pieces of an unknown type in a column of their own, and seals at the right end', () => {
    const { placements, blueprints } = layoutScene(
      [piece('x', 'generic-block', { type: undefined })],
      [clazz('T'), clazz('I', { kind: 'interface' })],
    );

    expect(placements.get('x')?.position[0]).toBeGreaterThan(
      blueprints.get('T')?.position[0] ?? NaN,
    );
    expect(blueprints.get('I')?.position[0]).toBeGreaterThan(
      placements.get('x')?.position[0] ?? NaN,
    );
  });
});

describe('containedBy', () => {
  it('reads composition from the fields, each piece held by one owner only', () => {
    const contained = containedBy([
      piece('fr', 'regional-faculty', {
        fields: [
          { name: 'decano', value: { ref: 'o2' }, pieceId: 'decano' },
          { name: 'departamentos', value: { ref: 'o3' }, pieceIds: ['dep-1', 'dep-2'] },
        ],
      }),
      piece('otra', 'regional-faculty', {
        fields: [{ name: 'decano', value: { ref: 'o2' }, pieceId: 'decano' }],
      }),
      piece('decano', 'person'),
      piece('dep-1', 'department'),
      piece('dep-2', 'department'),
    ]);

    expect(contained.get('fr')).toEqual(['decano', 'dep-1', 'dep-2']);
    expect(contained.has('otra')).toBe(false);
  });
});

describe('boundsOf', () => {
  it('frames nothing as a small sphere at the origin', () => {
    expect(boundsOf([])).toEqual({ center: [0, 0, 0], radius: 3 });
  });

  it('never gets smaller than the minimum', () => {
    const { placements } = layoutScene([piece('rectorado', 'rectorate')], [clazz('T')]);

    expect(boundsOf(placements.values()).radius).toBeGreaterThanOrEqual(2.6);
  });
});
