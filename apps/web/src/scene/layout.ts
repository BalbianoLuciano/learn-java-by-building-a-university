import type { ClassInfo, Piece } from '@ljbu/contracts';

/** Where a piece stands in the model (DESIGN.md §B2): 1 unit = 1 grid module. */
export interface Placement {
  id: string;
  /** Center of the island; for a variable, the building it hangs from or its spot on the ground. */
  position: [number, number, number];
  /** Island footprint; variables have none. */
  island?: { width: number; depth: number };
  /** For a piece that lives on an island attached to another piece's island. */
  owner?: string;
  /** For a variable: the piece it hangs from, and its place among the tags of that piece. */
  hangsFrom?: string;
  tagIndex?: number;
  /** Stable pseudo-random phase for the floating animation. */
  phase: number;
}

/** A short walkway between the island of an owner and the island of what it holds. */
export interface Bridge {
  from: [number, number, number];
  to: [number, number, number];
}

export interface SceneLayout {
  placements: Map<string, Placement>;
  bridges: Bridge[];
}

const RING_SPACING_GAP = 1.6;
const MIN_RING_RADIUS = 3.6;
const RING_GAP = 1.4;
const ATTACHED_GAP = 0.5;
const ATTACHED_ISLAND = { width: 1.3, depth: 1.3 };
const NULL_TAG_SPACING = 1.6;
const ROW_LIMIT = 4;
const ROW_GAP = 1.2;

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

/** The pieces each piece holds through its fields (composition), in field order. */
export function containedBy(pieces: Piece[]): Map<string, string[]> {
  const ids = new Set(pieces.map((piece) => piece.id));
  const result = new Map<string, string[]>();
  const taken = new Set<string>();
  for (const piece of pieces) {
    const held: string[] = [];
    for (const field of piece.fields ?? []) {
      for (const id of field.pieceId ? [field.pieceId] : (field.pieceIds ?? [])) {
        if (ids.has(id) && id !== piece.id && !taken.has(id)) {
          taken.add(id);
          held.push(id);
        }
      }
    }
    if (held.length === 0) {
      // Pieces that were not run yet, or old results: the slots say the same.
      for (const slot of Object.values(piece.slots ?? {})) {
        for (const id of slot.pieceIds) {
          if (ids.has(id) && id !== piece.id && !taken.has(id)) {
            taken.add(id);
            held.push(id);
          }
        }
      }
    }
    if (held.length > 0) {
      result.set(piece.id, held);
    }
  }
  return result;
}

/** Concentric rings from the inside out, each holding as many pieces as fit with the spacing. */
function ringsFor(
  count: number,
  innermost: number,
  spacing: number,
): { radius: number; size: number }[] {
  const rings: { radius: number; size: number }[] = [];
  let radius = innermost;
  for (let placed = 0; placed < count; radius += spacing + RING_GAP) {
    const capacity = Math.max(1, Math.floor((2 * Math.PI * radius) / spacing));
    const size = Math.min(capacity, count - placed);
    rings.push({ radius, size });
    placed += size;
  }
  return rings;
}

/**
 * The Rectorado in the middle, Facultades Regionales and other free-standing pieces in rings
 * around it, what a piece holds on islands attached to its own, and the variables hanging as
 * tags from the pieces they point to (DESIGN.md §B4) or lying at the front when they are null.
 */
export function layoutScene(pieces: Piece[]): SceneLayout {
  const placements = new Map<string, Placement>();
  const bridges: Bridge[] = [];
  const contained = containedBy(pieces);
  const owners = new Map<string, string>();
  for (const [owner, held] of contained) {
    for (const id of held) {
      owners.set(id, owner);
    }
  }
  const byId = new Map(pieces.map((piece) => [piece.id, piece]));

  /** Width of a piece with everything attached to its right. */
  const extentOf = (piece: Piece): number => footprint(piece).width;

  const signs = pieces.filter((piece) => piece.archetype === 'variable-sign');
  const centers = pieces.filter(
    (piece) => piece.archetype === 'rectorate' && !owners.has(piece.id),
  );
  const ring = pieces.filter(
    (piece) =>
      piece.archetype !== 'variable-sign' && !centers.includes(piece) && !owners.has(piece.id),
  );

  /** Places what a piece holds on small islands behind its own, one after the other. */
  const attach = (owner: Piece, x: number, edge: number): number => {
    for (const id of contained.get(owner.id) ?? []) {
      const child = byId.get(id);
      if (!child) {
        continue;
      }
      const cz = edge - ATTACHED_GAP - ATTACHED_ISLAND.depth / 2;
      placements.set(id, {
        id,
        position: [x, 0, cz],
        island: ATTACHED_ISLAND,
        owner: owner.id,
        phase: phaseOf(id),
      });
      bridges.push({ from: [x, 0, edge], to: [x, 0, cz + ATTACHED_ISLAND.depth / 2] });
      edge = attach(child, x, cz - ATTACHED_ISLAND.depth / 2);
    }
    return edge;
  };

  const placeWithAttachments = (piece: Piece, x: number, z: number) => {
    const own = footprint(piece);
    placements.set(piece.id, {
      id: piece.id,
      position: [x, 0, z],
      island: own,
      phase: phaseOf(piece.id),
    });
    attach(piece, x, z - own.depth / 2);
  };

  centers.forEach((piece, index) => {
    const x = (index - (centers.length - 1) / 2) * 5.5;
    placeWithAttachments(piece, x, 0);
  });

  let radius = 0;
  if (ring.length <= ROW_LIMIT && centers.length === 0) {
    // A few pieces read better side by side than around an empty middle.
    const total =
      ring.reduce((sum, piece) => sum + extentOf(piece), 0) + (ring.length - 1) * ROW_GAP;
    let x = -total / 2;
    for (const piece of ring) {
      placeWithAttachments(piece, x + footprint(piece).width / 2, 0);
      x += extentOf(piece) + ROW_GAP;
    }
  } else if (ring.length > 0) {
    const spacing = Math.max(...ring.map(extentOf)) + RING_SPACING_GAP;
    const innermost = centers.length > 0 ? MIN_RING_RADIUS + 1.8 : MIN_RING_RADIUS;
    let next = 0;
    for (const current of ringsFor(ring.length, innermost, spacing)) {
      radius = current.radius;
      for (let index = 0; index < current.size; index++) {
        const piece = ring[next++];
        if (!piece) {
          break;
        }
        const angle = -Math.PI / 2 + (index / current.size) * Math.PI * 2;
        placeWithAttachments(piece, Math.cos(angle) * radius, Math.sin(angle) * radius);
      }
    }
  }

  // Variables: tags on the building they point to; null ones lie on the ground at the front.
  const tagsOf = new Map<string, number>();
  const nulls = signs.filter((sign) => !sign.target || !placements.has(sign.target));
  const front = Math.max(radius, 1.5) + 2.2;
  for (const sign of signs) {
    const target = sign.target && placements.has(sign.target) ? sign.target : undefined;
    if (target) {
      const home = placements.get(target);
      const index = tagsOf.get(target) ?? 0;
      tagsOf.set(target, index + 1);
      placements.set(sign.id, {
        id: sign.id,
        position: home?.position ?? [0, 0, 0],
        hangsFrom: target,
        tagIndex: index,
        phase: phaseOf(sign.id),
      });
    } else {
      const index = nulls.indexOf(sign);
      placements.set(sign.id, {
        id: sign.id,
        position: [(index - (nulls.length - 1) / 2) * NULL_TAG_SPACING, 0, front],
        phase: phaseOf(sign.id),
      });
    }
  }

  return { placements, bridges };
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
  /** Height of the panel. */
  height: number;
  /** Footprint, so the camera frames the row of blueprints too. */
  island: { width: number; depth: number };
}

export const BLUEPRINT_WIDTH = 2.4;
export const BLUEPRINT_HEIGHT = 2.2;
const BLUEPRINT_SPACING = 3.4;
const BLUEPRINT_GAP = 0.3;

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
    placements.set(info.name, {
      name: info.name,
      position: [x, y, back],
      height: BLUEPRINT_HEIGHT,
      island: { width: BLUEPRINT_WIDTH, depth: 0.4 },
    });
    classes
      .filter((child) => child.superclass === info.name)
      .forEach((child, index) => {
        place(child, x + index * BLUEPRINT_SPACING, y + BLUEPRINT_HEIGHT + BLUEPRINT_GAP);
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
      height: BLUEPRINT_HEIGHT,
      island: { width: BLUEPRINT_WIDTH, depth: 0.4 },
    });
    column++;
  }
  return placements;
}
