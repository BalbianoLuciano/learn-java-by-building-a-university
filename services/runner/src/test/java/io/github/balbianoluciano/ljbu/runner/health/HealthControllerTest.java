package io.github.balbianoluciano.ljbu.runner.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

  @Autowired private MockMvcTester mvc;

  @Test
  void reportsOkStatus() {
    assertThat(mvc.get().uri("/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("ok");
  }
}
