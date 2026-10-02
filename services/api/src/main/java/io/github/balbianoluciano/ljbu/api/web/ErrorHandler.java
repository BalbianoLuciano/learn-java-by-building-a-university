package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.runner.RunnerClient;
import io.github.balbianoluciano.ljbu.api.web.RunLimiter.RateLimitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errors travel as application/problem+json (RFC 9457) with a machine-readable code. */
@RestControllerAdvice
public class ErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException exception) {
    return ResponseEntity.status(exception.status())
        .body(problem(exception.status(), exception.code(), exception.getMessage()));
  }

  @ExceptionHandler(RateLimitedException.class)
  ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
        .body(
            problem(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.code(),
                "Too many executions from this address; try again shortly."));
  }

  @ExceptionHandler(RunnerClient.RunnerBusyException.class)
  ProblemDetail busy() {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "runner_busy",
        "Too many executions right now; try again shortly.");
  }

  @ExceptionHandler(RunnerClient.RunnerUnavailableException.class)
  ProblemDetail unavailable(RunnerClient.RunnerUnavailableException exception) {
    log.error("runner unavailable: {}", exception.getMessage());
    return problem(HttpStatus.BAD_GATEWAY, "runner_unavailable", "The code could not be executed.");
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("code", code);
    return problem;
  }
}
