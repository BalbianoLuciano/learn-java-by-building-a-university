package io.github.balbianoluciano.ljbu.api.support;

import io.github.balbianoluciano.ljbu.api.runner.RunnerClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The real runner, started once for the whole test run. The api tests go through it: checks and
 * content are only worth testing against what the runner really reports.
 */
public final class TestRunner {

  public static final String TOKEN = "test-token";

  private static final Path JAR = Path.of("../runner/target/ljbu-runner.jar");
  private static String url;

  private TestRunner() {}

  public static synchronized String url() {
    if (url == null) {
      url = start();
    }
    return url;
  }

  public static RunnerClient client() {
    return new RunnerClient(url(), TOKEN);
  }

  private static String start() {
    if (!Files.isRegularFile(JAR)) {
      throw new IllegalStateException(
          "Build the runner first: (cd ../runner && ./mvnw -DskipTests package). Missing " + JAR);
    }
    try {
      int port;
      try (ServerSocket socket = new ServerSocket(0)) {
        port = socket.getLocalPort();
      }
      ProcessBuilder builder =
          new ProcessBuilder(
              Path.of(System.getProperty("java.home"), "bin", "java").toString(),
              "-jar",
              JAR.toString(),
              "--server.port=" + port);
      builder.environment().put("RUNNER_TOKEN", TOKEN);
      builder.redirectErrorStream(true);
      builder.redirectOutput(Path.of("target", "test-runner.log").toFile());
      Process process = builder.start();
      Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

      String address = "http://127.0.0.1:" + port;
      HttpClient http = HttpClient.newHttpClient();
      HttpRequest health = HttpRequest.newBuilder(URI.create(address + "/health")).build();
      long deadline = System.nanoTime() + 90_000_000_000L;
      while (System.nanoTime() < deadline) {
        if (!process.isAlive()) {
          throw new IllegalStateException("The runner stopped; see target/test-runner.log");
        }
        try {
          if (http.send(health, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
            return address;
          }
        } catch (IOException e) {
          // Not listening yet.
        }
        Thread.sleep(200);
      }
      throw new IllegalStateException("The runner did not start; see target/test-runner.log");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot start the runner", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
