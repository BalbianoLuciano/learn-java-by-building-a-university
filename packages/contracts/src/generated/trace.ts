// Generated from trace.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Execution report returned by the runner to the api (docs/ARCHITECTURE.md §5.2).
 */
export type Trace = CompileErrorTrace | RejectedTrace | ExecutedTrace;
export type Visibility = 'public' | 'protected' | 'package' | 'private';
/**
 * Identifier of a traced object, stable within one trace.
 */
export type ObjectId = string;
/**
 * A Java value. Strings and wrappers are values (they are immutable); every other object is a reference.
 */
export type Value =
  | {
      int: number;
    }
  | {
      double: number;
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
      ref: ObjectId;
    }
  | {
      null: true;
    };

/**
 * The code did not compile; nothing was executed.
 */
export interface CompileErrorTrace {
  status: 'compile_error';
  /**
   * @minItems 1
   */
  diagnostics: [Diagnostic, ...Diagnostic[]];
}
/**
 * A javac error.
 */
export interface Diagnostic {
  file: string;
  line: number;
  column: number;
  /**
   * javac diagnostic code, e.g. compiler.err.cant.resolve.location.
   */
  code: string;
  /**
   * Original javac message.
   */
  message: string;
}
/**
 * The code compiled but uses something outside the bytecode allowlist; nothing was executed.
 */
export interface RejectedTrace {
  status: 'rejected';
  structure: Structure;
  /**
   * @minItems 1
   */
  violations: [Violation, ...Violation[]];
}
/**
 * Static structure of the learner's compiled classes.
 */
export interface Structure {
  classes: ClassInfo[];
}
export interface ClassInfo {
  name: string;
  kind: 'class' | 'interface' | 'enum' | 'record';
  abstract: boolean;
  /**
   * Null when the class extends java.lang.Object directly.
   */
  superclass: string | null;
  interfaces: string[];
  file: string;
  line: number;
  fields: FieldInfo[];
  constructors: ConstructorInfo[];
  methods: MethodInfo[];
}
export interface FieldInfo {
  name: string;
  type: string;
  visibility: Visibility;
  final: boolean;
  static: boolean;
  line: number;
}
export interface ConstructorInfo {
  parameterTypes: string[];
  visibility: Visibility;
  line: number;
}
export interface MethodInfo {
  name: string;
  returnType: string;
  parameterTypes: string[];
  visibility: Visibility;
  static: boolean;
  abstract: boolean;
  line: number;
}
/**
 * A reference to a class, method or field outside the allowlist.
 */
export interface Violation {
  file: string;
  line: number;
  /**
   * The forbidden reference, e.g. java.lang.System.exit.
   */
  symbol: string;
}
/**
 * The code ran in the child JVM, to completion or until an exception or a limit stopped it.
 */
export interface ExecutedTrace {
  status: 'completed' | 'runtime_error' | 'timeout' | 'limit_exceeded';
  structure: Structure;
  /**
   * Standard output, truncated to the output limit.
   */
  stdout: string;
  steps: Step[];
  /**
   * Final state of the objects reachable from learner classes, keyed by object id.
   */
  heap: {
    [k: string]: HeapObject;
  };
  exception: null | ExceptionInfo;
  limits: {
    /**
     * Number of steps recorded.
     */
    steps: number;
    /**
     * True when a limit cut the trace or the output short.
     */
    truncated: boolean;
  };
}
/**
 * One traced event in learner code. Each step carries only what changed: object for object_created; target, field and value for field_set.
 */
export interface Step {
  index: number;
  file: string;
  line: number;
  event: 'object_created' | 'field_set' | 'local_set' | 'call' | 'return' | 'output' | 'exception';
  /**
   * object_created: the new object.
   */
  object?: {
    id: ObjectId;
    type: string;
  };
  target?: ObjectId;
  /**
   * field_set: the field written.
   */
  field?: string;
  value?: Value;
  /**
   * Call stack with the locals visible after the event, innermost frame last.
   */
  frames?: Frame[];
}
export interface Frame {
  /**
   * Qualified as Class.method, e.g. Main.main.
   */
  method: string;
  locals: {
    [k: string]: Value;
  };
}
export interface HeapObject {
  type: string;
  fields: {
    [k: string]: Value;
  };
}
/**
 * The uncaught exception that stopped the program. file and line point to the first frame in learner code.
 */
export interface ExceptionInfo {
  /**
   * Fully qualified class name.
   */
  type: string;
  message: string | null;
  file: string;
  line: number;
}
