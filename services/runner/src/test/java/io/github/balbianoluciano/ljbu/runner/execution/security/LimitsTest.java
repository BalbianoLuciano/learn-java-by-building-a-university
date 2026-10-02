package io.github.balbianoluciano.ljbu.runner.execution.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Step;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Programs that would exhaust the runner and must be cut by the limits of the child JVM
 * (docs/SECURITY.md §5).
 */
class LimitsTest {

  private static final ExecutionLimits LIMITS = ExecutionLimits.DEFAULT;
  private static final ExecutionLimits ONE_SECOND = LIMITS.withTimeoutMs(1000);

  /** The default step limit with fewer steps: how long 5000 steps take depends on the machine. */
  private static final ExecutionLimits FEW_STEPS =
      new ExecutionLimits(
          LIMITS.timeoutMs(),
          300,
          LIMITS.maxObjects(),
          LIMITS.maxOutputBytes(),
          LIMITS.maxCallDepth(),
          LIMITS.heapMb(),
          LIMITS.stackMb());

  @Test
  void cutsAnInfiniteLoopAtTheTimeout() {
    long start = System.nanoTime();

    Trace.Executed trace = run(Programs.mainWithBody("while (true) {}"), ONE_SECOND);

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.TIMEOUT);
    assertThat(trace.limits().truncated()).isTrue();
    assertThat(elapsedMs(start)).isLessThan(4000);
  }

  @Test
  void cutsAnInfiniteLoopThatKeepsWorkingAtTheStepLimit() {
    Trace.Executed trace =
        run(
            Programs.mainWithBody(
                """
                int vueltas = 0;
                while (true) {
                  vueltas++;
                  int doble = vueltas * 2;
                }
                """),
            FEW_STEPS);

    assertStoppedBy(trace, Trace.ExceededLimit.STEPS);
    assertThat(trace.steps()).hasSize(FEW_STEPS.maxSteps());
  }

  @Test
  void cutsInfiniteRecursion() {
    long start = System.nanoTime();

    Trace.Executed trace =
        run(
            Programs.main(
                """
                public class Main {
                  static int bajar(int nivel) {
                    return bajar(nivel + 1);
                  }

                  public static void main(String[] args) {
                    bajar(0);
                  }
                }
                """),
            LIMITS);

    assertStoppedBy(trace, Trace.ExceededLimit.CALL_DEPTH);
    assertThat(elapsedMs(start)).isLessThan(4000);
  }

  @Test
  void runsRecursionThatEnds() {
    Trace.Executed trace =
        run(
            Programs.main(
                """
                public class Main {
                  static int sumarHasta(int n) {
                    return n == 0 ? 0 : n + sumarHasta(n - 1);
                  }

                  public static void main(String[] args) {
                    System.out.println(sumarHasta(100));
                  }
                }
                """),
            LIMITS);

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.COMPLETED);
    assertThat(trace.stdout()).isEqualTo("5050\n");
  }

  @Test
  void endsWithAnErrorWhenOneAllocationDoesNotFitInTheHeap() {
    Trace.Executed trace =
        run(Programs.mainWithBody("long[] enorme = new long[50_000_000];"), LIMITS);

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.RUNTIME_ERROR);
    assertThat(trace.exception().type()).isEqualTo("java.lang.OutOfMemoryError");
    assertThat(trace.exception().line()).isEqualTo(3);
  }

  @Test
  void endsWithAnErrorWhenTheHeapFillsUpLittleByLittle() {
    Trace.Executed trace =
        run(
            Programs.mainWithBody(
                """
                java.util.List<long[]> bloques = new java.util.ArrayList<>();
                while (true) bloques.add(new long[100_000]);
                """),
            LIMITS);

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.RUNTIME_ERROR);
    assertThat(trace.exception().type()).isEqualTo("java.lang.OutOfMemoryError");
  }

  @Test
  void cutsInfiniteOutput() {
    Trace.Executed trace =
        run(
            Programs.mainWithBody(
                """
                String linea = "x".repeat(200);
                while (true) System.out.println(linea);
                """),
            LIMITS);

    assertStoppedBy(trace, Trace.ExceededLimit.OUTPUT);
    assertThat(trace.stdout().getBytes(StandardCharsets.UTF_8)).hasSize(LIMITS.maxOutputBytes());
  }

  @Test
  void cutsAProgramThatCreatesTooManyObjects() {
    Trace.Executed trace =
        run(
            Programs.files(
                Programs.file("Banca.java", "public class Banca {}"),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {
                        while (true) new Banca();
                      }
                    }
                    """)),
            LIMITS);

    assertStoppedBy(trace, Trace.ExceededLimit.OBJECTS);
    assertThat(trace.steps().stream().filter(Step.ObjectCreated.class::isInstance))
        .hasSize(LIMITS.maxObjects());
  }

  @Test
  void keepsWhatTheProgramDidBeforeBeingCut() {
    Trace.Executed trace =
        run(
            Programs.mainWithBody(
                """
                System.out.println("empieza");
                while (true) {}
                """),
            ONE_SECOND);

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.TIMEOUT);
    assertThat(trace.stdout()).isEqualTo("empieza\n");
    assertThat(trace.steps()).isNotEmpty();
  }

  @Test
  void aRequestCanLowerTheTimeoutButNotRaiseIt() {
    assertThat(LIMITS.withTimeoutMs(200).timeoutMs()).isEqualTo(200);
    assertThat(LIMITS.withTimeoutMs(60_000).timeoutMs()).isEqualTo(LIMITS.timeoutMs());
    assertThat(LIMITS.withTimeoutMs(0).timeoutMs()).isEqualTo(1);
  }

  private static Trace.Executed run(List<SourceFile> files, ExecutionLimits limits) {
    Trace trace = Traces.run(files, limits);
    assertThat(trace).as(Traces.toJson(trace)).isInstanceOf(Trace.Executed.class);
    return (Trace.Executed) trace;
  }

  private static void assertStoppedBy(Trace.Executed trace, Trace.ExceededLimit limit) {
    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.LIMIT_EXCEEDED);
    assertThat(trace.limits().exceeded()).isEqualTo(limit);
    assertThat(trace.limits().truncated()).isTrue();
  }

  private static long elapsedMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }
}
