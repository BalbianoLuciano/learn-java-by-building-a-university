package io.github.balbianoluciano.ljbu.api.trace;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/** One traced event in learner code. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Step.ObjectCreated.class, name = "object_created"),
  @JsonSubTypes.Type(value = Step.FieldSet.class, name = "field_set"),
  @JsonSubTypes.Type(value = Step.LocalSet.class, name = "local_set"),
  @JsonSubTypes.Type(value = Step.Call.class, name = "call"),
  @JsonSubTypes.Type(value = Step.Return.class, name = "return"),
  @JsonSubTypes.Type(value = Step.Output.class, name = "output"),
  @JsonSubTypes.Type(value = Step.ExceptionThrown.class, name = "exception")
})
public sealed interface Step {

  int index();

  String file();

  int line();

  record ObjectRef(String id, String type) {}

  record ObjectCreated(int index, String file, int line, ObjectRef object) implements Step {}

  /** {@code target} is the object written; a static field carries {@code ownerClass} instead. */
  record FieldSet(
      int index, String file, int line, String target, String ownerClass, String field, Value value)
      implements Step {}

  record LocalSet(int index, String file, int line, String method, String name, Value value)
      implements Step {}

  record Call(int index, String file, int line, String method, String target, List<Value> args)
      implements Step {}

  record Return(int index, String file, int line, String method, Value value) implements Step {}

  record Output(int index, String file, int line, String text) implements Step {}

  record ExceptionThrown(
      int index, String file, int line, Trace.ExceptionInfo exception, boolean caught)
      implements Step {}
}
