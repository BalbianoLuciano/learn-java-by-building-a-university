package io.github.balbianoluciano.ljbu.runner.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param token shared secret the api sends on every call (RUNNER_TOKEN)
 * @param maxConcurrent executions that run at the same time
 * @param queueCapacity executions that may wait; beyond that the runner answers 503
 */
@ConfigurationProperties("ljbu.runner")
public record RunnerProperties(String token, int maxConcurrent, int queueCapacity) {

  public RunnerProperties {
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("RUNNER_TOKEN is required");
    }
    if (maxConcurrent < 1 || queueCapacity < 1) {
      throw new IllegalStateException("The runner needs at least one worker and one queue slot");
    }
  }
}
