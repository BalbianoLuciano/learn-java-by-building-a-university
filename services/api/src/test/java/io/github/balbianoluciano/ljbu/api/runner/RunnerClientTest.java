package io.github.balbianoluciano.ljbu.api.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.runner.RunnerClient.RunnerBusyException;
import io.github.balbianoluciano.ljbu.api.runner.RunnerClient.RunnerUnavailableException;
import io.github.balbianoluciano.ljbu.api.support.Programs;
import io.github.balbianoluciano.ljbu.api.support.TestRunner;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The api does not trust the runner: what it answers must follow the contract. */
class RunnerClientTest {

  private static final List<SourceFile> PROGRAM = List.of(Programs.mainWithBody("int cupos = 40;"));

  private HttpServer impostor;

  @AfterEach
  void stopTheImpostor() {
    if (impostor != null) {
      impostor.stop(0);
    }
  }

  @Test
  void readsTheTraceOfTheRealRunner() {
    Trace trace = TestRunner.client().execute(PROGRAM, null);

    assertThat(trace)
        .isInstanceOfSatisfying(
            Trace.Executed.class, executed -> assertThat(executed.completed()).isTrue());
  }

  @Test
  void passesTheTimeoutOfTheChallengeToTheRunner() {
    List<SourceFile> endless = List.of(Programs.mainWithBody("while (true) {}"));
    long start = System.nanoTime();

    Trace trace = TestRunner.client().execute(endless, 500);

    assertThat(trace.status()).isEqualTo("timeout");
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(4000);
  }

  @Test
  void failsWhenTheTokenIsNotTheOneOfTheRunner() {
    RunnerClient stranger = new RunnerClient(TestRunner.url(), "another-token");

    assertThatThrownBy(() -> stranger.execute(PROGRAM, null))
        .isInstanceOf(RunnerUnavailableException.class);
  }

  @Test
  void failsWhenTheRunnerCannotBeReached() {
    RunnerClient nobody = new RunnerClient("http://127.0.0.1:1", TestRunner.TOKEN);

    assertThatThrownBy(() -> nobody.execute(PROGRAM, null))
        .isInstanceOf(RunnerUnavailableException.class);
  }

  @Test
  void rejectsAnAnswerThatBreaksTheTraceContract() throws IOException {
    RunnerClient client =
        clientOfImpostorAnswering(200, "{\"status\": \"completed\", \"stdout\": \"hola\"}");

    assertThatThrownBy(() -> client.execute(PROGRAM, null))
        .isInstanceOf(RunnerUnavailableException.class)
        .hasMessageContaining("breaks the trace contract");
  }

  @Test
  void rejectsAnAnswerThatIsNotJson() throws IOException {
    RunnerClient client = clientOfImpostorAnswering(200, "<html>runner</html>");

    assertThatThrownBy(() -> client.execute(PROGRAM, null))
        .isInstanceOf(RunnerUnavailableException.class);
  }

  @Test
  void tellsABusyRunnerFromABrokenOne() throws IOException {
    RunnerClient client = clientOfImpostorAnswering(503, "{}");

    assertThatThrownBy(() -> client.execute(PROGRAM, null)).isInstanceOf(RunnerBusyException.class);
  }

  private RunnerClient clientOfImpostorAnswering(int status, String body) throws IOException {
    impostor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    impostor.createContext(
        "/",
        exchange -> {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    impostor.start();
    return new RunnerClient(
        "http://127.0.0.1:" + impostor.getAddress().getPort(), TestRunner.TOKEN);
  }
}
