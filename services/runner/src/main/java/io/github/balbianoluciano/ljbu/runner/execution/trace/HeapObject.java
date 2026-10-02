package io.github.balbianoluciano.ljbu.runner.execution.trace;

import java.util.List;
import java.util.Map;

/** Final state of an object referenced by the trace. */
public sealed interface HeapObject {

  String type();

  /** An object with its fields; objects of classes the learner did not write carry none. */
  record Instance(String type, Map<String, Value> fields) implements HeapObject {}

  /** An array, a list or a set. {@code size} is the real size; elements may be cut. */
  record Sequence(String type, int size, List<Value> elements) implements HeapObject {}

  /** A map. {@code size} is the real size; entries may be cut. */
  record MapObject(String type, int size, List<Entry> entries) implements HeapObject {}

  record Entry(Value key, Value value) {}
}
