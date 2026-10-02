package io.github.balbianoluciano.ljbu.runner.execution.compile;

import java.util.Map;

/**
 * Facts that only the source has: declaration lines and {@code @Override}, which has source
 * retention and never reaches the bytecode. Keyed by binary class name.
 */
public record SourceFacts(Map<String, ClassFacts> classes) {

  /**
   * @param fieldLines declaration line by field name
   * @param methods facts by method name followed by its erased descriptor, e.g. {@code
   *     main([Ljava/lang/String;)V}; constructors are named {@code <init>}
   */
  public record ClassFacts(
      String file, int line, Map<String, Integer> fieldLines, Map<String, MethodFacts> methods) {}

  public record MethodFacts(int line, boolean override) {}

  public int classLine(String className) {
    ClassFacts facts = classes.get(className);
    return facts == null ? 1 : facts.line();
  }

  public int fieldLine(String className, String fieldName) {
    ClassFacts facts = classes.get(className);
    if (facts == null) {
      return 1;
    }
    return facts.fieldLines().getOrDefault(fieldName, facts.line());
  }

  public MethodFacts method(String className, String name, String descriptor) {
    ClassFacts facts = classes.get(className);
    if (facts == null) {
      return new MethodFacts(1, false);
    }
    MethodFacts method = facts.methods().get(name + descriptor);
    return method == null ? new MethodFacts(facts.line(), false) : method;
  }
}
