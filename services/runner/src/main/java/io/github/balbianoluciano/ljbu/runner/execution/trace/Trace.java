package io.github.balbianoluciano.ljbu.runner.execution.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Map;

/**
 * Execution report returned to the api. Its JSON form is the contract in
 * packages/contracts/trace.schema.json.
 */
public sealed interface Trace {

  String status();

  @JsonPropertyOrder({"status", "diagnostics"})
  record CompileError(List<Diagnostic> diagnostics) implements Trace {
    @Override
    @JsonProperty("status")
    public String status() {
      return "compile_error";
    }
  }

  @JsonPropertyOrder({"status", "structure", "violations"})
  record Rejected(Structure structure, List<Violation> violations) implements Trace {
    @Override
    @JsonProperty("status")
    public String status() {
      return "rejected";
    }
  }

  @JsonPropertyOrder({"status", "structure", "stdout", "steps", "heap", "statics", "exception"})
  record Executed(
      @JsonProperty("status") Status executionStatus,
      Structure structure,
      String stdout,
      List<Step> steps,
      Map<String, HeapObject> heap,
      Map<String, Map<String, Value>> statics,
      ExceptionInfo exception,
      Limits limits)
      implements Trace {
    @Override
    public String status() {
      return executionStatus.wireName();
    }
  }

  enum Status {
    COMPLETED("completed"),
    RUNTIME_ERROR("runtime_error"),
    TIMEOUT("timeout"),
    LIMIT_EXCEEDED("limit_exceeded");

    private final String wireName;

    Status(String wireName) {
      this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
      return wireName;
    }
  }

  enum ExceededLimit {
    STEPS("steps"),
    OBJECTS("objects"),
    OUTPUT("output"),
    CALL_DEPTH("call_depth");

    private final String wireName;

    ExceededLimit(String wireName) {
      this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
      return wireName;
    }
  }

  record Limits(int steps, boolean truncated, ExceededLimit exceeded) {}

  record Diagnostic(String file, int line, int column, String code, String message) {}

  record Violation(String file, int line, String symbol) {}

  record ExceptionInfo(String type, String message, String file, int line) {}
}
