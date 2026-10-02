package io.github.balbianoluciano.ljbu.api.checks;

import io.github.balbianoluciano.ljbu.api.trace.Value;

/** Compares values of the trace with the texts, numbers and booleans written in a challenge. */
public final class Values {

  private Values() {}

  /** A null or a reference never matches: the expected value is always a plain value. */
  public static boolean matches(Value actual, Object expected) {
    return switch (actual) {
      case Value.StringValue text ->
          expected instanceof String string && string.equals(text.value());
      case Value.CharValue character ->
          expected instanceof String string && string.equals(character.value());
      case Value.BooleanValue bool -> expected instanceof Boolean flag && flag == bool.value();
      case Value.IntValue number ->
          expected instanceof Number wanted && wanted.doubleValue() == number.value();
      case Value.DoubleValue number ->
          expected instanceof Number wanted
              && number.value() instanceof Number real
              && wanted.doubleValue() == real.doubleValue();
      case Value.RefValue reference -> false;
      case Value.NullValue nothing -> false;
      case null -> false;
    };
  }

  public static boolean isNull(Value value) {
    return value instanceof Value.NullValue;
  }

  /** The id of the object a value points to, or null if it is not a reference. */
  public static String referenceOf(Value value) {
    return value instanceof Value.RefValue reference ? reference.id() : null;
  }
}
