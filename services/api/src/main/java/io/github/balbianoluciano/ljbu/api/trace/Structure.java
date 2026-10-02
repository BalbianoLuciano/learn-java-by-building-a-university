package io.github.balbianoluciano.ljbu.api.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;

/** Static structure of the learner's compiled classes. */
public record Structure(List<ClassInfo> classes) {

  public Optional<ClassInfo> find(String className) {
    return classes.stream().filter(type -> type.name().equals(className)).findFirst();
  }

  /** Whether {@code type} is {@code ancestor} or extends or implements it, at any distance. */
  public boolean isSubtype(String type, String ancestor) {
    if (type.equals(ancestor)) {
      return true;
    }
    Optional<ClassInfo> info = find(type);
    if (info.isEmpty()) {
      return false;
    }
    if (info.get().superclass() != null && isSubtype(info.get().superclass(), ancestor)) {
      return true;
    }
    return info.get().interfaces().stream().anyMatch(parent -> isSubtype(parent, ancestor));
  }

  public record ClassInfo(
      String name,
      String kind,
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
      String visibility,
      @JsonProperty("final") boolean isFinal,
      @JsonProperty("static") boolean isStatic,
      int line) {}

  public record ConstructorInfo(List<String> parameterTypes, String visibility, int line) {}

  public record MethodInfo(
      String name,
      String returnType,
      List<String> parameterTypes,
      String visibility,
      @JsonProperty("static") boolean isStatic,
      @JsonProperty("abstract") boolean isAbstract,
      @JsonProperty("override") boolean isOverride,
      int line) {}
}
