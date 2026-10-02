package io.github.balbianoluciano.ljbu.runner.execution.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

/** Static structure of the learner's compiled classes. */
public record Structure(List<ClassInfo> classes) {

  public record ClassInfo(
      String name,
      Kind kind,
      @JsonProperty("abstract") boolean isAbstract,
      String superclass,
      List<String> interfaces,
      String file,
      int line,
      List<FieldInfo> fields,
      List<ConstructorInfo> constructors,
      List<MethodInfo> methods) {}

  public record FieldInfo(
      String name,
      String type,
      Visibility visibility,
      @JsonProperty("final") boolean isFinal,
      @JsonProperty("static") boolean isStatic,
      int line) {}

  public record ConstructorInfo(List<String> parameterTypes, Visibility visibility, int line) {}

  public record MethodInfo(
      String name,
      String returnType,
      List<String> parameterTypes,
      Visibility visibility,
      @JsonProperty("static") boolean isStatic,
      @JsonProperty("abstract") boolean isAbstract,
      @JsonProperty("override") boolean isOverride,
      int line) {}

  public enum Kind {
    CLASS,
    INTERFACE,
    ENUM,
    RECORD;

    @JsonValue
    public String wireName() {
      return name().toLowerCase();
    }
  }

  public enum Visibility {
    PUBLIC,
    PROTECTED,
    PACKAGE,
    PRIVATE;

    @JsonValue
    public String wireName() {
      return name().toLowerCase();
    }
  }
}
