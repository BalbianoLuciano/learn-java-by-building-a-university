package io.github.balbianoluciano.ljbu.api.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the api (docs/ARCHITECTURE.md §9).
 *
 * @param contentDir folder with challenges, domain data and feedback catalogs (CONTENT_DIR)
 * @param runnerUrl private URL of the runner (RUNNER_URL)
 * @param runnerToken secret shared with the runner (RUNNER_TOKEN)
 * @param allowedOrigins origins the web is served from (ALLOWED_ORIGINS)
 * @param rateLimitRunsPerMinute executions a single address may ask for per minute
 */
@ConfigurationProperties("ljbu.api")
public record ApiProperties(
    String contentDir,
    String runnerUrl,
    String runnerToken,
    List<String> allowedOrigins,
    int rateLimitRunsPerMinute) {

  public ApiProperties {
    if (runnerToken == null || runnerToken.isBlank()) {
      throw new IllegalStateException("RUNNER_TOKEN is required");
    }
    if (rateLimitRunsPerMinute < 1) {
      throw new IllegalStateException("RATE_LIMIT_RUNS_PER_MINUTE must be at least 1");
    }
  }
}
