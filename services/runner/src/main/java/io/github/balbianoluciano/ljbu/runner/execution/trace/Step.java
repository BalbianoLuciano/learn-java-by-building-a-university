package io.github.balbianoluciano.ljbu.runner.execution.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/** One traced event in learner code. {@code file} and {@code line} say where it happened. */
public sealed interface Step {

  int index();

  String file();

  int line();

  String event();

  record ObjectRef(String id, String type) {}

  @JsonPropertyOrder({"index", "file", "line", "event"})
  record ObjectCreated(int index, String file, int line, ObjectRef object) implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "object_created";
    }
  }

  /** {@code target} is the object written; a static field carries {@code ownerClass} instead. */
  @JsonPropertyOrder({"index", "file", "line", "event"})
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record FieldSet(
      int index, String file, int line, String target, String ownerClass, String field, Value value)
      implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "field_set";
    }
  }

  @JsonPropertyOrder({"index", "file", "line", "event"})
  record LocalSet(int index, String file, int line, String method, String name, Value value)
      implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "local_set";
    }
  }

  /** {@code target} is the receiver; it is absent in static methods. */
  @JsonPropertyOrder({"index", "file", "line", "event"})
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Call(int index, String file, int line, String method, String target, List<Value> args)
      implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "call";
    }
  }

  /** {@code value} is absent when the method returns void. */
  @JsonPropertyOrder({"index", "file", "line", "event"})
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Return(int index, String file, int line, String method, Value value) implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "return";
    }
  }

  @JsonPropertyOrder({"index", "file", "line", "event"})
  record Output(int index, String file, int line, String text) implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "output";
    }
  }

  @JsonPropertyOrder({"index", "file", "line", "event"})
  record ExceptionThrown(
      int index, String file, int line, Trace.ExceptionInfo exception, boolean caught)
      implements Step {
    @Override
    @JsonProperty("event")
    public String event() {
      return "exception";
    }
  }
}
