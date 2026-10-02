// Generated from trace.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Execution report returned by the runner to the api (docs/ARCHITECTURE.md §5.2).
 */
export type Trace = CompileErrorTrace | RejectedTrace | ExecutedTrace;
export type Visibility = 'public' | 'protected' | 'package' | 'private';
/**
 * One traced event in learner code. file and line say where it happened.
 */
export type Step =
  | ObjectCreatedStep
  | FieldSetStep
  | LocalSetStep
  | CallStep
  | ReturnStep
  | OutputStep
  | ExceptionStep;
/**
 * Identifier of a traced object, stable within one trace.
 */
export type ObjectId = string;
/**
 * A Java value. Strings and wrappers are values (they are immutable); every other object is a reference.
 */
export type Value =
  | {
      /**
       * byte, short, int or long.
       */
      int: number;
    }
  | {
      /**
       * float or double; the non-finite values travel as strings.
       */
      double: number | ('NaN' | 'Infinity' | '-Infinity');
    }
  | {
      boolean: boolean;
    }
  | {
      char: string;
    }
  | {
      /**
       * Cut at 1000 characters.
       */
      string: string;
    }
  | {
      ref: ObjectId;
    }
  | {
      null: true;
    };
/**
 * Class and method joined by a dot, e.g. Main.main; constructors are Class.<init>.
 */
export type MethodName = string;
export type HeapObject = InstanceObject | SequenceObject | MapObject;

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
 * A compilation error: a javac diagnostic, or a runner rule reported with an ljbu.err.* code.
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
/**
 * Types are written with simple names and their type arguments, e.g. List<Departamento>.
 */
export interface ClassInfo {
  name: string;
  kind: 'class' | 'interface' | 'enum' | 'record';
  abstract: boolean;
  /**
   * Null when the class has no explicit superclass.
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
  /**
   * Line of the declaration; the line of the class for an implicit constructor.
   */
  line: number;
}
export interface MethodInfo {
  name: string;
  returnType: string;
  parameterTypes: string[];
  visibility: Visibility;
  static: boolean;
  abstract: boolean;
  /**
   * True when the method is annotated with @Override in the source.
   */
  override: boolean;
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
   * Standard output, cut at the output limit.
   */
  stdout: string;
  steps: Step[];
  /**
   * Final state of every object referenced by the trace, keyed by object id.
   */
  heap: {
    [k: string]: HeapObject;
  };
  /**
   * Final values of the static fields of learner classes, keyed by class name.
   */
  statics: {
    [k: string]: {
      [k: string]: Value;
    };
  };
  /**
   * The uncaught exception that ended the program, if any.
   */
  exception: null | ExceptionInfo;
  limits: {
    /**
     * Number of steps recorded.
     */
    steps: number;
    /**
     * True when the program was cut before it finished on its own.
     */
    truncated: boolean;
    /**
     * The limit that cut the program when status is limit_exceeded; null otherwise.
     */
    exceeded: 'steps' | 'objects' | 'output' | 'call_depth' | null;
  };
}
/**
 * An object of a learner class was created. The line is the one with the new expression.
 */
export interface ObjectCreatedStep {
  index: number;
  file: string;
  line: number;
  event: 'object_created';
  object: {
    id: ObjectId;
    type: string;
  };
}
/**
 * A field of a learner class was written. target is the object; a static field carries ownerClass instead.
 */
export interface FieldSetStep {
  index: number;
  file: string;
  line: number;
  event: 'field_set';
  target?: ObjectId;
  ownerClass?: string;
  field: string;
  value: Value;
}
/**
 * A local variable of a learner method received a value.
 */
export interface LocalSetStep {
  index: number;
  file: string;
  line: number;
  event: 'local_set';
  method: MethodName;
  name: string;
  value: Value;
}
/**
 * A learner method or constructor started. The line is the call site when the caller is learner code. method names the implementation that runs; target is the receiver, absent in static methods.
 */
export interface CallStep {
  index: number;
  file: string;
  line: number;
  event: 'call';
  method: MethodName;
  target?: ObjectId;
  args: Value[];
}
/**
 * A learner method or constructor finished normally. value is absent when it returns void.
 */
export interface ReturnStep {
  index: number;
  file: string;
  line: number;
  event: 'return';
  method: MethodName;
  value?: Value;
}
/**
 * The line wrote text to standard output.
 */
export interface OutputStep {
  index: number;
  file: string;
  line: number;
  event: 'output';
  text: string;
}
/**
 * An exception was thrown. caught says whether learner code catches it; otherwise it ends the program.
 */
export interface ExceptionStep {
  index: number;
  file: string;
  line: number;
  event: 'exception';
  exception: ExceptionInfo;
  caught: boolean;
}
/**
 * An exception. file and line point to the first frame in learner code.
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
/**
 * An object with its fields. Objects of classes the learner did not write carry no fields.
 */
export interface InstanceObject {
  type: string;
  fields: {
    [k: string]: Value;
  };
}
/**
 * An array, a list or a set, with at most its first 200 elements.
 */
export interface SequenceObject {
  type: string;
  size: number;
  elements: Value[];
}
/**
 * A map, with at most its first 200 entries.
 */
export interface MapObject {
  type: string;
  size: number;
  entries: {
    key: Value;
    value: Value;
  }[];
}
