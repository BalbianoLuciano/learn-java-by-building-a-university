package io.github.balbianoluciano.ljbu.runner.execution.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Java value. Strings and wrappers are values (they are immutable); every other object is a
 * reference to an entry of the heap.
 */
public sealed interface Value {

  record IntValue(@JsonProperty("int") long value) implements Value {}

  /** {@code value} is a {@link Double}, or "NaN", "Infinity" or "-Infinity" as a string. */
  record DoubleValue(@JsonProperty("double") Object value) implements Value {
    public static DoubleValue of(double number) {
      return new DoubleValue(Double.isFinite(number) ? (Object) number : Double.toString(number));
    }
  }

  record BooleanValue(@JsonProperty("boolean") boolean value) implements Value {}

  record CharValue(@JsonProperty("char") String value) implements Value {}

  record StringValue(@JsonProperty("string") String value) implements Value {}

  record RefValue(@JsonProperty("ref") String id) implements Value {}

  record NullValue() implements Value {
    @JsonProperty("null")
    public boolean isNull() {
      return true;
    }
  }
}
