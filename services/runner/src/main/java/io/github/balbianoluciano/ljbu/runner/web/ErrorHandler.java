package io.github.balbianoluciano.ljbu.runner.web;

import io.github.balbianoluciano.ljbu.runner.execution.InvalidInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errors travel as application/problem+json (RFC 9457) with a machine-readable code. */
@RestControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(InvalidInputException.class)
  ProblemDetail invalidInput(InvalidInputException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setProperty("code", exception.code());
    return problem;
  }

  @ExceptionHandler(ExecutionQueue.RunnerBusyException.class)
  ProblemDetail busy() {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE, "The runner is at capacity; try again shortly.");
    problem.setProperty("code", "runner_busy");
    return problem;
  }
}
