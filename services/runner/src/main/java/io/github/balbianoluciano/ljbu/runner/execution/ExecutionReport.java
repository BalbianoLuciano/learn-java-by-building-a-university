package io.github.balbianoluciano.ljbu.runner.execution;

import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;

/**
 * The trace of an execution and how long each stage took, in milliseconds. A stage that did not run
 * takes 0.
 */
public record ExecutionReport(Trace trace, long compileMs, long verifyMs, long runMs) {}
