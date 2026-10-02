import type { Piece } from '@ljbu/contracts';

/** Where a piece stands in the model (DESIGN.md §B2): 1 unit = 1 grid module. */
export interface Placement {
  id: string;
  /** Center of the island, or foot of the sign. */
  position: [number, number, number];
  /** Island footprint; signs have none. */
  island?: { width: number; depth: number };
  /** For pieces that occupy a slot of another piece. */
  owner?: string;
  /** Stable pseudo-random phase for the floating animation. */
  phase: number;
}

/** Height of the sign of a variable, where its cable starts. */
export const SIGN_TOP = 1.2;

const RING_SPACING = 3.4;
const MIN_RING_RADIUS = 3.2;
const RING_GAP = 3.2;
const SIGN_SPACING = 2.2;

function footprint(piece: Piece): { width: number; depth: number } {
  switch (piece.archetype) {
    case 'rectorate':
      return { width: 3.6, depth: 2.6 };
    case 'regional-faculty':
    case 'inheritance-floors':
      return { width: 2.4, depth: 2.4 };
    default:
      return { width: 1.8, depth: 1.8 };
  }
}

function phaseOf(id: string): number {
  let hash = 0;
  for (const character of id) {
    hash = (hash * 31 + character.charCodeAt(0)) % 1000;
  }
  return (hash / 1000) * Math.PI * 2;
}

/** Concentric rings from the inside out, each holding as many pieces as fit with the spacing. */
function ringsFor(count: number, innermost: number): { radius: number; size: number }[] {
  const rings: { radius: number; size: number }[] = [];
  let radius = innermost;
  for (let placed = 0; placed < count; radius += RING_GAP) {
    const capacity = Math.max(1, Math.floor((2 * Math.PI * radius) / RING_SPACING));
    const size = Math.min(capacity, count - placed);
    rings.push({ radius, size });
    placed += size;
  }
  // The outer ring spreads its pieces out instead of leaving a gap.
  return rings;
}

/**
 * The Rectorado in the middle, Facultades Regionales and other pieces in rings around it,
 * what a piece holds in its slots next to that piece, and the variable signs along the front.
 */
export function layoutScene(pieces: Piece[]): Map<string, Placement> {
  const placements = new Map<string, Placement>();
  const occupants = new Map<string, string>();
  for (const piece of pieces) {
    for (const slot of Object.values(piece.slots ?? {})) {
      for (const occupant of slot.pieceIds) {
        occupants.set(occupant, piece.id);
      }
    }
  }

  const centers = pieces.filter((piece) => piece.archetype === 'rectorate');
  const signs = pieces.filter((piece) => piece.archetype === 'variable-sign');
  const ring = pieces.filter(
    (piece) => !centers.includes(piece) && !signs.includes(piece) && !occupants.has(piece.id),
  );

  centers.forEach((piece, index) => {
    const x = (index - (centers.length - 1) / 2) * 4.5;
    placements.set(piece.id, {
      id: piece.id,
      position: [x, 0, 0],
      island: footprint(piece),
      phase: phaseOf(piece.id),
    });
  });

  let radius = 0;
  if (ring.length === 1 && centers.length === 0) {
    const [only] = ring;
    if (only) {
      placements.set(only.id, {
        id: only.id,
        position: [0, 0, 0],
        island: footprint(only),
        phase: phaseOf(only.id),
      });
    }
  } else if (ring.length > 0) {
    const innermost = centers.length > 0 ? MIN_RING_RADIUS + 1.2 : MIN_RING_RADIUS;
    let next = 0;
    for (const current of ringsFor(ring.length, innermost)) {
      radius = current.radius;
      for (let index = 0; index < current.size; index++) {
        const piece = ring[next++];
        if (!piece) {
          break;
        }
        const angle = -Math.PI / 2 + (index / current.size) * Math.PI * 2;
        placements.set(piece.id, {
          id: piece.id,
          position: [Math.cos(angle) * radius, 0, Math.sin(angle) * radius],
          island: footprint(piece),
          phase: phaseOf(piece.id),
        });
      }
    }
  }

  // Occupants orbit their owner, outside its island.
  const byOwner = new Map<string, string[]>();
  for (const [occupant, owner] of occupants) {
    byOwner.set(owner, [...(byOwner.get(owner) ?? []), occupant]);
  }
  for (const [owner, ids] of byOwner) {
    const home = placements.get(owner);
    if (!home) {
      continue;
    }
    ids.forEach((id, index) => {
      const angle = Math.PI / 4 + (index / Math.max(ids.length, 3)) * Math.PI * 2;
      const distance = (home.island?.width ?? 2) / 2 + 1.6;
      placements.set(id, {
        id,
        position: [
          home.position[0] + Math.cos(angle) * distance,
          0,
          home.position[2] + Math.sin(angle) * distance,
        ],
        island: { width: 1.4, depth: 1.4 },
        owner,
        phase: phaseOf(id),
      });
    });
  }

  // Signs stand in a row in front of everything, outside the islands.
  const front = Math.max(radius, 1.5) + 2.4;
  signs.forEach((piece, index) => {
    const x = (index - (signs.length - 1) / 2) * SIGN_SPACING;
    placements.set(piece.id, { id: piece.id, position: [x, 0, front], phase: phaseOf(piece.id) });
  });

  return placements;
}

/** Center and radius of a sphere around every placement, to frame the camera. */
export function boundsOf(placements: Iterable<Placement>): {
  center: [number, number, number];
  radius: number;
} {
  let minX = Infinity;
  let maxX = -Infinity;
  let minZ = Infinity;
  let maxZ = -Infinity;
  for (const placement of placements) {
    const half = (placement.island?.width ?? 1) / 2;
    minX = Math.min(minX, placement.position[0] - half);
    maxX = Math.max(maxX, placement.position[0] + half);
    minZ = Math.min(minZ, placement.position[2] - half);
    maxZ = Math.max(maxZ, placement.position[2] + half);
  }
  if (minX === Infinity) {
    return { center: [0, 0, 0], radius: 3 };
  }
  const width = maxX - minX;
  const depth = maxZ - minZ;
  return {
    center: [(minX + maxX) / 2, 0.8, (minZ + maxZ) / 2],
    radius: Math.max(2.6, Math.hypot(width, depth) / 2 + 0.6),
  };
}
