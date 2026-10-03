import type { Piece } from '@ljbu/contracts';

type Archetype = Piece['archetype'];
import { boundsOf, layoutScene } from './layout';

function piece(id: string, archetype: Archetype, extra: Partial<Piece> = {}): Piece {
  return { id, archetype, state: 'passed', built: true, label: id, ...extra };
}

describe('layoutScene', () => {
  it('puts a lone piece in the middle', () => {
    const placements = layoutScene([piece('fr', 'regional-faculty')]);

    expect(placements.get('fr')?.position).toEqual([0, 0, 0]);
  });

  it('keeps the Rectorado in the middle and the faculties in a ring around it', () => {
    const faculties = Array.from({ length: 6 }, (_, i) =>
      piece(`fr-${String(i)}`, 'regional-faculty'),
    );
    const placements = layoutScene([piece('rectorado', 'rectorate'), ...faculties]);

    expect(placements.get('rectorado')?.position).toEqual([0, 0, 0]);
    for (const faculty of faculties) {
      const [x, , z] = placements.get(faculty.id)?.position ?? [0, 0, 0];
      expect(Math.hypot(x, z)).toBeCloseTo(4.8, 5);
    }
  });

  it('adds rings when there are many pieces, so islands never touch', () => {
    const many = Array.from({ length: 150 }, (_, i) =>
      piece(`fr-${String(i)}`, 'regional-faculty'),
    );
    const placements = layoutScene(many);
    const positions = many.map((p) => {
      const [x, , z] = placements.get(p.id)?.position ?? [0, 0, 0];
      return { x, z };
    });

    const radii = new Set(positions.map(({ x, z }) => Math.round(Math.hypot(x, z) * 100) / 100));
    expect(radii.size).toBeGreaterThan(3);
    expect(Math.max(...radii)).toBeLessThan(40);
    for (const a of positions) {
      for (const b of positions) {
        if (a !== b) {
          expect(Math.hypot(a.x - b.x, a.z - b.z)).toBeGreaterThanOrEqual(2.4);
        }
      }
    }
  });

  it('places what a piece holds next to that piece, not in the ring', () => {
    const placements = layoutScene([
      piece('fr', 'regional-faculty', {
        slots: { dean: { state: 'filled', pieceIds: ['decano'] } },
      }),
      piece('decano', 'person'),
    ]);

    const owner = placements.get('fr');
    const occupant = placements.get('decano');
    expect(owner?.position).toEqual([0, 0, 0]);
    expect(occupant?.owner).toBe('fr');
    expect(Math.hypot(occupant?.position[0] ?? 0, occupant?.position[2] ?? 0)).toBeCloseTo(3.6, 5);
  });

  it('lines the variable signs up in front, with a stable phase per piece', () => {
    const placements = layoutScene([
      piece('fr', 'regional-faculty'),
      piece('var-a', 'variable-sign', { target: 'fr' }),
      piece('var-b', 'variable-sign'),
    ]);

    expect(placements.get('var-a')?.position).toEqual([-1.4, 0, 3.3]);
    expect(placements.get('var-b')?.position).toEqual([1.4, 0, 3.3]);
    expect(placements.get('var-a')?.island).toBeUndefined();
    expect(placements.get('fr')?.phase).toBe(
      layoutScene([piece('fr', 'rectorate')]).get('fr')?.phase,
    );
  });
});

describe('boundsOf', () => {
  it('frames nothing as a small sphere at the origin', () => {
    expect(boundsOf([])).toEqual({ center: [0, 0, 0], radius: 3 });
  });

  it('centers on every placement and never gets smaller than the minimum', () => {
    const placements = layoutScene([
      piece('rectorado', 'rectorate'),
      piece('var-a', 'variable-sign'),
    ]);

    const { center, radius } = boundsOf(placements.values());
    expect(center[0]).toBe(0);
    expect(center[2]).toBeGreaterThan(0);
    expect(radius).toBeGreaterThanOrEqual(3);
  });
});
