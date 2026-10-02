package io.github.balbianoluciano.ljbu.api.trace;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

/** Final state of an object of the trace. The variant is told apart by its properties. */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
  @JsonSubTypes.Type(HeapObject.Instance.class),
  @JsonSubTypes.Type(HeapObject.Sequence.class),
  @JsonSubTypes.Type(HeapObject.MapObject.class)
})
public sealed interface HeapObject {

  String type();

  record Instance(String type, Map<String, Value> fields) implements HeapObject {}

  record Sequence(String type, int size, List<Value> elements) implements HeapObject {}

  record MapObject(String type, int size, List<Entry> entries) implements HeapObject {}

  record Entry(Value key, Value value) {}
}
