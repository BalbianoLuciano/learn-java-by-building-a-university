package io.github.balbianoluciano.ljbu.api.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** A Java value of the trace. The variant is told apart by its only property. */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
  @JsonSubTypes.Type(Value.IntValue.class),
  @JsonSubTypes.Type(Value.DoubleValue.class),
  @JsonSubTypes.Type(Value.BooleanValue.class),
  @JsonSubTypes.Type(Value.CharValue.class),
  @JsonSubTypes.Type(Value.StringValue.class),
  @JsonSubTypes.Type(Value.RefValue.class),
  @JsonSubTypes.Type(Value.NullValue.class)
})
public sealed interface Value {

  record IntValue(@JsonProperty("int") long value) implements Value {}

  /** {@code value} is a number, or "NaN", "Infinity" or "-Infinity" as a string. */
  record DoubleValue(@JsonProperty("double") Object value) implements Value {}

  record BooleanValue(@JsonProperty("boolean") boolean value) implements Value {}

  record CharValue(@JsonProperty("char") String value) implements Value {}

  record StringValue(@JsonProperty("string") String value) implements Value {}

  record RefValue(@JsonProperty("ref") String id) implements Value {}

  record NullValue(@JsonProperty("null") boolean isNull) implements Value {}
}
