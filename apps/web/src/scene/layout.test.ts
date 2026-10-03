import type { Piece } from '@ljbu/contracts';
import { boundsOf, containedBy, layoutScene } from './layout';

type Archetype = Piece['archetype'];

function piece(id: string, archetype: Archetype, extra: Partial<Piece> = {}): Piece {
  return { id, archetype, state: 'passed', built: true, label: id, type: 'T', ...extra };
}

describe('layoutScene', () => {
  it('puts a lone piece in the middle, and a few side by side', () => {
    const { placements } = layoutScene([piece('fr', 'regional-faculty')]);
    expect(placements.get('fr')?.position.map((v) => v + 0)).toEqual([0, 0, 0]);

    const row = layoutScene([piece('a', 'regional-faculty'), piece('b', 'regional-faculty')]);
    expect(row.placements.get('a')?.position[2]).toBe(0);
    expect(row.placements.get('b')?.position[2]).toBe(0);
    expect(
      (row.placements.get('b')?.position[0] ?? 0) - (row.placements.get('a')?.position[0] ?? 0),
    ).toBeCloseTo(3.6, 5);
  });

  it('keeps the Rectorado in the middle and the faculties in a ring around it', () => {
    const faculties = Array.from({ length: 6 }, (_, i) =>
      piece(`fr-${String(i)}`, 'regional-faculty'),
    );
    const { placements } = layoutScene([piece('rectorado', 'rectorate'), ...faculties]);

    expect(placements.get('rectorado')?.position).toEqual([0, 0, 0]);
    for (const faculty of faculties) {
      const [x, , z] = placements.get(faculty.id)?.position ?? [0, 0, 0];
      expect(Math.hypot(x, z)).toBeCloseTo(5.4, 5);
    }
  });

  it('adds rings when there are many pieces, so islands never touch', () => {
    const many = Array.from({ length: 150 }, (_, i) =>
      piece(`fr-${String(i)}`, 'regional-faculty'),
    );
    const { placements } = layoutScene(many);
    const positions = many.map((p) => {
      const [x, , z] = placements.get(p.id)?.position ?? [0, 0, 0];
      return { x, z };
    });

    const radii = new Set(positions.map(({ x, z }) => Math.round(Math.hypot(x, z) * 100) / 100));
    expect(radii.size).toBeGreaterThan(3);
    for (const a of positions) {
      for (const b of positions) {
        if (a !== b) {
          expect(Math.hypot(a.x - b.x, a.z - b.z)).toBeGreaterThanOrEqual(2.4);
        }
      }
    }
  });

  it('puts what a piece holds on small islands attached to its own, with a walkway', () => {
    const { placements, bridges } = layoutScene([
      piece('fr', 'regional-faculty', {
        fields: [{ name: 'decano', value: { ref: 'o2' }, pieceId: 'decano' }],
      }),
      piece('decano', 'person'),
    ]);

    const owner = placements.get('fr');
    const held = placements.get('decano');
    expect(held?.owner).toBe('fr');
    expect(held?.position[0]).toBe(owner?.position[0]);
    expect((owner?.position[2] ?? 0) - (held?.position[2] ?? 0)).toBeCloseTo(
      2.4 / 2 + 0.5 + 1.3 / 2,
      5,
    );
    expect(bridges).toHaveLength(1);
  });

  it('hangs a variable from the piece it points to, and lays a null one at the front', () => {
    const { placements } = layoutScene([
      piece('fr', 'regional-faculty'),
      piece('var-a', 'variable-sign', { target: 'fr', type: undefined }),
      piece('var-b', 'variable-sign', { target: 'fr', type: undefined }),
      piece('var-nada', 'variable-sign', { type: undefined }),
    ]);

    expect(placements.get('var-a')).toMatchObject({ hangsFrom: 'fr', tagIndex: 0 });
    expect(placements.get('var-b')).toMatchObject({ hangsFrom: 'fr', tagIndex: 1 });
    const nothing = placements.get('var-nada');
    expect(nothing?.hangsFrom).toBeUndefined();
    expect(nothing?.position[2]).toBeGreaterThan(0);
    expect(nothing?.island).toBeUndefined();
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

  it('centers on every placement and never gets smaller than the minimum', () => {
    const { placements } = layoutScene([
      piece('rectorado', 'rectorate'),
      piece('var-a', 'variable-sign', { type: undefined }),
    ]);

    const { center, radius } = boundsOf(placements.values());
    expect(center[0]).toBe(0);
    expect(center[2]).toBeGreaterThan(0);
    expect(radius).toBeGreaterThanOrEqual(2.6);
  });
});
