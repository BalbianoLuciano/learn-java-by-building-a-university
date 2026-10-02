package io.github.balbianoluciano.ljbu.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.balbianoluciano.ljbu.api.web.RunLimiter.RateLimitedException;
import org.junit.jupiter.api.Test;

class RunLimiterTest {

  private static final String LEARNER = "203.0.113.10";
  private static final String ANOTHER_LEARNER = "203.0.113.11";

  @Test
  void allowsTheConfiguredRunsPerMinuteAndThenAsksToWait() {
    RunLimiter limiter = new RunLimiter(2);
    limiter.acquire(LEARNER).close();
    limiter.acquire(LEARNER).close();

    assertThatThrownBy(() -> limiter.acquire(LEARNER))
        .isInstanceOfSatisfying(
            RateLimitedException.class,
            limited -> {
              assertThat(limited.code()).isEqualTo("rate_limited");
              assertThat(limited.retryAfterSeconds()).isBetween(1L, 60L);
            });
  }

  @Test
  void countsEachAddressOnItsOwn() {
    RunLimiter limiter = new RunLimiter(1);
    limiter.acquire(LEARNER).close();

    assertThatCode(() -> limiter.acquire(ANOTHER_LEARNER).close()).doesNotThrowAnyException();
  }

  @Test
  void allowsOneRunAtATimePerAddress() {
    RunLimiter limiter = new RunLimiter(10);

    try (RunLimiter.Permit running = limiter.acquire(LEARNER)) {
      assertThatThrownBy(() -> limiter.acquire(LEARNER))
          .isInstanceOfSatisfying(
              RateLimitedException.class,
              limited -> assertThat(limited.code()).isEqualTo("run_in_progress"));
      assertThatCode(() -> limiter.acquire(ANOTHER_LEARNER).close()).doesNotThrowAnyException();
    }

    assertThatCode(() -> limiter.acquire(LEARNER).close()).doesNotThrowAnyException();
  }

  @Test
  void aRunTurnedAwayForBeingTooEarlyDoesNotBlockTheNextOne() {
    RunLimiter limiter = new RunLimiter(1);
    limiter.acquire(LEARNER).close();
    assertThatThrownBy(() -> limiter.acquire(LEARNER)).isInstanceOf(RateLimitedException.class);

    assertThatThrownBy(() -> limiter.acquire(LEARNER))
        .isInstanceOfSatisfying(
            RateLimitedException.class,
            limited -> assertThat(limited.code()).isEqualTo("rate_limited"));
  }
}
