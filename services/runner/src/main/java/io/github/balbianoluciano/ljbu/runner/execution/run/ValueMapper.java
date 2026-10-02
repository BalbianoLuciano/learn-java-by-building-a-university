package io.github.balbianoluciano.ljbu.runner.execution.run;

import com.sun.jdi.ByteValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StringReference;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import java.util.Set;

/** Turns values of the child JVM into the values of the trace. */
final class ValueMapper {

  static final int MAX_STRING_LENGTH = 1000;

  private static final Set<String> WRAPPERS =
      Set.of(
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Short",
          "java.lang.Byte",
          "java.lang.Double",
          "java.lang.Float",
          "java.lang.Boolean",
          "java.lang.Character");

  private final ObjectRegistry registry;

  ValueMapper(ObjectRegistry registry) {
    this.registry = registry;
  }

  Value toValue(com.sun.jdi.Value value) {
    return switch (value) {
      case null -> new Value.NullValue();
      case com.sun.jdi.BooleanValue bool -> new Value.BooleanValue(bool.value());
      case com.sun.jdi.CharValue character ->
          new Value.CharValue(String.valueOf(character.value()));
      case ByteValue number -> new Value.IntValue(number.value());
      case ShortValue number -> new Value.IntValue(number.value());
      case IntegerValue number -> new Value.IntValue(number.value());
      case LongValue number -> new Value.IntValue(number.value());
      case FloatValue number -> Value.DoubleValue.of(number.value());
      case com.sun.jdi.DoubleValue number -> Value.DoubleValue.of(number.value());
      case StringReference string -> new Value.StringValue(cut(string.value()));
      case ObjectReference object when isWrapper(object) ->
          toValue(object.getValue(object.referenceType().fieldByName("value")));
      case ObjectReference object -> new Value.RefValue(registry.idOf(object));
      default -> new Value.NullValue();
    };
  }

  /**
   * A key that changes when a variable changes, computed without registering objects: a variable
   * that is only read never puts its object in the trace.
   */
  Object changeKey(com.sun.jdi.Value value) {
    if (value instanceof ObjectReference object
        && !(value instanceof StringReference)
        && !isWrapper(object)) {
      return object.uniqueID();
    }
    return toValue(value);
  }

  static String cut(String text) {
    return text.length() <= MAX_STRING_LENGTH ? text : text.substring(0, MAX_STRING_LENGTH);
  }

  private static boolean isWrapper(ObjectReference object) {
    return WRAPPERS.contains(object.referenceType().name());
  }
}
