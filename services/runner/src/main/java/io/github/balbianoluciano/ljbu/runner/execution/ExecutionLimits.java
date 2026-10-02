package io.github.balbianoluciano.ljbu.runner.execution;

/** Limits of one execution (docs/SECURITY.md §3, layer 4). */
public record ExecutionLimits(
    int timeoutMs,
    int maxSteps,
    int maxObjects,
    int maxOutputBytes,
    int maxCallDepth,
    int heapMb,
    int stackMb) {

  public static final ExecutionLimits DEFAULT =
      new ExecutionLimits(5000, 5000, 500, 16 * 1024, 1000, 64, 1);

  /** A request can lower the timeout, never raise it. */
  public ExecutionLimits withTimeoutMs(int requested) {
    int capped = Math.clamp(requested, 1, timeoutMs);
    return new ExecutionLimits(
        capped, maxSteps, maxObjects, maxOutputBytes, maxCallDepth, heapMb, stackMb);
  }
}
