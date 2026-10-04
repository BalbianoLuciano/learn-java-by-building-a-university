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
  /** Every other attached island raises its nameplate, so neighbours do not overlap. */
  stagger?: boolean;
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

/** Where the blueprint of a class stands: on the drafting board, over the column of its instances. */
export interface BlueprintPlacement {
  name: string;
  position: [number, number, number];
  height: number;
  /** Footprint, so the camera frames the board too. */
  island: { width: number; depth: number };
}

/** The long table at the back every blueprint stands on. */
export interface Board {
  x: number;
  z: number;
  width: number;
  depth: number;
}

export interface SceneLayout {
  placements: Map<string, Placement>;
  bridges: Bridge[];
  blueprints: Map<string, BlueprintPlacement>;
  board: Board | null;
}

export const BLUEPRINT_WIDTH = 2.4;
export const BLUEPRINT_HEIGHT = 2.2;
const BOARD_DEPTH = 1.6;
/** Between the board and the first row of instances. */
const BOARD_GAP = 1.6;
const COLUMN_GAP = 1.6;
const ROW_GAP = 1.4;
const ATTACHED_GAP = 0.9;
const ATTACHED_ISLAND = { width: 1.3, depth: 1.3 };
const NULL_TAG_SPACING = 1.8;
const FRONT_GAP = 1.8;

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

/**
 * A grid (DESIGN.md §B2): one column per class, its blueprint on the drafting board at the
 * back and its instances in a row in front of it, oldest first. What an instance holds stands
 * on small islands attached to its right. Variables hang as tags from the pieces they point
 * to; null ones lie on the ground along the front.
 */
export function layoutScene(pieces: Piece[], classes: ClassInfo[] = []): SceneLayout {
  const placements = new Map<string, Placement>();
  const bridges: Bridge[] = [];
  const blueprints = new Map<string, BlueprintPlacement>();
  const contained = containedBy(pieces);
  const held = new Set([...contained.values()].flat());
  const byId = new Map(pieces.map((piece) => [piece.id, piece]));

  const descendants = (id: string): number =>
    (contained.get(id) ?? []).reduce((count, child) => count + 1 + descendants(child), 0);
  /** Width of a piece with everything attached to its right. */
  const extentOf = (piece: Piece): number =>
    footprint(piece).width + descendants(piece.id) * (ATTACHED_ISLAND.width + ATTACHED_GAP);

  const signs = pieces.filter((piece) => piece.archetype === 'variable-sign');
  const freeStanding = pieces.filter(
    (piece) => piece.archetype !== 'variable-sign' && !held.has(piece.id),
  );

  // Columns: the classes of the program in order, plus one for anything of another type.
  const named = classes.filter((info) => info.kind !== 'interface').map((info) => info.name);
  const seals = classes.filter((info) => info.kind === 'interface').map((info) => info.name);
  const columns: { name: string; instances: Piece[] }[] = named.map((name) => ({
    name,
    instances: freeStanding.filter((piece) => piece.type === name),
  }));
  const orphans = freeStanding.filter((piece) => !piece.type || !named.includes(piece.type));
  if (orphans.length > 0) {
    columns.push({ name: '', instances: orphans });
  }
  for (const name of seals) {
    columns.push({ name, instances: [] });
  }

  const widths = columns.map((column) =>
    Math.max(BLUEPRINT_WIDTH, ...column.instances.map(extentOf)),
  );
  const total = widths.reduce((sum, width) => sum + width, 0) + (columns.length - 1) * COLUMN_GAP;
  const boardZ = -(BOARD_DEPTH / 2 + BOARD_GAP);

  /** Places what a piece holds on small islands in a row to its right, recursively. */
  const attach = (owner: Piece, edge: number, z: number, depth = 0): number => {
    for (const id of contained.get(owner.id) ?? []) {
      const child = byId.get(id);
      if (!child) {
        continue;
      }
      const cx = edge + ATTACHED_GAP + ATTACHED_ISLAND.width / 2;
      placements.set(id, {
        id,
        position: [cx, 0, z],
        island: ATTACHED_ISLAND,
        owner: owner.id,
        stagger: depth % 2 === 1,
        phase: phaseOf(id),
      });
      bridges.push({ from: [edge, 0, z], to: [cx - ATTACHED_ISLAND.width / 2, 0, z] });
      edge = attach(child, cx + ATTACHED_ISLAND.width / 2, z, depth + 1);
    }
    return edge;
  };

  let x = -total / 2;
  let front = 0;
  columns.forEach((column, index) => {
    const width = widths[index] ?? BLUEPRINT_WIDTH;
    const center = x + width / 2;
    if (column.name) {
      blueprints.set(column.name, {
        name: column.name,
        position: [center, 0, boardZ],
        height: BLUEPRINT_HEIGHT,
        island: { width: BLUEPRINT_WIDTH, depth: BOARD_DEPTH },
      });
    }
    // Instances from the board forward, each one (with what it holds) centered in the column.
    let z = 0;
    for (const piece of column.instances) {
      const own = footprint(piece);
      const px = x + (width - extentOf(piece)) / 2 + own.width / 2;
      const pz = z + own.depth / 2;
      placements.set(piece.id, {
        id: piece.id,
        position: [px, 0, pz],
        island: own,
        phase: phaseOf(piece.id),
      });
      attach(piece, px + own.width / 2, pz);
      z += own.depth + ROW_GAP;
      front = Math.max(front, pz + own.depth / 2);
    }
    x += width + COLUMN_GAP;
  });

  // Variables: tags on the building they point to; null ones lie on the ground at the front.
  const tagsOf = new Map<string, number>();
  const nulls = signs.filter((sign) => !sign.target || !placements.has(sign.target));
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
        position: [(index - (nulls.length - 1) / 2) * NULL_TAG_SPACING, 0, front + FRONT_GAP],
        phase: phaseOf(sign.id),
      });
    }
  }

  const board: Board | null =
    columns.length > 0 ? { x: 0, z: boardZ, width: total + 1.2, depth: BOARD_DEPTH } : null;
  return { placements, bridges, blueprints, board };
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
