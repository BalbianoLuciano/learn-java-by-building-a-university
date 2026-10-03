package io.github.balbianoluciano.ljbu.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.api.support.Contracts;
import io.github.balbianoluciano.ljbu.api.support.TestRunner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/** The public API, with the real content and the real runner behind it. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

  private static final Path ALIASING_SOLUTION =
      Path.of(
          "../../content/challenges/m1-clases-y-objetos/03-la-facultad-donde-estudias/solution/Main.java");

  @Autowired private MockMvcTester mvc;

  @DynamicPropertySource
  static void useTheTestRunner(DynamicPropertyRegistry registry) {
    registry.add("ljbu.api.runner-url", TestRunner::url);
    registry.add("ljbu.api.runner-token", () -> TestRunner.TOKEN);
    registry.add("ljbu.api.allowed-origins", () -> "https://curso.example");
  }

  @Test
  void reportsItsHealth() {
    assertThat(mvc.get().uri("/api/v1/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("ok");
  }

  @Test
  void listsModulesWithTheirChallengesInOrder() {
    MvcTestResult result = mvc.get().uri("/api/v1/modules").exchange();

    assertThat(result).hasStatusOk();
    Contracts.assertMatches("module-list.schema.json", body(result));
    assertThat(result).bodyJson().extractingPath("$.modules[0].id").isEqualTo("m1");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.modules[0].challenges[*].id")
        .asArray()
        .containsExactly("m1-01", "m1-02", "m1-03", "m1-04", "m1-05");
  }

  @Test
  void servesAChallengeWithoutItsChecksOrItsSolution() throws Exception {
    MvcTestResult result = mvc.get().uri("/api/v1/challenges/m1-03").exchange();

    assertThat(result).hasStatusOk();
    String body = body(result);
    Contracts.assertMatches("challenge-view.schema.json", body);
    assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("La facultad donde estudiás");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.files[?(@.path == 'Main.java')].editable")
        .asArray()
        .containsExactly(true);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.files[?(@.path == 'FacultadRegional.java')].editable")
        .asArray()
        .containsExactly(false);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.realReference.regionalFaculties[0].province")
        .isEqualTo("Chaco");
    assertThat(result).bodyJson().extractingPath("$.rules[0].id").isEqualTo("R-UNI-02");
    assertThat(body)
        .doesNotContain("miFacultad = resistencia")
        .doesNotContain("Asignar una referencia no copia");
  }

  @Test
  void answersNotFoundForAChallengeThatDoesNotExist() {
    MvcTestResult result = mvc.get().uri("/api/v1/challenges/m7-01").exchange();

    assertThat(result)
        .hasStatus(404)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("challenge_not_found");
  }

  @Test
  void servesHintsOneLevelAtATime() {
    MvcTestResult result = mvc.get().uri("/api/v1/challenges/m1-03/hints/2").exchange();

    assertThat(result).hasStatusOk();
    Contracts.assertMatches("hint.schema.json", body(result));
    assertThat(result)
        .bodyJson()
        .extractingPath("$.text")
        .isEqualTo("Declará `FacultadRegional miFacultad = …` sin usar `new`.");
    assertThat(mvc.get().uri("/api/v1/challenges/m1-03/hints/4")).hasStatus(404);
  }

  @Test
  void servesTheSolutionWithItsExplanation() {
    MvcTestResult result = mvc.get().uri("/api/v1/challenges/m1-03/solution").exchange();

    assertThat(result).hasStatusOk();
    Contracts.assertMatches("solution.schema.json", body(result));
    assertThat(body(result)).contains("miFacultad = resistencia").contains("aliasing");
  }

  @Test
  void runsAnAttemptAndAnswersWithTheResult() throws Exception {
    MvcTestResult result = run("m1-03", Files.readString(ALIASING_SOLUTION), "203.0.113.1");

    assertThat(result).hasStatusOk();
    Contracts.assertMatches("result.schema.json", body(result));
    assertThat(result).bodyJson().extractingPath("$.outcome").isEqualTo("passed");
    assertThat(result).bodyJson().extractingPath("$.progress.passed").isEqualTo(2);
  }

  @Test
  void runsTheStarterCodeWhenNoFileIsSent() {
    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/runs")
            .with(
                request -> {
                  request.setRemoteAddr("203.0.113.2");
                  return request;
                })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"challengeId\": \"m1-03\", \"files\": []}")
            .exchange();

    assertThat(result).hasStatusOk().bodyJson().extractingPath("$.outcome").isEqualTo("incomplete");
  }

  @Test
  void refusesToReplaceAReadOnlyFile() {
    MvcTestResult result =
        mvc.post()
            .uri("/api/v1/runs")
            .with(
                request -> {
                  request.setRemoteAddr("203.0.113.3");
                  return request;
                })
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"challengeId\": \"m1-03\", \"files\": [{\"path\": \"FacultadRegional.java\", \"content\": \"\"}]}")
            .exchange();

    assertThat(result)
        .hasStatus(400)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("not_editable");
  }

  @Test
  void refusesMoreCodeThanTheLimit() {
    String big = "//" + "x".repeat(70_000);

    MvcTestResult result = run("m1-03", big, "203.0.113.4");

    assertThat(result)
        .hasStatus(400)
        .bodyJson()
        .extractingPath("$.code")
        .isEqualTo("files_too_large");
  }

  @Test
  void answersNotFoundWhenRunningAChallengeThatDoesNotExist() {
    assertThat(run("m7-01", "", "203.0.113.5")).hasStatus(404);
  }

  @Test
  void allowsTheOriginOfTheWebAndNoOther() {
    MvcTestResult allowed =
        mvc.get()
            .uri("/api/v1/modules")
            .header(HttpHeaders.ORIGIN, "https://curso.example")
            .exchange();
    MvcTestResult stranger =
        mvc.get()
            .uri("/api/v1/modules")
            .header(HttpHeaders.ORIGIN, "https://otro.example")
            .exchange();

    assertThat(allowed)
        .hasStatusOk()
        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://curso.example");
    assertThat(stranger).hasStatus(403);
  }

  private MvcTestResult run(String challengeId, String main, String address) {
    String request =
        JsonMapper.builder()
            .build()
            .writeValueAsString(
                new Views.RunRequest(
                    challengeId,
                    java.util.List.of(
                        new io.github.balbianoluciano.ljbu.api.content.SourceFile(
                            "Main.java", main))));
    return mvc.post()
        .uri("/api/v1/runs")
        .with(
            http -> {
              http.setRemoteAddr(address);
              return http;
            })
        .contentType(MediaType.APPLICATION_JSON)
        .content(request)
        .exchange();
  }

  private static String body(MvcTestResult result) {
    try {
      return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    } catch (java.io.UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}
