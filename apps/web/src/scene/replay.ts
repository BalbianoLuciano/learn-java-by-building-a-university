import type { Piece, TimelineStep, Value } from '@ljbu/contracts';

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
  /** The value of each attribute of each object: pieceId → name → value (DESIGN.md §B4). */
  fields: Map<string, Map<string, FieldState>>;
  /** The method running on each object at this step (its simple name; <init> for a constructor). */
  running: Map<string, string>;
  /** The object the chosen step touches, and the attribute it wrote, if any: its bubble opens. */
  focus?: { pieceId: string; field?: string };
}

export interface FieldState {
  value: Value | null;
  /** The piece the value refers to, when it is a drawn object. */
  pieceId?: string;
  /** For a collection, the pieces of its elements. */
  pieceIds?: string[];
}

/** Collection elements are written through add(), not through the field: a null value with pieces. */
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
    const fields = new Map<string, Map<string, FieldState>>();
    for (const piece of built) {
      if (piece.fields) {
        fields.set(
          piece.id,
          new Map(
            piece.fields.map((field) => [
              field.name,
              { value: field.value, pieceId: field.pieceId },
            ]),
          ),
        );
      }
    }
    return { visible, targets, filled, floors, fields, running: new Map() };
  }

  const visible = new Set<string>();
  const targets = new Map<string, string | null>();
  const filled = new Set<string>();
  const floors = new Map<string, number>();
  const written = new Set<string>();
  const constructed = new Set<string>();
  const fields = new Map<string, Map<string, FieldState>>();
  for (const piece of built) {
    // Every declared attribute starts without a value: the plaque reads "?".
    fields.set(
      piece.id,
      new Map((piece.fields ?? []).map((field) => [field.name, { value: null }])),
    );
  }
  const running = new Map<string, string>();
  const stack: { pieceId: string; method: string }[] = [];
  for (const current of timeline.slice(0, step + 1)) {
    if (current.event === 'object_created' && current.pieceId) {
      visible.add(current.pieceId);
    } else if (current.event === 'local_set' && current.pieceId) {
      visible.add(current.pieceId);
      targets.set(current.pieceId, current.targetPieceId ?? null);
    } else if (current.event === 'field_set' && current.pieceId) {
      if (current.targetPieceId) {
        written.add(slotKey(current.pieceId, current.targetPieceId));
      }
      if (current.name) {
        fields
          .get(current.pieceId)
          ?.set(current.name, { value: current.value ?? null, pieceId: current.targetPieceId });
      }
    } else if (current.event === 'call') {
      const method = current.name?.slice(current.name.lastIndexOf('.') + 1) ?? '';
      if (current.pieceId && method.endsWith('<init>')) {
        constructed.add(slotKey(current.pieceId, current.name?.slice(0, -'.<init>'.length) ?? ''));
      }
      stack.push({ pieceId: current.pieceId ?? '', method });
    } else if (current.event === 'return') {
      stack.pop();
    }
  }
  // Elements enter a collection through add(), which is no field write: an element is in
  // place as soon as it exists.
  for (const piece of built) {
    for (const field of piece.fields ?? []) {
      if (field.pieceIds) {
        const present = field.pieceIds.filter((id) => visible.has(id));
        const current = fields.get(piece.id)?.get(field.name);
        fields.get(piece.id)?.set(field.name, {
          value: current?.value ?? field.value,
          pieceIds: present.length > 0 ? present : undefined,
        });
      }
    }
  }
  // Every method still on the stack is running: the innermost one wins per object.
  for (const frame of stack) {
    if (frame.pieceId) {
      running.set(frame.pieceId, frame.method);
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
  const current = timeline[step];
  const focusId =
    current?.pieceId ?? (current?.event === 'local_set' ? current.targetPieceId : undefined);
  const focus =
    focusId && visible.has(focusId)
      ? { pieceId: focusId, field: current?.event === 'field_set' ? current.name : undefined }
      : undefined;
  return { visible, targets, filled, floors, fields, running, focus };
}
