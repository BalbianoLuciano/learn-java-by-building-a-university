package io.github.balbianoluciano.ljbu.runner.execution;

import io.github.balbianoluciano.ljbu.runner.execution.compile.CompilationResult;
import io.github.balbianoluciano.ljbu.runner.execution.compile.InMemoryCompiler;
import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts;
import io.github.balbianoluciano.ljbu.runner.execution.run.JdiTracer;
import io.github.balbianoluciano.ljbu.runner.execution.run.RunOutcome;
import io.github.balbianoluciano.ljbu.runner.execution.structure.StructureExtractor;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Diagnostic;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Violation;
import io.github.balbianoluciano.ljbu.runner.execution.verify.AllowlistVerifier;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

/**
 * The pipeline of docs/ARCHITECTURE.md §4: compile, extract the structure, verify the bytecode and,
 * only if everything is allowed, run under JDI.
 */
public final class Executor {

  public static final String MAIN_NOT_FOUND = "ljbu.err.main.not.found";

  private final InMemoryCompiler compiler = new InMemoryCompiler();

  /**
   * @throws InvalidInputException if the files break an input rule
   */
  public ExecutionReport execute(List<SourceFile> files, ExecutionLimits limits)
      throws InterruptedException {
    InputValidator.validate(files);

    long start = System.nanoTime();
    CompilationResult compilation = compiler.compile(files);
    long compiled = System.nanoTime();
    if (compilation instanceof CompilationResult.Failure failure) {
      return report(
          new Trace.CompileError(failure.diagnostics()), start, compiled, compiled, compiled);
    }
    CompilationResult.Success success = (CompilationResult.Success) compilation;
    Structure structure = StructureExtractor.extract(success.classes().values(), success.facts());
    if (!declaresMain(structure)) {
      return report(mainNotFound(success.facts()), start, compiled, compiled, compiled);
    }

    List<Violation> violations = AllowlistVerifier.verify(success.classes(), success.facts());
    long verified = System.nanoTime();
    if (!violations.isEmpty()) {
      return report(new Trace.Rejected(structure, violations), start, compiled, verified, verified);
    }

    RunOutcome outcome = run(success.classes(), limits);
    Trace trace =
        new Trace.Executed(
            outcome.status(),
            structure,
            outcome.stdout(),
            outcome.steps(),
            outcome.heap(),
            outcome.statics(),
            outcome.exception(),
            outcome.limits());
    return report(trace, start, compiled, verified, System.nanoTime());
  }

  private static ExecutionReport report(
      Trace trace, long start, long compiled, long verified, long ran) {
    return new ExecutionReport(
        trace,
        (compiled - start) / 1_000_000,
        (verified - compiled) / 1_000_000,
        (ran - verified) / 1_000_000);
  }

  private static boolean declaresMain(Structure structure) {
    return structure.classes().stream()
        .filter(type -> type.name().equals("Main"))
        .flatMap(type -> type.methods().stream())
        .anyMatch(
            method ->
                method.name().equals("main")
                    && method.isStatic()
                    && method.visibility() == Structure.Visibility.PUBLIC
                    && method.returnType().equals("void")
                    && method.parameterTypes().equals(List.of("String[]")));
  }

  private static Trace mainNotFound(SourceFacts facts) {
    return new Trace.CompileError(
        List.of(
            new Diagnostic(
                InputValidator.MAIN_FILE,
                facts.classLine("Main"),
                1,
                MAIN_NOT_FOUND,
                "Main must declare public static void main(String[] args)")));
  }

  /** Each execution gets its own directory, deleted when it ends. */
  private static RunOutcome run(Map<String, byte[]> classes, ExecutionLimits limits)
      throws InterruptedException {
    Path directory = null;
    try {
      directory = Files.createTempDirectory("ljbu-run-");
      for (Map.Entry<String, byte[]> compiled : classes.entrySet()) {
        Files.write(directory.resolve(compiled.getKey() + ".class"), compiled.getValue());
      }
      return JdiTracer.run(directory, classes.keySet(), limits);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot run the child JVM", e);
    } finally {
      delete(directory);
    }
  }

  private static void delete(Path directory) {
    if (directory == null) {
      return;
    }
    try {
      Files.walkFileTree(
          directory,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path visited, IOException failure)
                throws IOException {
              Files.delete(visited);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      // Left to the operating system: the directory holds only class files.
    }
  }
}
