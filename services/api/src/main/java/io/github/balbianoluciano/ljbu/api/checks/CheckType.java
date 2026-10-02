package io.github.balbianoluciano.ljbu.api.checks;

import java.util.Arrays;
import java.util.Optional;

/** The check types of docs/ARCHITECTURE.md §6 and the parameters each one takes. */
public enum CheckType {
  CLASS_EXISTS("class_exists", CheckParams.ClassExists.class),
  EXTENDS("extends", CheckParams.Extends.class),
  IMPLEMENTS("implements", CheckParams.Implements.class),
  IS_ABSTRACT("is_abstract", CheckParams.IsAbstract.class),
  FIELD("field", CheckParams.Field.class),
  CONSTRUCTOR("constructor", CheckParams.Constructor.class),
  METHOD("method", CheckParams.Method.class),
  OBJECT_COUNT("object_count", CheckParams.ObjectCount.class),
  OBJECT_FIELD("object_field", CheckParams.ObjectField.class),
  SHARED_REFERENCE("shared_reference", CheckParams.SharedReference.class),
  CALL_DISPATCH("call_dispatch", CheckParams.CallDispatch.class),
  STDOUT_CONTAINS("stdout_contains", CheckParams.StdoutContains.class),
  NO_EXCEPTION("no_exception", CheckParams.NoException.class),
  THROWS("throws", CheckParams.Throws.class);

  private final String wireName;
  private final Class<? extends CheckParams> paramsType;

  CheckType(String wireName, Class<? extends CheckParams> paramsType) {
    this.wireName = wireName;
    this.paramsType = paramsType;
  }

  public Class<? extends CheckParams> paramsType() {
    return paramsType;
  }

  public static Optional<CheckType> of(String wireName) {
    return Arrays.stream(values()).filter(type -> type.wireName.equals(wireName)).findFirst();
  }
}
