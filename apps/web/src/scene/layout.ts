import type { ClassInfo, Piece } from '@ljbu/contracts';

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

const RING_SPACING = 4.6;
const MIN_RING_RADIUS = 3.6;
const RING_GAP = 3.8;
const SIGN_SPACING = 2.8;

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
      const distance = (home.island?.width ?? 2) / 2 + 2.4;
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
  const front = Math.max(radius, 1.5) + 1.8;
  signs.forEach((piece, index) => {
    const x = (index - (signs.length - 1) / 2) * SIGN_SPACING;
    placements.set(piece.id, { id: piece.id, position: [x, 0, front], phase: phaseOf(piece.id) });
  });

  return placements;
}

/** Center and radius of a sphere around every placement, to frame the camera. */
export function boundsOf(placements: Iterable<Pick<Placement, 'position' | 'island'>>): {
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

/** Where the blueprint of a class stands (DESIGN.md §B4): a row at the back, stacked by inheritance. */
export interface BlueprintPlacement {
  name: string;
  position: [number, number, number];
  /** Height of the panel, which grows with its attributes and methods. */
  height: number;
  /** Footprint, so the camera frames the row of blueprints too. */
  island: { width: number; depth: number };
}

export const BLUEPRINT_WIDTH = 3.6;
const BLUEPRINT_SPACING = 5.2;
const BLUEPRINT_GAP = 0.3;
/** 20px per row at BASE_ZOOM (overlay.tsx). */
const BLUEPRINT_ROW_HEIGHT = 0.5;

export function blueprintHeight(info: ClassInfo): number {
  const rows = info.fields.length + info.methods.length + info.constructors.length;
  return 1.0 + Math.min(8, Math.max(1, rows)) * BLUEPRINT_ROW_HEIGHT;
}

/**
 * Classes in a row behind everything else, interfaces (seals) at the right end. A subclass
 * stands on top of its superclass, in the same column.
 */
export function layoutBlueprints(
  classes: ClassInfo[],
  back: number,
): Map<string, BlueprintPlacement> {
  const placements = new Map<string, BlueprintPlacement>();
  const byName = new Map(classes.map((info) => [info.name, info]));
  const roots = classes.filter(
    (info) => info.kind !== 'interface' && (!info.superclass || !byName.has(info.superclass)),
  );
  const seals = classes.filter((info) => info.kind === 'interface');
  const columns = roots.length + seals.length;
  let column = 0;
  const place = (info: ClassInfo, x: number, y: number) => {
    const height = blueprintHeight(info);
    placements.set(info.name, {
      name: info.name,
      position: [x, y, back],
      height,
      island: { width: BLUEPRINT_WIDTH, depth: 0.4 },
    });
    classes
      .filter((child) => child.superclass === info.name)
      .forEach((child, index) => {
        place(child, x + index * BLUEPRINT_SPACING, y + height + BLUEPRINT_GAP);
      });
  };
  for (const root of roots) {
    place(root, (column - (columns - 1) / 2) * BLUEPRINT_SPACING, 0);
    column++;
  }
  for (const seal of seals) {
    placements.set(seal.name, {
      name: seal.name,
      position: [(column - (columns - 1) / 2) * BLUEPRINT_SPACING, 0, back],
      height: blueprintHeight(seal),
      island: { width: BLUEPRINT_WIDTH, depth: 0.4 },
    });
    column++;
  }
  return placements;
}
