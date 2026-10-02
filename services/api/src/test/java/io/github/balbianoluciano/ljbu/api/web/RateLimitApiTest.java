package io.github.balbianoluciano.ljbu.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.api.support.TestRunner;
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

@SpringBootTest(properties = "ljbu.api.rate-limit-runs-per-minute=2")
@AutoConfigureMockMvc
class RateLimitApiTest {

  @Autowired private MockMvcTester mvc;

  @DynamicPropertySource
  static void useTheTestRunner(DynamicPropertyRegistry registry) {
    registry.add("ljbu.api.runner-url", TestRunner::url);
    registry.add("ljbu.api.runner-token", () -> TestRunner.TOKEN);
  }

  @Test
  void asksTheLearnerToWaitAfterTheRunsOfTheMinute() {
    assertThat(run()).hasStatusOk();
    assertThat(run()).hasStatusOk();

    MvcTestResult third = run();

    assertThat(third)
        .hasStatus(429)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(third).bodyJson().extractingPath("$.code").isEqualTo("rate_limited");
    assertThat(third.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
  }

  private MvcTestResult run() {
    return mvc.post()
        .uri("/api/v1/runs")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"challengeId\": \"m1-01\", \"files\": []}")
        .exchange();
  }
}
