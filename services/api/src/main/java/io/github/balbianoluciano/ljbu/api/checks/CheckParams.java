package io.github.balbianoluciano.ljbu.api.checks;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;

/**
 * Parameters of each check type of docs/ARCHITECTURE.md §6, as written under {@code params} in a
 * challenge.yaml. Adding a type requires an ADR.
 */
public sealed interface CheckParams {

  /** Fails with a message for the content author if a parameter is missing or inconsistent. */
  void validate();

  /** Placeholders the feedback of this check may use. */
  default Set<String> placeholders() {
    return Set.of();
  }

  // --- Structure

  record ClassExists(@JsonProperty("class") String className) implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
    }
  }

  record Extends(@JsonProperty("class") String className, String superclass)
      implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
      require(superclass, "superclass");
    }
  }

  record Implements(
      @JsonProperty("class") String className, @JsonProperty("interface") String interfaceName)
      implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
      require(interfaceName, "interface");
    }
  }

  /** The class is abstract or, when {@code method} is given, that method is. */
  record IsAbstract(@JsonProperty("class") String className, String method) implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
    }
  }

  /** One field ({@code name}) or several ({@code names}), all with the given attributes. */
  record Field(
      @JsonProperty("class") String className,
      String name,
      List<String> names,
      String type,
      String visibility,
      @JsonProperty("final") Boolean isFinal,
      @JsonProperty("static") Boolean isStatic)
      implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
      if ((name == null) == (names == null || names.isEmpty())) {
        throw new IllegalArgumentException("exactly one of name and names is required");
      }
    }

    public List<String> fieldNames() {
      return name != null ? List.of(name) : names;
    }

    @Override
    public Set<String> placeholders() {
      return Set.of("missing");
    }
  }

  /**
   * @param delegatesTo "this" or "super": the constructor, when it runs, calls another constructor
   *     of its class or one of its superclass
   */
  record Constructor(
      @JsonProperty("class") String className,
      List<String> parameterTypes,
      String visibility,
      String delegatesTo)
      implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
      require(parameterTypes, "parameterTypes");
      if (delegatesTo != null && !Set.of("this", "super").contains(delegatesTo)) {
        throw new IllegalArgumentException("delegatesTo must be this or super");
      }
    }
  }

  record Method(
      @JsonProperty("class") String className,
      String name,
      List<String> parameterTypes,
      String returnType,
      String visibility,
      @JsonProperty("static") Boolean isStatic,
      @JsonProperty("abstract") Boolean isAbstract,
      @JsonProperty("override") Boolean isOverride)
      implements CheckParams {
    @Override
    public void validate() {
      require(className, "class");
      require(name, "name");
    }
  }

  // --- Execution

  /** How many objects of the type exist; with {@code where}, how many satisfy it at the end. */
  record ObjectCount(
      String type,
      FieldCondition where,
      @JsonProperty("equals") Integer exactly,
      Integer atLeast,
      Integer atMost)
      implements CheckParams {
    @Override
    public void validate() {
      require(type, "type");
      if (exactly == null && atLeast == null && atMost == null) {
        throw new IllegalArgumentException("one of equals, atLeast and atMost is required");
      }
    }

    @Override
    public Set<String> placeholders() {
      return Set.of("count");
    }
  }

  /**
   * The object selected by {@code where} ends with the expected field values.
   *
   * @param all every object selected must satisfy the expectations, not just the first one
   * @param writtenThrough the last write of the field went through that local variable
   */
  record ObjectField(
      ObjectSelector where,
      Boolean all,
      @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<Expectation> expect,
      LocalRef writtenThrough)
      implements CheckParams {
    @Override
    public void validate() {
      require(where, "where");
      where.validate();
      require(expect, "expect");
      if (expect.isEmpty()) {
        throw new IllegalArgumentException("expect needs at least one field");
      }
      expect.forEach(Expectation::validate);
      if (writtenThrough != null) {
        require(writtenThrough.local(), "writtenThrough.local");
        if (expect.size() != 1 || Boolean.TRUE.equals(all)) {
          throw new IllegalArgumentException("writtenThrough needs one object and one field");
        }
      }
    }

    public boolean everyObject() {
      return Boolean.TRUE.equals(all);
    }

    @Override
    public Set<String> placeholders() {
      return Set.of("missing");
    }
  }

  /**
   * Two references point to the same object ({@code a} and {@code b}), or the field of every object
   * of a type points to one same object ({@code all}).
   */
  record SharedReference(Reference a, Reference b, FieldOfAll all) implements CheckParams {
    @Override
    public void validate() {
      if (all != null) {
        require(all.type(), "all.type");
        require(all.field(), "all.field");
        if (a != null || b != null) {
          throw new IllegalArgumentException("use a and b, or all, not both");
        }
        return;
      }
      require(a, "a");
      require(b, "b");
      a.validate();
      b.validate();
    }
  }

  /**
   * A call to {@code method} on an object whose class is {@code receiverType} ran the
   * implementation declared in {@code implementation}.
   */
  record CallDispatch(String method, String receiverType, String implementation)
      implements CheckParams {
    @Override
    public void validate() {
      require(method, "method");
      require(receiverType, "receiverType");
      require(implementation, "implementation");
    }
  }

  record StdoutContains(String text, Boolean ignoreCase) implements CheckParams {
    @Override
    public void validate() {
      require(text, "text");
    }
  }

  /** The program ended without an uncaught exception. */
  record NoException() implements CheckParams {
    @Override
    public void validate() {}
  }

  /**
   * An exception of the type was thrown.
   *
   * @param caught true: the code must catch it; false: it must end the program; null: either
   */
  record Throws(String type, String messageContains, Boolean caught) implements CheckParams {
    @Override
    public void validate() {
      require(type, "type");
    }
  }

  // --- Selectors

  /** A condition on a field: equal, or different, to a text, a number or a boolean. */
  record FieldCondition(String field, @JsonProperty("equals") Object expected, Object notEquals) {
    void validate() {
      require(field, "field");
      if ((expected == null) == (notEquals == null)) {
        throw new IllegalArgumentException("exactly one of equals and notEquals is required");
      }
    }
  }

  /** Objects of a type (or of its subtypes), optionally those whose field meets a condition. */
  record ObjectSelector(
      String type, String field, @JsonProperty("equals") Object expected, Object notEquals) {
    void validate() {
      require(type, "where.type");
      if (field != null && (expected == null) == (notEquals == null)) {
        throw new IllegalArgumentException("where.field needs exactly one of equals and notEquals");
      }
    }

    public FieldCondition condition() {
      return field == null ? null : new FieldCondition(field, expected, notEquals);
    }
  }

  record Expectation(
      String field, @JsonProperty("equals") Object expected, Boolean notNull, Boolean isNull) {
    void validate() {
      require(field, "expect.field");
      int conditions =
          (expected != null ? 1 : 0) + (notNull != null ? 1 : 0) + (isNull != null ? 1 : 0);
      if (conditions != 1) {
        throw new IllegalArgumentException(
            "expect needs exactly one of equals, notNull and isNull");
      }
    }
  }

  /** A local variable of a method written as Class.method; Main.main when {@code in} is absent. */
  record LocalRef(String local, String in) {
    public String method() {
      return in == null ? "Main.main" : in;
    }
  }

  /** A local variable, or the field of an object. */
  record Reference(String local, String in, ObjectSelector object, String field) {
    void validate() {
      if (local != null && object == null && field == null) {
        return;
      }
      if (local == null && object != null && field != null) {
        object.validate();
        return;
      }
      throw new IllegalArgumentException("a reference is a local, or an object with a field");
    }

    public String method() {
      return in == null ? "Main.main" : in;
    }
  }

  record FieldOfAll(String type, String field) {}

  private static void require(Object value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required");
    }
  }
}
