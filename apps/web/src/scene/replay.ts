import type { Piece, TimelineStep } from '@ljbu/contracts';

/** What the model shows at one moment of the execution. */
export interface SceneState {
  /** Built pieces that already exist at this moment. */
  visible: Set<string>;
  /** Where each variable sign points; null is a dangling cable. */
  targets: Map<string, string | null>;
  /** Slot occupants already in place: "pieceId/occupantId". */
  filled: Set<string>;
  /**
   * For pieces drawn with inheritance floors: how many floors exist, from the bottom. A floor
   * is built once the constructor of its class ran for the object (super() first).
   */
  floors: Map<string, number>;
}

export function slotKey(pieceId: string, occupantId: string): string {
  return `${pieceId}/${occupantId}`;
}

/**
 * The state at the end of the run, or after the given step of the timeline. Pieces the brief
 * asks for and the code did not build are never visible: they are drawn as silhouettes.
 */
export function sceneStateAt(pieces: Piece[], timeline: TimelineStep[], step?: number): SceneState {
  const built = pieces.filter((piece) => piece.built);
  if (step === undefined) {
    const visible = new Set(built.map((piece) => piece.id));
    const targets = new Map<string, string | null>();
    const filled = new Set<string>();
    const floors = new Map<string, number>();
    for (const piece of built) {
      if (piece.archetype === 'variable-sign') {
        targets.set(piece.id, piece.target ?? null);
      }
      if (piece.floors) {
        floors.set(piece.id, piece.floors.length);
      }
      for (const slot of Object.values(piece.slots ?? {})) {
        for (const occupant of slot.pieceIds) {
          filled.add(slotKey(piece.id, occupant));
        }
      }
    }
    return { visible, targets, filled, floors };
  }

  const visible = new Set<string>();
  const targets = new Map<string, string | null>();
  const filled = new Set<string>();
  const floors = new Map<string, number>();
  const written = new Set<string>();
  const constructed = new Set<string>();
  for (const current of timeline.slice(0, step + 1)) {
    if (current.event === 'object_created' && current.pieceId) {
      visible.add(current.pieceId);
    } else if (current.event === 'local_set' && current.pieceId) {
      visible.add(current.pieceId);
      targets.set(current.pieceId, current.targetPieceId ?? null);
    } else if (current.event === 'field_set' && current.pieceId && current.targetPieceId) {
      written.add(slotKey(current.pieceId, current.targetPieceId));
    } else if (current.event === 'call' && current.pieceId && current.name?.endsWith('.<init>')) {
      constructed.add(slotKey(current.pieceId, current.name.slice(0, -'.<init>'.length)));
    }
  }
  for (const piece of built) {
    if (piece.floors && visible.has(piece.id)) {
      // The floors are listed from the top of the hierarchy down: super() runs first.
      const ran = piece.floors.filter((name) => constructed.has(slotKey(piece.id, name))).length;
      floors.set(piece.id, Math.max(1, ran));
    }
  }
  // A slot is in place once its write was seen; a slot filled through a collection leaves no
  // write, so it is in place as soon as its occupant exists.
  const everWritten = new Set(
    timeline
      .filter(
        (current) => current.event === 'field_set' && current.pieceId && current.targetPieceId,
      )
      .map((current) => slotKey(current.pieceId ?? '', current.targetPieceId ?? '')),
  );
  for (const piece of built) {
    for (const slot of Object.values(piece.slots ?? {})) {
      for (const occupant of slot.pieceIds) {
        const key = slotKey(piece.id, occupant);
        if (written.has(key) || (!everWritten.has(key) && visible.has(occupant))) {
          filled.add(key);
        }
      }
    }
  }
  return { visible, targets, filled, floors };
}
