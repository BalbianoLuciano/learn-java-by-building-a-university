package io.github.balbianoluciano.ljbu.runner.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The trace shown in the contract and in docs/ARCHITECTURE.md §5.2 is what the runner really
 * produces for the aliasing program. If the trace changes on purpose, regenerate the example with
 * {@code -Dljbu.updateExamples=true}.
 */
class ContractExampleTest {

  private static final Path EXAMPLE =
      Path.of("../../packages/contracts/examples/trace.completed.json");
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void producesTheExampleTraceOfTheContract() throws Exception {
    String produced = Traces.toJson(Traces.run(Programs.named("aliasing")));

    if (Boolean.getBoolean("ljbu.updateExamples")) {
      Files.writeString(
          EXAMPLE,
          MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(produced))
              + "\n");
    }

    assertThat(MAPPER.readTree(produced)).isEqualTo(MAPPER.readTree(Files.readString(EXAMPLE)));
  }
}
