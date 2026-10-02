package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.ContentLoader;
import io.github.balbianoluciano.ljbu.api.result.ResultBuilder;
import io.github.balbianoluciano.ljbu.api.runner.RunnerClient;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiConfiguration implements WebMvcConfigurer {

  private final ApiProperties properties;

  public ApiConfiguration(ApiProperties properties) {
    this.properties = properties;
  }

  /** Loaded once at startup: an invalid challenge stops the api from starting. */
  @Bean
  Content content() {
    return new ContentLoader().load(Path.of(properties.contentDir()));
  }

  @Bean
  ResultBuilder resultBuilder(Content content) {
    return new ResultBuilder(content.feedback());
  }

  @Bean
  RunnerClient runnerClient() {
    return new RunnerClient(properties.runnerUrl(), properties.runnerToken());
  }

  @Bean
  RunLimiter runLimiter() {
    return new RunLimiter(properties.rateLimitRunsPerMinute());
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "POST");
  }
}
