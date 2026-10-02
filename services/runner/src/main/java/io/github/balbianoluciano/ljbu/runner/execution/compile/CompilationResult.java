package io.github.balbianoluciano.ljbu.runner.execution.compile;

import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Diagnostic;
import java.util.List;
import java.util.Map;

public sealed interface CompilationResult {

  record Failure(List<Diagnostic> diagnostics) implements CompilationResult {}

  /**
   * @param classes class file bytes by binary class name, in compilation order
   */
  record Success(Map<String, byte[]> classes, SourceFacts facts) implements CompilationResult {}
}
