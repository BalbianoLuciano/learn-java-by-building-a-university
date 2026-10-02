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
 * A Java value, as in the trace.
 */
export type Value =
  | {
      int: number;
    }
  | {
      double: number | ('NaN' | 'Infinity' | '-Infinity');
    }
  | {
      boolean: boolean;
    }
  | {
      char: string;
    }
  | {
      string: string;
    }
  | {
      ref: string;
    }
  | {
      null: true;
    };

/**
 * Result of a run, returned by the api to the web (docs/ARCHITECTURE.md §5.4).
 */
export interface RunResult {
  runId: string;
  outcome: State;
  /**
   * Checks of the challenge that pass, out of all of them.
   */
  progress: {
    passed: number;
    total: number;
  };
  pieces: Piece[];
  log: LogEntry[];
  timeline: TimelineStep[];
  stdout: string;
}
/**
 * A visual element of the model: an object the brief asks for, another object the code created, or a variable.
 */
export interface Piece {
  id: string;
  archetype: Archetype;
  state: State;
  /**
   * False when the brief asks for the piece and the code did not create it.
   */
  built: boolean;
  label: string;
  sourceRef?: SourceRef;
  /**
   * Composition slots of the piece, keyed by slot name.
   */
  slots?: {
    [k: string]: Slot;
  };
  /**
   * For a variable: the piece it points to. Absent when it points to nothing.
   */
  target?: string;
}
/**
 * Where the code created it.
 */
export interface SourceRef {
  file: string;
  line: number;
}
export interface Slot {
  state: 'filled' | 'missing';
  /**
   * Pieces that occupy the slot.
   */
  pieceIds: string[];
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
  sourceRef?: SourceRef1;
  hint?: string;
  pieceId?: string;
  /**
   * The check of the challenge this entry reports, if any.
   */
  checkId?: string;
  /**
   * Original text of the compiler or the JVM, shown under the message.
   */
  detail?: string;
}
/**
 * A line of the learner's code.
 */
export interface SourceRef1 {
  file: string;
  line: number;
}
/**
 * A step of the execution, tied to a line and to the pieces it touches. name is the class created, the field or variable written, the method called or the exception thrown, depending on the event.
 */
export interface TimelineStep {
  index: number;
  sourceRef: SourceRef1;
  event: 'object_created' | 'field_set' | 'local_set' | 'call' | 'return' | 'output' | 'exception';
  /**
   * The piece created, written, called or, for a local, the variable.
   */
  pieceId?: string;
  /**
   * The piece a field or a variable points to after the step.
   */
  targetPieceId?: string;
  name?: string;
  value?: Value;
  /**
   * Output written, or message of the exception.
   */
  text?: string;
  /**
   * For an exception: whether the code catches it.
   */
  caught?: boolean;
}
