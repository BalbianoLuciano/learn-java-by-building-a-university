package io.github.balbianoluciano.ljbu.api.trace;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

/**
 * Execution report of the runner, as the api reads it. The contract is
 * packages/contracts/trace.schema.json; answers are validated against it before they get here.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Trace.CompileError.class, name = "compile_error"),
  @JsonSubTypes.Type(value = Trace.Rejected.class, name = "rejected"),
  @JsonSubTypes.Type(
      value = Trace.Executed.class,
      names = {"completed", "runtime_error", "timeout", "limit_exceeded"})
})
public sealed interface Trace {

  String status();

  record CompileError(String status, List<Diagnostic> diagnostics) implements Trace {}

  record Rejected(String status, Structure structure, List<Violation> violations)
      implements Trace {}

  record Executed(
      String status,
      Structure structure,
      String stdout,
      List<Step> steps,
      Map<String, HeapObject> heap,
      Map<String, Map<String, Value>> statics,
      ExceptionInfo exception,
      Limits limits)
      implements Trace {

    public boolean completed() {
      return status.equals("completed");
    }
  }

  /** {@code exceeded} is steps, objects, output or call_depth; null when no limit cut the run. */
  record Limits(int steps, boolean truncated, String exceeded) {}

  record Diagnostic(String file, int line, int column, String code, String message) {}

  record Violation(String file, int line, String symbol) {}

  record ExceptionInfo(String type, String message, String file, int line) {}
}
