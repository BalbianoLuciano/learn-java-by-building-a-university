package io.github.balbianoluciano.ljbu.runner.execution.run;

import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The JVM that runs learner code (docs/SECURITY.md §3, layer 4): a new process per execution, with
 * memory limits, an empty environment and a debug port open only on the loopback interface.
 */
final class ChildJvm implements AutoCloseable {

  private static final Pattern LISTENING =
      Pattern.compile("Listening for transport dt_socket at address: (\\d+)");
  private static final long STARTUP_TIMEOUT_NANOS = 10_000_000_000L;

  private final Process process;
  private final int debugPort;

  private ChildJvm(Process process, int debugPort) {
    this.process = process;
    this.debugPort = debugPort;
  }

  static ChildJvm start(Path classDirectory, ExecutionLimits limits) throws IOException {
    List<String> command =
        List.of(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Xmx" + limits.heapMb() + "m",
            "-Xss" + limits.stackMb() + "m",
            "-XX:+UseSerialGC",
            "-XX:TieredStopAtLevel=1",
            "-XX:-UsePerfData",
            "-Xshare:auto",
            "--limit-modules=java.base,jdk.jdwp.agent",
            "-Dfile.encoding=UTF-8",
            "-Djava.io.tmpdir=" + classDirectory,
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:0",
            "-cp",
            classDirectory.toString(),
            "Main");
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().clear();
    builder.directory(classDirectory.toFile());
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    Process process = builder.start();
    process.getOutputStream().close();
    try {
      return new ChildJvm(process, readDebugPort(process));
    } catch (IOException | RuntimeException e) {
      kill(process);
      throw e;
    }
  }

  /** The agent announces its port on the first line of standard output. */
  private static int readDebugPort(Process process) throws IOException {
    InputStream stdout = process.getInputStream();
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    long deadline = System.nanoTime() + STARTUP_TIMEOUT_NANOS;
    while (System.nanoTime() < deadline) {
      if (stdout.available() > 0) {
        int next = stdout.read();
        if (next == '\n') {
          Matcher matcher = LISTENING.matcher(line.toString(StandardCharsets.UTF_8));
          if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
          }
          line.reset();
        } else if (next >= 0) {
          line.write(next);
        }
      } else if (!process.isAlive()) {
        break;
      } else {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    throw new IOException("The child JVM did not open its debug port");
  }

  int debugPort() {
    return debugPort;
  }

  InputStream stdout() {
    return process.getInputStream();
  }

  /** Kills the process and anything it may have started. */
  @Override
  public void close() {
    kill(process);
  }

  private static void kill(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }
}
