package io.github.balbianoluciano.ljbu.runner.execution.run;

import io.github.balbianoluciano.ljbu.runner.execution.trace.HeapObject;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Step;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import java.util.List;
import java.util.Map;

/** What the child JVM did, as observed through JDI. */
public record RunOutcome(
    Trace.Status status,
    String stdout,
    List<Step> steps,
    Map<String, HeapObject> heap,
    Map<String, Map<String, Value>> statics,
    Trace.ExceptionInfo exception,
    Trace.Limits limits) {}
