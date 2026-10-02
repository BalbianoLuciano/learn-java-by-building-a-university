package io.github.balbianoluciano.ljbu.runner.web;

import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import java.util.List;

/** Body of POST /internal/v1/executions (packages/contracts/execution-request.schema.json). */
public record ExecutionRequest(List<SourceFile> files, Limits limits) {

  public record Limits(Integer timeoutMs) {}
}
