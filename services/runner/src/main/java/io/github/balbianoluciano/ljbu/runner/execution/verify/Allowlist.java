package io.github.balbianoluciano.ljbu.runner.execution.verify;

import java.util.Map;
import java.util.Set;

/**
 * What learner bytecode may reference outside its own classes (docs/SECURITY.md §3, layer 3).
 * Everything that is not listed here is forbidden. Widening it requires an ADR and new security
 * tests.
 */
public final class Allowlist {

  /** Members every object has; calling them through an allowed type adds no power. */
  private static final Set<String> OBJECT_METHODS =
      Set.of("toString", "equals", "hashCode", "getClass");

  /** Classes whose members are all allowed, except the ones in {@link #DENIED_MEMBERS}. */
  private static final Set<String> COMPLETE =
      Set.of(
          "java/lang/Object",
          "java/lang/String",
          "java/lang/StringBuilder",
          "java/lang/Math",
          "java/lang/Integer",
          "java/lang/Double",
          "java/lang/Boolean",
          "java/lang/Character",
          "java/lang/Long",
          "java/lang/Enum",
          "java/lang/Iterable",
          "java/util/List",
          "java/util/ArrayList",
          "java/util/Map",
          "java/util/Map$Entry",
          "java/util/HashMap",
          "java/util/Set",
          "java/util/HashSet",
          "java/util/Collection",
          "java/util/Iterator",
          "java/util/Objects",
          "java/util/Arrays");

  /** Members of otherwise complete classes that read system properties or start threads. */
  private static final Map<String, Set<String>> DENIED_MEMBERS =
      Map.of(
          "java/lang/Integer", Set.of("getInteger"),
          "java/lang/Long", Set.of("getLong"),
          "java/lang/Boolean", Set.of("getBoolean"),
          "java/util/Arrays", Set.of("parallelSort", "parallelPrefix", "parallelSetAll"),
          "java/util/Collection", Set.of("parallelStream"));

  /** Classes with only some members allowed. */
  private static final Map<String, Set<String>> PARTIAL =
      Map.of(
          "java/lang/System", Set.of("out"),
          "java/io/PrintStream", Set.of("print", "println", "printf"),
          "java/lang/Record", Set.of("<init>"));

  private static final Set<String> THROWABLE_MEMBERS = Set.of("<init>", "getMessage");

  /** Bootstrap methods javac emits for string concatenation, lambdas and records. */
  private static final Map<String, Set<String>> BOOTSTRAPS =
      Map.of(
          "java/lang/invoke/StringConcatFactory", Set.of("makeConcatWithConstants", "makeConcat"),
          "java/lang/invoke/LambdaMetafactory", Set.of("metafactory"),
          "java/lang/runtime/ObjectMethods", Set.of("bootstrap"));

  private final Set<String> learnerClasses;

  public Allowlist(Set<String> learnerClasses) {
    this.learnerClasses = Set.copyOf(learnerClasses);
  }

  /** Whether the class can be named: instantiated, extended, cast to or caught. */
  public boolean allowsClass(String internalName) {
    String name = elementType(internalName);
    return isPrimitiveDescriptor(name)
        || learnerClasses.contains(name)
        || COMPLETE.contains(name)
        || PARTIAL.containsKey(name)
        || isThrowable(name);
  }

  /** Whether the field or method can be read, written or called. */
  public boolean allowsMember(String owner, String member) {
    if (owner.startsWith("[")) {
      // Arrays only have clone() and the methods of Object.
      return allowsClass(owner) && (member.equals("clone") || OBJECT_METHODS.contains(member));
    }
    if (learnerClasses.contains(owner)) {
      return true;
    }
    if (COMPLETE.contains(owner)) {
      return !DENIED_MEMBERS.getOrDefault(owner, Set.of()).contains(member);
    }
    if (PARTIAL.containsKey(owner)) {
      return PARTIAL.get(owner).contains(member) || OBJECT_METHODS.contains(member);
    }
    if (isThrowable(owner)) {
      return THROWABLE_MEMBERS.contains(member) || OBJECT_METHODS.contains(member);
    }
    return false;
  }

  /** Whether the method can be the bootstrap of an {@code invokedynamic} call site. */
  public boolean allowsBootstrap(String owner, String method) {
    return BOOTSTRAPS.getOrDefault(owner, Set.of()).contains(method);
  }

  public boolean isLearnerClass(String internalName) {
    return learnerClasses.contains(internalName);
  }

  /** The exceptions and errors of {@code java.lang}. */
  private static boolean isThrowable(String name) {
    if (!name.startsWith("java/lang/") || name.indexOf('/', "java/lang/".length()) >= 0) {
      return false;
    }
    return name.equals("java/lang/Throwable")
        || name.endsWith("Exception")
        || name.endsWith("Error");
  }

  /**
   * {@code [[Ljava/lang/String;} becomes {@code java/lang/String}; {@code [I} becomes {@code I}.
   */
  private static String elementType(String internalName) {
    if (!internalName.startsWith("[")) {
      return internalName;
    }
    String element = internalName.substring(internalName.lastIndexOf('[') + 1);
    return element.startsWith("L") ? element.substring(1, element.length() - 1) : element;
  }

  private static boolean isPrimitiveDescriptor(String name) {
    return name.length() == 1 && "ZBCSIJFD".contains(name);
  }
}
