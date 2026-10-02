package io.github.balbianoluciano.ljbu.runner.execution.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import io.github.balbianoluciano.ljbu.runner.execution.compile.CompilationResult;
import io.github.balbianoluciano.ljbu.runner.execution.compile.InMemoryCompiler;
import io.github.balbianoluciano.ljbu.runner.execution.run.JdiTracer;
import io.github.balbianoluciano.ljbu.runner.execution.run.RunOutcome;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the child JVM can see if the allowlist ever fails (docs/SECURITY.md §3, layer 4). These
 * programs skip the verifier on purpose: they use exactly what it forbids.
 */
class ChildJvmIsolationTest {

  @TempDir Path classDirectory;

  @Test
  void startsWithAnEmptyEnvironment() throws Exception {
    assertThat(runUnverified("System.out.print(System.getenv().size());")).isEqualTo("0");
  }

  @Test
  void seesOnlyTheBaseModule() throws Exception {
    String modules =
        runUnverified(
            """
            ModuleLayer.boot().modules().stream()
                .map(Module::getName)
                .sorted()
                .forEach(System.out::println);
            """);

    assertThat(modules.lines()).containsExactly("java.base");
  }

  @Test
  void worksInsideItsOwnDirectory() throws Exception {
    String directories =
        runUnverified(
            """
            System.out.println(System.getProperty("user.dir"));
            System.out.println(System.getProperty("java.io.tmpdir"));
            """);

    assertThat(directories.lines())
        .containsExactly(classDirectory.toString(), classDirectory.toString());
  }

  @Test
  void leavesNoDirectoryBehindAfterAnExecution() throws Exception {
    Path temporary = Path.of(System.getProperty("java.io.tmpdir"));
    long before = executionDirectories(temporary);

    Traces.run(Programs.mainWithBody("System.out.println(1);"));

    assertThat(executionDirectories(temporary)).isEqualTo(before);
  }

  private String runUnverified(String body) throws Exception {
    List<SourceFile> files = Programs.mainWithBody(body);
    CompilationResult.Success compiled =
        (CompilationResult.Success) new InMemoryCompiler().compile(files);
    for (Map.Entry<String, byte[]> type : compiled.classes().entrySet()) {
      Files.write(classDirectory.resolve(type.getKey() + ".class"), type.getValue());
    }
    RunOutcome outcome =
        JdiTracer.run(classDirectory, compiled.classes().keySet(), ExecutionLimits.DEFAULT);
    return outcome.stdout();
  }

  private static long executionDirectories(Path temporary) throws IOException {
    try (Stream<Path> entries = Files.list(temporary)) {
      return entries.filter(path -> path.getFileName().toString().startsWith("ljbu-run-")).count();
    }
  }
}
