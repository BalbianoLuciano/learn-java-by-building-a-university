package io.github.balbianoluciano.ljbu.api.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.api.content.Challenge.ExpectedEntry;
import io.github.balbianoluciano.ljbu.api.content.Challenge.Variant;
import io.github.balbianoluciano.ljbu.api.result.RunResult;
import io.github.balbianoluciano.ljbu.api.result.RunResult.LogEntry;
import io.github.balbianoluciano.ljbu.api.support.TestContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Content tests (docs/specs/challenge-format.md): for every challenge of content/, the solution
 * passes every check, the starter code does not, and each variant of its tests folder gets the
 * feedback it expects.
 */
class ContentTest {

  private static final Content CONTENT = TestContent.real();

  @TestFactory
  Stream<DynamicTest> everyChallengeBehavesAsItsAuthorExpects() {
    List<DynamicTest> tests = new ArrayList<>();
    for (Challenge challenge : CONTENT.challenges().values()) {
      String id = challenge.id();
      tests.add(
          DynamicTest.dynamicTest(
              id + ": the solution passes every check", () -> solutionPasses(challenge)));
      tests.add(
          DynamicTest.dynamicTest(
              id + ": the starter code runs and does not pass",
              () -> starterDoesNotPass(challenge)));
      for (Map.Entry<String, Variant> variant : challenge.variants().entrySet()) {
        tests.add(
            DynamicTest.dynamicTest(
                id + ": variant " + variant.getKey(),
                () -> variantGetsItsFeedback(challenge, variant.getValue())));
      }
    }
    return tests.stream();
  }

  private static void solutionPasses(Challenge challenge) {
    RunResult result =
        TestContent.run(CONTENT, challenge.id(), editable(challenge, challenge.solution()));

    assertThat(result.log())
        .as("log of the solution")
        .allSatisfy(
            entry -> assertThat(entry.state()).as(entry.title()).isEqualTo(RunResult.PASSED));
    assertThat(result.outcome()).isEqualTo(RunResult.PASSED);
    assertThat(result.progress().passed()).isEqualTo(challenge.checks().size());
    assertThat(result.pieces())
        .allSatisfy(piece -> assertThat(piece.built()).as(piece.id()).isTrue());
  }

  private static void starterDoesNotPass(Challenge challenge) {
    RunResult result = TestContent.run(CONTENT, challenge.id(), List.of());

    // The starter must compile and run: what is missing is the work of the learner.
    assertThat(result.outcome()).as(result.log().toString()).isEqualTo(RunResult.INCOMPLETE);
    assertThat(result.log()).allSatisfy(entry -> assertThat(entry.checkId()).isNotNull());
  }

  private static void variantGetsItsFeedback(Challenge challenge, Variant variant) {
    RunResult result = TestContent.run(CONTENT, challenge.id(), variant.files());

    assertThat(result.outcome())
        .as(result.log().toString())
        .isEqualTo(variant.expected().outcome());
    for (ExpectedEntry expected : variant.expected().log()) {
      LogEntry entry =
          result.log().stream()
              .filter(candidate -> expected.check().equals(candidate.checkId()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("No log entry for check " + expected.check()));
      assertThat(entry.state()).as(expected.check()).isEqualTo(expected.state());
      if (expected.title() != null) {
        assertThat(entry.title()).isEqualTo(expected.title());
      }
    }
  }

  /** What a learner would send: only the editable files. */
  private static List<SourceFile> editable(Challenge challenge, List<SourceFile> files) {
    return files.stream().filter(file -> challenge.isEditable(file.path())).toList();
  }
}
