package io.github.balbianoluciano.ljbu.runner.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import io.github.balbianoluciano.ljbu.runner.execution.Executor;
import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import io.github.balbianoluciano.ljbu.runner.execution.trace.HeapObject;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Step;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** Runs programs and checks every trace against the contract. */
public final class Traces {

  /** The contract shared with the api: packages/contracts/trace.schema.json. */
  private static final Path SCHEMA_FILE = Path.of("../../packages/contracts/trace.schema.json");

  private static final Schema SCHEMA = loadSchema();
  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Executor EXECUTOR = new Executor();

  private Traces() {}

  public static Trace run(List<SourceFile> files) {
    return run(files, ExecutionLimits.DEFAULT);
  }

  /** Executes the program; whatever the outcome, the trace must honor the contract. */
  public static Trace run(List<SourceFile> files, ExecutionLimits limits) {
    try {
      Trace trace = EXECUTOR.execute(files, limits).trace();
      assertMatchesSchema(toJson(trace));
      if (trace instanceof Trace.Executed executed) {
        assertEveryReferenceResolves(executed);
      }
      return trace;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  public static Trace.Executed runToTheEnd(List<SourceFile> files) {
    Trace trace = run(files);
    assertThat(trace).as(toJson(trace)).isInstanceOf(Trace.Executed.class);
    return (Trace.Executed) trace;
  }

  public static String toJson(Trace trace) {
    return MAPPER.writeValueAsString(trace);
  }

  public static void assertMatchesSchema(String json) {
    List<Error> errors = SCHEMA.validate(json, InputFormat.JSON);
    assertThat(errors).as("violations of trace.schema.json in %s", json).isEmpty();
  }

  private static void assertEveryReferenceResolves(Trace.Executed trace) {
    List<Value> values = new ArrayList<>();
    for (Step step : trace.steps()) {
      switch (step) {
        case Step.FieldSet write -> values.add(write.value());
        case Step.LocalSet write -> values.add(write.value());
        case Step.Call call -> values.addAll(call.args());
        case Step.Return returned -> values.add(returned.value());
        default -> {}
      }
    }
    for (HeapObject object : trace.heap().values()) {
      switch (object) {
        case HeapObject.Instance instance -> values.addAll(instance.fields().values());
        case HeapObject.Sequence sequence -> values.addAll(sequence.elements());
        case HeapObject.MapObject map ->
            map.entries()
                .forEach(
                    entry -> {
                      values.add(entry.key());
                      values.add(entry.value());
                    });
      }
    }
    trace.statics().values().forEach(fields -> values.addAll(fields.values()));
    for (Value value : values) {
      if (value instanceof Value.RefValue reference) {
        assertThat(trace.heap()).containsKey(reference.id());
      }
    }
  }

  private static Schema loadSchema() {
    try {
      return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
          .getSchema(Files.readString(SCHEMA_FILE), InputFormat.JSON);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
