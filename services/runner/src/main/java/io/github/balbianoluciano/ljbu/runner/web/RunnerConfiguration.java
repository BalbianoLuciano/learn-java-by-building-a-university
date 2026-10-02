package io.github.balbianoluciano.ljbu.runner.web;

import io.github.balbianoluciano.ljbu.runner.execution.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RunnerProperties.class)
public class RunnerConfiguration {

  @Bean
  Executor executor() {
    return new Executor();
  }

  @Bean(destroyMethod = "close")
  ExecutionQueue executionQueue(RunnerProperties properties) {
    return new ExecutionQueue(properties.maxConcurrent(), properties.queueCapacity());
  }

  @Bean
  FilterRegistrationBean<RunnerTokenFilter> runnerTokenFilter(RunnerProperties properties) {
    FilterRegistrationBean<RunnerTokenFilter> registration =
        new FilterRegistrationBean<>(new RunnerTokenFilter(properties.token()));
    registration.addUrlPatterns("/internal/*");
    return registration;
  }
}
