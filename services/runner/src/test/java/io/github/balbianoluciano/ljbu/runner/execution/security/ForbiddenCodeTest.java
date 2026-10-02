package io.github.balbianoluciano.ljbu.runner.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Malicious programs the bytecode allowlist must reject before anything runs (docs/SECURITY.md §5).
 * A change to the allowlist must add cases here.
 */
class ForbiddenCodeTest {

  static Stream<Arguments> forbiddenStatements() {
    return Stream.of(
        arguments("exiting the JVM", "System.exit(0);", "java.lang.System.exit"),
        arguments(
            "reading the environment",
            "System.getenv(\"RUNNER_TOKEN\");",
            "java.lang.System.getenv"),
        arguments(
            "reading system properties",
            "System.getProperty(\"user.home\");",
            "java.lang.System.getProperty"),
        arguments(
            "reading system properties through a wrapper",
            "Integer.getInteger(\"user.home\");",
            "java.lang.Integer.getInteger"),
        arguments("reading standard input", "System.in.read();", "java.lang.System.in"),
        arguments("replacing standard output", "System.setOut(null);", "java.lang.System.setOut"),
        arguments(
            "running a command",
            "Runtime.getRuntime().exec(\"id\");",
            "java.lang.Runtime.getRuntime"),
        arguments(
            "starting a process",
            "new ProcessBuilder(\"id\").start();",
            "java.lang.ProcessBuilder"),
        arguments(
            "reading a file with java.io",
            "new java.io.FileReader(\"/etc/passwd\").read();",
            "java.io.FileReader"),
        arguments(
            "reading a file with java.nio",
            "java.nio.file.Files.readString(java.nio.file.Path.of(\"/etc/passwd\"));",
            "java.nio.file.Path.of"),
        arguments(
            "listing members by reflection",
            "Main.class.getDeclaredMethods();",
            "java.lang.Class.getDeclaredMethods"),
        arguments(
            "reflection from an object",
            "\"\".getClass().getClassLoader();",
            "java.lang.Class.getClassLoader"),
        arguments(
            "loading a class by name",
            "Class.forName(\"java.lang.Runtime\");",
            "java.lang.Class.forName"),
        arguments(
            "method handles",
            "java.lang.invoke.MethodHandles.lookup();",
            "java.lang.invoke.MethodHandles.lookup"),
        arguments("starting a thread", "new Thread(() -> {}).start();", "java.lang.Thread"),
        arguments(
            "a thread pool",
            "java.util.concurrent.Executors.newCachedThreadPool();",
            "java.util.concurrent.Executors.newCachedThreadPool"),
        arguments(
            "parallel work on the common pool",
            "java.util.Arrays.parallelSort(new int[] {2, 1});",
            "java.util.Arrays.parallelSort"),
        arguments(
            "opening a socket", "new java.net.Socket(\"example.com\", 80);", "java.net.Socket"),
        arguments(
            "an HTTP request",
            "new java.net.URI(\"http://example.com\").toURL().openStream();",
            "java.net.URI"),
        arguments(
            "a method reference to a forbidden method",
            "java.util.List.of(1).forEach(Runtime.getRuntime()::exit);",
            "java.lang.Runtime.getRuntime"),
        arguments(
            "a forbidden method behind a lambda",
            "java.util.List.of(1).forEach(code -> System.exit(code));",
            "java.lang.System.exit"),
        arguments(
            "a static method reference to a forbidden method",
            "java.util.List.of(1).forEach(System::exit);",
            "java.lang.System.exit"),
        arguments(
            "a class literal used to reach the class loader",
            "Object loader = Thread.class;",
            "java.lang.Thread"),
        arguments(
            "casting to a forbidden type",
            "Object o = null; Runnable r = (Runnable) o;",
            "java.lang.Runnable"),
        arguments(
            "catching a forbidden type",
            "try { int x = 1; } catch (java.io.UncheckedIOException e) { }",
            "java.io.UncheckedIOException"),
        arguments(
            "serialization", "new java.io.ObjectOutputStream(null);", "java.io.ObjectOutputStream"),
        arguments("reading with a scanner", "new java.util.Scanner(\"x\");", "java.util.Scanner"),
        arguments(
            "printing a stack trace to standard error",
            "new RuntimeException().printStackTrace();",
            "java.lang.RuntimeException.printStackTrace"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("forbiddenStatements")
  void rejectsForbiddenStatements(String attack, String statement, String symbol) {
    Trace trace = Traces.run(Programs.mainWithBody(statement));

    assertRejected(trace, symbol, "Main.java", 3);
  }

  @Test
  void rejectsAClassThatExtendsAForbiddenClass() {
    Trace trace =
        Traces.run(
            Programs.files(
                mainThatDoesNothing(),
                Programs.file(
                    "Trabajo.java",
                    """
                    public class Trabajo extends Thread {
                      @Override
                      public void run() {}
                    }
                    """)));

    assertRejected(trace, "java.lang.Thread", "Trabajo.java", 1);
  }

  @Test
  void rejectsAClassThatImplementsAForbiddenInterface() {
    Trace trace =
        Traces.run(
            Programs.files(
                mainThatDoesNothing(),
                Programs.file(
                    "Dato.java",
                    """
                    public class Dato implements java.io.Serializable {}
                    """)));

    assertRejected(trace, "java.io.Serializable", "Dato.java", 1);
  }

  @Test
  void rejectsNativeMethods() {
    Trace trace =
        Traces.run(
            Programs.main(
                """
                public class Main {
                  static native void escapar();

                  public static void main(String[] args) {}
                }
                """));

    assertRejected(trace, "native escapar", "Main.java", 1);
  }

  @Test
  void rejectsForbiddenCodeInAnyClassEvenIfNothingCallsIt() {
    Trace trace =
        Traces.run(
            Programs.files(
                mainThatDoesNothing(),
                Programs.file(
                    "Oculta.java",
                    """
                    public class Oculta {
                      static {
                        System.exit(1);
                      }
                    }
                    """)));

    assertRejected(trace, "java.lang.System.exit", "Oculta.java", 3);
  }

  @Test
  void rejectsAnAnnotationProcessorAndNeverRunsIt() {
    Trace trace =
        Traces.run(
            Programs.files(
                mainThatDoesNothing(),
                Programs.file(
                    "Procesador.java",
                    """
                    import java.util.Set;
                    import javax.annotation.processing.AbstractProcessor;
                    import javax.annotation.processing.RoundEnvironment;
                    import javax.annotation.processing.SupportedAnnotationTypes;
                    import javax.lang.model.element.TypeElement;

                    @SupportedAnnotationTypes("*")
                    public class Procesador extends AbstractProcessor {
                      static {
                        System.setProperty("ljbu.processor.ran", "true");
                      }

                      @Override
                      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
                        System.setProperty("ljbu.processor.ran", "true");
                        return false;
                      }
                    }
                    """)));

    assertThat(trace).isInstanceOf(Trace.Rejected.class);
    assertThat(((Trace.Rejected) trace).violations())
        .extracting(Trace.Violation::symbol)
        .contains("javax.annotation.processing.AbstractProcessor", "java.lang.System.setProperty");
    assertThat(System.getProperty("ljbu.processor.ran")).isNull();
  }

  @Test
  void reportsEveryForbiddenUseWithItsOwnLine() {
    Trace trace =
        Traces.run(
            Programs.mainWithBody(
                """
                String nombre = "UTN";
                System.getenv("HOME");
                Thread.sleep(10);
                """));

    assertThat(((Trace.Rejected) trace).violations())
        .containsExactly(
            new Trace.Violation("Main.java", 4, "java.lang.System.getenv"),
            new Trace.Violation("Main.java", 5, "java.lang.Thread.sleep"));
  }

  @Test
  void keepsTheStructureOfARejectedProgram() {
    Trace trace = Traces.run(Programs.mainWithBody("System.exit(0);"));

    assertThat(((Trace.Rejected) trace).structure().classes())
        .singleElement()
        .satisfies(type -> assertThat(type.name()).isEqualTo("Main"));
  }

  private static void assertRejected(Trace trace, String symbol, String file, int line) {
    assertThat(trace).as(Traces.toJson(trace)).isInstanceOf(Trace.Rejected.class);
    List<Trace.Violation> violations = ((Trace.Rejected) trace).violations();
    assertThat(violations).contains(new Trace.Violation(file, line, symbol));
  }

  private static SourceFile mainThatDoesNothing() {
    return Programs.file(
        "Main.java",
        """
        public class Main {
          public static void main(String[] args) {}
        }
        """);
  }
}
