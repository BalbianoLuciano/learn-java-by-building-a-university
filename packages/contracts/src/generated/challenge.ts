// Generated from challenge.schema.json by scripts/generate.mjs. Do not edit.

export type CheckId = string;
export type JavaFileName = string;
/**
 * Scene archetype from DESIGN.md §B4.
 */
export type Archetype =
  | 'rectorate'
  | 'regional-faculty'
  | 'department'
  | 'career'
  | 'person'
  | 'inheritance-floors'
  | 'slot'
  | 'variable-sign'
  | 'reference-link'
  | 'interface-badge'
  | 'generic-block';
/**
 * Check types of docs/ARCHITECTURE.md §6. Adding one requires an ADR.
 */
export type CheckType =
  | 'class_exists'
  | 'extends'
  | 'implements'
  | 'is_abstract'
  | 'field'
  | 'constructor'
  | 'method'
  | 'object_count'
  | 'object_field'
  | 'shared_reference'
  | 'call_dispatch'
  | 'stdout_contains'
  | 'no_exception'
  | 'throws';

/**
 * A challenge.yaml file (docs/specs/challenge-format.md).
 */
export interface Challenge {
  /**
   * Unique and stable; never reused.
   */
  id: string;
  module: string;
  order: number;
  title: LocalizedText;
  /**
   * Tag of the main concept.
   */
  concept: string;
  /**
   * Rule ids from docs/DOMAIN.md.
   */
  rules: string[];
  /**
   * The brief shown to the learner (short Markdown).
   */
  brief: {
    es: string;
    en?: string;
  };
  /**
   * @minItems 1
   */
  criteria: [Criterion, ...Criterion[]];
  /**
   * Files the learner can edit; the rest are read-only.
   *
   * @minItems 1
   */
  editable: [JavaFileName, ...JavaFileName[]];
  /**
   * Overrides of the docs/SECURITY.md limits; they can only be lowered.
   */
  limits?: {
    timeoutMs?: number;
  };
  scene: Scene;
  /**
   * @minItems 1
   */
  checks: [Check, ...Check[]];
  /**
   * @minItems 3
   * @maxItems 3
   */
  hints: [LocalizedText, LocalizedText, LocalizedText];
}
/**
 * Learner-facing text by language; es is mandatory.
 */
export interface LocalizedText {
  es: string;
  en?: string;
}
/**
 * An item of the learner's goal list, ticked when its check passes.
 */
export interface Criterion {
  check: CheckId;
  es: string;
  en?: string;
}
export interface Scene {
  bindings: Binding[];
  /**
   * Draws signs for the local variables of main.
   */
  showVariables?: boolean;
  /**
   * Challenges whose pieces appear as context.
   */
  context?: string[];
  /**
   * Silhouette of the real UTN, from content/domain/.
   */
  realReference?: {
    regionalFaculties?: string[];
  };
}
/**
 * Maps a learner type to the archetype that draws it.
 */
export interface Binding {
  type: string;
  archetype: Archetype;
  /**
   * Field shown on the sign.
   */
  label?: string;
  /**
   * Composition fields drawn as slots: field name to slot style.
   */
  slots?: {
    [k: string]: string;
  };
}
export interface Check {
  id: CheckId;
  type: CheckType;
  /**
   * Parameters; their shape depends on type.
   */
  params?: {
    [k: string]: unknown;
  };
  /**
   * Piece affected by the check.
   */
  piece: string;
  /**
   * Slot of the piece affected by the check.
   */
  slot?: string;
  /**
   * Which line to highlight.
   */
  highlight?: 'object_creation' | 'declaration' | 'last_write';
  feedback: {
    pass: {
      what: LocalizedText;
      why: LocalizedText;
    };
    fail: {
      what: LocalizedText;
      why: LocalizedText;
      hint: LocalizedText;
    };
  };
  /**
   * Specific detections of common mistakes.
   */
  traps?: Trap[];
}
export interface Trap {
  when: {
    type: CheckType;
    params?: {
      [k: string]: unknown;
    };
  };
  what: LocalizedText;
  why: LocalizedText;
  hint?: LocalizedText;
}
