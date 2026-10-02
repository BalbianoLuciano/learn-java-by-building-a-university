package io.github.balbianoluciano.ljbu.api.result;

import static io.github.balbianoluciano.ljbu.api.support.Programs.file;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.api.content.Challenge;
import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.RunResult.LogEntry;
import io.github.balbianoluciano.ljbu.api.result.RunResult.Piece;
import io.github.balbianoluciano.ljbu.api.result.RunResult.Slot;
import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.result.RunResult.TimelineStep;
import io.github.balbianoluciano.ljbu.api.support.TestContent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pieces, log and timeline of real runs. Every result is also checked against its contract. */
class ResultBuilderTest {

  private static final String ALIASING = "m1-03";
  private static final String COMPOSITION = "m9-01";

  private static Content content;
  private static Content fixtures;

  @BeforeAll
  static void loadContent(@TempDir Path temporary) {
    content = TestContent.real();
    fixtures = TestContent.fixtures(temporary);
  }

  @Test
  void aSolvedChallengeHasItsPiecesBuiltAndEveryCheckPassed() {
    RunResult result = runSolution(content, ALIASING);

    assertThat(result.outcome()).isEqualTo(RunResult.PASSED);
    assertThat(result.progress()).isEqualTo(new RunResult.Progress(2, 2));
    assertThat(piece(result, "fr-resistencia"))
        .isEqualTo(
            new Piece(
                "fr-resistencia",
                "regional-faculty",
                RunResult.PASSED,
                true,
                "Resistencia",
                new SourceRef("Main.java", 3),
                null,
                null));
    assertThat(result.log())
        .extracting(LogEntry::checkId)
        .containsExactly("alias-declared", "province-through-alias");
  }

  @Test
  void variablesOfMainArePiecesThatPointToTheirObject() {
    RunResult result = runSolution(content, ALIASING);

    assertThat(result.pieces())
        .filteredOn(piece -> piece.archetype().equals("variable-sign"))
        .extracting(Piece::id, Piece::label, Piece::target)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("var-resistencia", "resistencia", "fr-resistencia"),
            org.assertj.core.groups.Tuple.tuple("var-miFacultad", "miFacultad", "fr-resistencia"));
  }

  @Test
  void theTimelineTiesEveryStepToItsLineAndItsPieces() {
    RunResult result = runSolution(content, ALIASING);

    TimelineStep aliasDeclared =
        result.timeline().stream()
            .filter(step -> step.event().equals("local_set") && step.name().equals("miFacultad"))
            .findFirst()
            .orElseThrow();
    TimelineStep created =
        result.timeline().stream()
            .filter(step -> step.event().equals("object_created"))
            .findFirst()
            .orElseThrow();

    assertThat(aliasDeclared.sourceRef()).isEqualTo(new SourceRef("Main.java", 7));
    assertThat(aliasDeclared.pieceId()).isEqualTo("var-miFacultad");
    assertThat(aliasDeclared.targetPieceId()).isEqualTo("fr-resistencia");
    assertThat(created.pieceId()).isEqualTo("fr-resistencia");
    assertThat(result.timeline()).extracting(TimelineStep::index).isSorted();
  }

  @Test
  void whatIsMissingComesBeforeWhatIsRight() {
    RunResult result =
        run(
            content,
            ALIASING,
            """
            FacultadRegional resistencia = new FacultadRegional();
            resistencia.nombre = "Resistencia";
            FacultadRegional miFacultad = resistencia;
            """);

    assertThat(result.outcome()).isEqualTo(RunResult.INCOMPLETE);
    assertThat(result.progress()).isEqualTo(new RunResult.Progress(1, 2));
    assertThat(result.log())
        .extracting(LogEntry::checkId, LogEntry::state)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("province-through-alias", RunResult.INCOMPLETE),
            org.assertj.core.groups.Tuple.tuple("alias-declared", RunResult.PASSED));
    assertThat(result.log().getFirst().hint()).isNotBlank();
    assertThat(result.log().getLast().hint()).isNull();
    assertThat(piece(result, "fr-resistencia").state()).isEqualTo(RunResult.INCOMPLETE);
  }

  @Test
  void aCheckWithNothingToPointAtPointsToMain() {
    RunResult result = run(content, ALIASING, "// Todavía nada.");

    assertThat(result.log())
        .allSatisfy(
            entry -> assertThat(entry.sourceRef()).isEqualTo(new SourceRef("Main.java", 2)));
  }

  @Test
  void aPieceTheCodeDidNotBuildIsListedAsNotBuilt() {
    RunResult result = run(content, ALIASING, "// Todavía nada.");

    assertThat(piece(result, "fr-resistencia"))
        .isEqualTo(
            new Piece(
                "fr-resistencia",
                "regional-faculty",
                RunResult.INCOMPLETE,
                false,
                "FR Resistencia",
                null,
                null,
                null));
  }

  @Test
  void anObjectTheBriefDidNotAskForIsAPieceOfItsOwn() {
    RunResult result =
        run(
            content,
            ALIASING,
            """
            FacultadRegional resistencia = new FacultadRegional();
            resistencia.nombre = "Resistencia";
            FacultadRegional miFacultad = new FacultadRegional();
            """);

    Piece extra = piece(result, "facultadregional-1");

    assertThat(extra.archetype()).isEqualTo("regional-faculty");
    assertThat(extra.label()).isEqualTo("FacultadRegional");
    assertThat(piece(result, "var-miFacultad").target()).isEqualTo("facultadregional-1");
  }

  @Test
  void aVariableThatPointsToNothingHasNoTarget() {
    RunResult result =
        run(
            content,
            ALIASING,
            """
            FacultadRegional resistencia = new FacultadRegional();
            FacultadRegional miFacultad = null;
            int cupos = 40;
            """);

    assertThat(piece(result, "var-miFacultad").target()).isNull();
    assertThat(result.pieces()).extracting(Piece::id).doesNotContain("var-cupos");
  }

  @Test
  void anUnexpectedExceptionFailsTheRunAndComesFirstInTheLog() {
    RunResult result =
        run(
            content,
            ALIASING,
            """
            FacultadRegional resistencia = null;
            resistencia.nombre = "Resistencia";
            """);

    assertThat(result.outcome()).isEqualTo(RunResult.FAILED);
    assertThat(result.log().getFirst().state()).isEqualTo(RunResult.FAILED);
    assertThat(result.log().getFirst().title())
        .isEqualTo("Usaste `resistencia`, pero apunta a `null`.");
    assertThat(result.log().getFirst().sourceRef()).isEqualTo(new SourceRef("Main.java", 4));
    assertThat(result.log())
        .extracting(LogEntry::checkId)
        .contains("alias-declared", "province-through-alias");
  }

  @Test
  void aProgramThatDoesNotCompileBuildsNothing() {
    RunResult result = run(content, ALIASING, "FacultadRegional resistencia = new Facultad();");

    assertThat(result.outcome()).isEqualTo(RunResult.FAILED);
    assertThat(result.pieces()).isEmpty();
    assertThat(result.timeline()).isEmpty();
    assertThat(result.progress()).isEqualTo(new RunResult.Progress(0, 2));
    assertThat(result.log())
        .singleElement()
        .satisfies(entry -> assertThat(entry.checkId()).isNull());
  }

  @Test
  void slotsSayWhichPiecesOccupyThem() {
    RunResult solved = runSolution(fixtures, COMPOSITION);
    RunResult starter = TestContent.run(fixtures, COMPOSITION, List.of());

    assertThat(piece(solved, "fr-resistencia").slots())
        .containsEntry("dean", new Slot("filled", List.of("decano")))
        .containsEntry("departments", new Slot("filled", List.of("departamento-1")));
    assertThat(piece(starter, "fr-resistencia").slots())
        .containsEntry("dean", new Slot("missing", List.of()))
        .containsEntry("departments", new Slot("missing", List.of()));
  }

  @Test
  void aPieceTakesItsLabelFromTheFieldItsBindingNames() {
    RunResult result = runSolution(fixtures, COMPOSITION);

    assertThat(piece(result, "fr-resistencia").label()).isEqualTo("Resistencia");
    assertThat(piece(result, "departamento-1").label()).isEqualTo("Materias Básicas");
    assertThat(piece(result, "departamento-1").archetype()).isEqualTo("department");
  }

  @Test
  void anExceptionTheChallengeAsksForIsNotAFailure() {
    RunResult result = runSolution(fixtures, COMPOSITION);

    assertThat(result.outcome()).isEqualTo(RunResult.PASSED);
    assertThat(result.log())
        .allSatisfy(entry -> assertThat(entry.state()).isEqualTo(RunResult.PASSED));
    assertThat(result.timeline().getLast().event()).isEqualTo("exception");
  }

  @Test
  void anUnexpectedExceptionMarksThePieceWhoseMethodWasRunning() {
    Challenge challenge = fixtures.challenge(COMPOSITION).orElseThrow();
    // Without the "throws" check the exception of asignar(null) is not expected.
    Challenge strict =
        new Challenge(
            challenge.spec(),
            challenge.checks().subList(0, 1),
            challenge.starter(),
            challenge.solution(),
            challenge.solutionExplanation(),
            challenge.variants());
    List<SourceFile> files = challenge.solution();

    RunResult result =
        new ResultBuilder(fixtures.feedback())
            .build(
                strict,
                io.github.balbianoluciano.ljbu.api.support.Programs.trace(files),
                files,
                "es");

    assertThat(result.outcome()).isEqualTo(RunResult.FAILED);
    assertThat(result.log().getFirst().title()).isEqualTo("La facultad no puede quedar sin decano");
    assertThat(result.log().getFirst().pieceId()).isEqualTo("fr-resistencia");
    assertThat(piece(result, "fr-resistencia").state()).isEqualTo(RunResult.FAILED);
  }

  // --- Helpers

  private static RunResult runSolution(Content from, String challengeId) {
    Challenge challenge = from.challenge(challengeId).orElseThrow();
    return TestContent.run(
        from,
        challengeId,
        challenge.solution().stream()
            .filter(source -> challenge.isEditable(source.path()))
            .toList());
  }

  /** Runs the challenge with a Main.java whose main method has the given body. */
  private static RunResult run(Content from, String challengeId, String mainBody) {
    SourceFile main = io.github.balbianoluciano.ljbu.api.support.Programs.mainWithBody(mainBody);
    return TestContent.run(from, challengeId, List.of(file(main.path(), main.content())));
  }

  private static Piece piece(RunResult result, String id) {
    return result.pieces().stream()
        .filter(candidate -> candidate.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No piece " + id + " in " + result.pieces()));
  }
}
