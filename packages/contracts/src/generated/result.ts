// Generated from result.schema.json by scripts/generate.mjs. Do not edit.

/**
 * passed: correct; incomplete: something from the brief is missing; failed: it broke (docs/FEEDBACK.md §1).
 */
export type State = 'passed' | 'incomplete' | 'failed';
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
 * Result of a run, returned by the api to the web (docs/ARCHITECTURE.md §5.4).
 */
export interface RunResult {
  runId: string;
  outcome: State;
  pieces: Piece[];
  log: LogEntry[];
  timeline: TimelineStep[];
  stdout: string;
}
/**
 * A visual element of the model: an object, class or relation.
 */
export interface Piece {
  id: string;
  archetype: Archetype;
  state: State;
  label: string;
  sourceRef?: SourceRef;
  /**
   * Composition slots of the piece, keyed by slot name.
   */
  slots?: {
    [k: string]: Slot;
  };
}
/**
 * A line of the learner's code.
 */
export interface SourceRef {
  file: string;
  line: number;
}
export interface Slot {
  state: 'filled' | 'missing';
}
/**
 * A log message: what happened, why, where and a hint (docs/FEEDBACK.md §3).
 */
export interface LogEntry {
  state: State;
  /**
   * What happened.
   */
  title: string;
  /**
   * The Java concept involved.
   */
  why: string;
  sourceRef?: SourceRef;
  hint?: string;
  pieceId?: string;
}
/**
 * A step of the execution replay, tied to a line and optionally to a piece.
 */
export interface TimelineStep {
  index: number;
  sourceRef: SourceRef;
  pieceId?: string;
}
