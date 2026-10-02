package io.github.balbianoluciano.ljbu.api.runner;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the runner to execute code. The api never runs learner code itself, and it does not trust
 * what the runner answers: the answer has a size limit and must follow the trace contract
 * (docs/SECURITY.md §3, layer 5).
 */
public class RunnerClient {

  /** A full trace of 5000 steps stays well below this. */
  private static final int MAX_ANSWER_BYTES = 8 * 1024 * 1024;

  /** Compilation may take 10 s and the execution 5 s; beyond that the runner is not answering. */
  private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(30);

  private final RestClient client;
  private final Schema traceSchema = traceSchema();
  private final ObjectMapper mapper = JsonMapper.builder().build();

  public RunnerClient(String url, String token) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(ANSWER_TIMEOUT);
    this.client =
        RestClient.builder()
            .baseUrl(url)
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
  }

  private record ExecutionRequest(List<SourceFile> files, Limits limits) {}

  private record Limits(int timeoutMs) {}

  /** What the runner answered: a status and a body that has not been trusted yet. */
  private record Answer(int status, byte[] body) {}

  /**
   * @param timeoutMs timeout of the challenge, or null for the one of the runner
   * @throws RunnerBusyException if the runner has no free slot
   * @throws RunnerUnavailableException if the runner cannot be reached or answers nonsense
   */
  public Trace execute(List<SourceFile> files, Integer timeoutMs) {
    ExecutionRequest request =
        new ExecutionRequest(files, timeoutMs == null ? null : new Limits(timeoutMs));
    Answer answer;
    try {
      answer =
          client
              .post()
              .uri("/internal/v1/executions")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .exchange(
                  (sent, response) -> {
                    try (InputStream body = response.getBody()) {
                      return new Answer(
                          response.getStatusCode().value(), body.readNBytes(MAX_ANSWER_BYTES + 1));
                    }
                  });
    } catch (ResourceAccessException | UncheckedIOException e) {
      throw new RunnerUnavailableException("The runner cannot be reached", e);
    }
    if (answer.status() == 503) {
      throw new RunnerBusyException();
    }
    if (answer.status() != 200) {
      throw new RunnerUnavailableException("The runner answered " + answer.status(), null);
    }
    if (answer.body().length > MAX_ANSWER_BYTES) {
      throw new RunnerUnavailableException("The answer of the runner is too large", null);
    }
    String json = new String(answer.body(), StandardCharsets.UTF_8);
    try {
      List<Error> errors = traceSchema.validate(json, InputFormat.JSON);
      if (!errors.isEmpty()) {
        throw new RunnerUnavailableException(
            "The answer of the runner breaks the trace contract", null);
      }
      return mapper.readValue(json, Trace.class);
    } catch (RunnerUnavailableException e) {
      throw e;
    } catch (RuntimeException e) {
      // Not even JSON, or JSON that the validator or the mapper cannot read.
      throw new RunnerUnavailableException("The answer of the runner cannot be read", e);
    }
  }

  private static Schema traceSchema() {
    try (InputStream stream =
        RunnerClient.class.getResourceAsStream("/contracts/trace.schema.json")) {
      if (stream == null) {
        throw new IllegalStateException("trace.schema.json is not on the classpath");
      }
      return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
          .getSchema(new String(stream.readAllBytes(), StandardCharsets.UTF_8), InputFormat.JSON);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static class RunnerBusyException extends RuntimeException {}

  public static class RunnerUnavailableException extends RuntimeException {
    public RunnerUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
