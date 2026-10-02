package io.github.balbianoluciano.ljbu.runner.web;

import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import io.github.balbianoluciano.ljbu.runner.execution.ExecutionReport;
import io.github.balbianoluciano.ljbu.runner.execution.Executor;
import io.github.balbianoluciano.ljbu.runner.execution.InputValidator;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionController {

  private static final Logger log = LoggerFactory.getLogger(ExecutionController.class);

  private final Executor executor;
  private final ExecutionQueue queue;

  public ExecutionController(Executor executor, ExecutionQueue queue) {
    this.executor = executor;
    this.queue = queue;
  }

  @PostMapping("/internal/v1/executions")
  public Trace execute(@RequestBody ExecutionRequest request) throws InterruptedException {
    // Checked before queueing: a request that cannot run must not take a slot.
    InputValidator.validate(request.files());
    ExecutionLimits limits = ExecutionLimits.DEFAULT;
    if (request.limits() != null && request.limits().timeoutMs() != null) {
      limits = limits.withTimeoutMs(request.limits().timeoutMs());
    }
    ExecutionLimits applied = limits;
    ExecutionReport report = queue.run(() -> executor.execute(request.files(), applied));
    // Learner code and its output are never logged (AGENTS.md).
    log.info(
        "execution status={} compileMs={} verifyMs={} runMs={}",
        report.trace().status(),
        report.compileMs(),
        report.verifyMs(),
        report.runMs());
    return report.trace();
  }
}
