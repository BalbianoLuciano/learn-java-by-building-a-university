package io.github.balbianoluciano.ljbu.runner.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest(properties = "ljbu.runner.token=test-token")
@AutoConfigureMockMvc
class ExecutionControllerTest {

  private static final String EXECUTIONS = "/internal/v1/executions";

  /** The example of the contract: packages/contracts/examples/execution-request.json. */
  private static final Path EXAMPLE_REQUEST =
      Path.of("../../packages/contracts/examples/execution-request.json");

  @Autowired private MockMvcTester mvc;

  @Test
  void runsTheExampleRequestAndAnswersWithATraceOfTheContract() throws Exception {
    MvcTestResult result = post(Files.readString(EXAMPLE_REQUEST), "Bearer test-token");

    assertThat(result).hasStatusOk();
    String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Traces.assertMatchesSchema(body);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("completed");
    assertThat(result).bodyJson().extractingPath("$.stdout").isEqualTo("Hola, UTN\n");
  }

  @Test
  void rejectsARequestWithoutToken() {
    assertThat(post("{\"files\": []}", null)).hasStatus(401);
  }

  @Test
  void rejectsARequestWithAnotherToken() {
    assertThat(post("{\"files\": []}", "Bearer other-token")).hasStatus(401);
  }

  @Test
  void explainsWhyARequestIsInvalid() {
    MvcTestResult result =
        post(
            """
            {"files": [{"path": "../Main.java", "content": ""}]}
            """,
            "Bearer test-token");

    assertThat(result)
        .hasStatus(400)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invalid_file_name");
  }

  @Test
  void answersHealthChecksWithoutToken() {
    assertThat(mvc.get().uri("/health")).hasStatusOk();
  }

  private MvcTestResult post(String body, String authorization) {
    MockMvcTester.MockMvcRequestBuilder request =
        mvc.post().uri(EXECUTIONS).contentType(MediaType.APPLICATION_JSON).content(body);
    if (authorization != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, authorization);
    }
    return request.exchange();
  }
}
