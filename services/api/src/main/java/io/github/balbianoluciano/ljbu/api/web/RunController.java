package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.result.RunResult;
import io.github.balbianoluciano.ljbu.api.web.Views.RunRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RunController {

  private final RunService runs;
  private final RunLimiter limiter;

  public RunController(RunService runs, RunLimiter limiter) {
    this.runs = runs;
    this.limiter = limiter;
  }

  @PostMapping("/runs")
  public RunResult run(
      @RequestBody RunRequest request,
      @RequestParam(defaultValue = "es") String lang,
      HttpServletRequest http) {
    try (RunLimiter.Permit permit = limiter.acquire(http.getRemoteAddr())) {
      return runs.run(request.challengeId(), request.files(), lang);
    }
  }
}
