package io.github.balbianoluciano.ljbu.api.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.balbianoluciano.ljbu.api.support.TestContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The api must refuse to start with content that breaks docs/specs/challenge-format.md. */
class ContentLoaderTest {

  private static final String ALIASING =
      "challenges/m1-clases-y-objetos/03-la-facultad-donde-estudias";

  @TempDir Path temporary;
  private Path content;

  @BeforeEach
  void copyTheRealContent() {
    content = TestContent.copyOfReal(temporary.resolve("content"));
  }

  @Test
  void loadsTheContentOfTheRepository() {
    Content loaded = new ContentLoader().load(content);

    assertThat(loaded.modules()).extracting(ModuleSpec::id).containsExactly("m1", "m2", "m3", "m4");
    assertThat(loaded.challengesOf("m1"))
        .extracting(Challenge::id)
        .containsExactly("m1-01", "m1-02", "m1-03", "m1-04", "m1-05");
    assertThat(loaded.modules())
        .allSatisfy(module -> assertThat(loaded.challengesOf(module.id())).hasSize(5));
    assertThat(loaded.challenge("m1-03").orElseThrow().variants())
        .containsKeys("segunda-facultad", "sin-usar-el-alias");
    assertThat(loaded.domain().rules()).hasSize(18);
  }

  @Test
  void rejectsAChallengeThatBreaksItsSchema() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("concept: aliasing", "concept: Aliasing!"));

    assertInvalid("does not follow its schema");
  }

  @Test
  void rejectsARuleThatTheDomainDoesNotHave() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("rules: [R-UNI-02]", "rules: [R-UNI-99]"));

    assertInvalid("rule R-UNI-99 is not in content/domain/rules.json");
  }

  @Test
  void rejectsACriterionThatRefersToAMissingCheck() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("checks: [alias-declared]", "checks: [alias-missing]"));

    assertInvalid("a criterion refers to check alias-missing");
  }

  @Test
  void rejectsACheckThatRefersToAMissingPiece() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml ->
            yaml.replace(
                "    piece: fr-resistencia\n    highlight: declaration",
                "    piece: fr-mendoza\n    highlight: declaration"));

    assertInvalid("check alias-declared refers to a piece that scene.pieces does not define");
  }

  @Test
  void rejectsParametersThatTheCheckTypeDoesNotTake() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml ->
            yaml.replace("b: { local: miFacultad, in: Main.main }", "c: { local: miFacultad }"));

    assertInvalid("check alias-declared has invalid params");
  }

  @Test
  void rejectsACheckWithoutARequiredParameter() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml ->
            yaml.replace(
                "      expect: { field: provincia, equals: \"Chaco\" }\n      writtenThrough",
                "      writtenThrough"));

    assertInvalid("check province-through-alias has invalid params: expect is required");
  }

  @Test
  void rejectsAPlaceholderThatTheCheckTypeDoesNotFill() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml ->
            yaml.replace(
                "`miFacultad` no apunta a la FR Resistencia.",
                "`miFacultad` no apunta a {missing}."));

    assertInvalid("check alias-declared uses the placeholder {missing}");
  }

  @Test
  void rejectsAMessageLongerThanTheFeedbackSpecAllows() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("`miFacultad` no apunta a la FR Resistencia.", "x".repeat(300)));

    assertInvalid("the limit is 280");
  }

  @Test
  void rejectsAnEditableFileThatTheStarterDoesNotHave() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("editable: [Main.java]", "editable: [Main.java, Rectorado.java]"));

    assertInvalid("editable file Rectorado.java is not in starter/");
  }

  @Test
  void rejectsAReadOnlyFileThatTheSolutionChanges() {
    edit(
        ALIASING + "/solution/FacultadRegional.java",
        java -> java.replace("String provincia;", "String provincia = \"Chaco\";"));

    assertInvalid("FacultadRegional.java is read-only but differs between starter/ and solution/");
  }

  @Test
  void rejectsASolutionClassWithoutABinding() throws IOException {
    for (String folder : new String[] {"starter", "solution"}) {
      Files.writeString(
          content.resolve(ALIASING).resolve(folder).resolve("Rectorado.java"),
          "public class Rectorado {}\n");
    }

    assertInvalid("class Rectorado.java of the solution has no scene binding");
  }

  @Test
  void rejectsAChallengeWithoutTheExplanationOfItsSolution() throws IOException {
    Files.delete(content.resolve(ALIASING).resolve("solution.md"));

    assertInvalid("solution.md is missing or empty");
  }

  @Test
  void rejectsAnOrderThatDoesNotMatchTheFolder() {
    edit(
        ALIASING + "/challenge.yaml",
        yaml -> yaml.replace("id: m1-03", "id: m1-04").replace("order: 3", "order: 4"));

    assertInvalid("order 4 does not match the folder name");
  }

  @Test
  void rejectsAVariantThatChangesAReadOnlyFile() throws IOException {
    Files.writeString(
        content.resolve(ALIASING).resolve("tests/segunda-facultad/FacultadRegional.java"),
        "public class FacultadRegional {}\n");

    assertInvalid("FacultadRegional.java is not an editable file of the challenge");
  }

  @Test
  void rejectsACatalogEntryWithAPlaceholderThatNothingFills() {
    edit(
        "feedback/javac.es.yaml",
        yaml ->
            yaml.replace(
                "what: \"A este método le falta un `return`.\"",
                "what: \"A `{metodo}` le falta un `return`.\""));

    assertInvalid("compiler.err.missing.ret.stmt uses {metodo}, which nothing fills");
  }

  @Test
  void rejectsARegionalFacultyWithoutASource() {
    edit(
        "domain/regional-faculties.json",
        json -> json.replaceFirst("(?s)\"sources\": \\[.*?\\]", "\"sources\": []"));

    assertInvalid("avellaneda has no source");
  }

  private void edit(String file, UnaryOperator<String> change) {
    try {
      Path path = content.resolve(file);
      String before = Files.readString(path);
      String after = change.apply(before);
      assertThat(after).as("the edit must change %s", file).isNotEqualTo(before);
      Files.writeString(path, after);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void assertInvalid(String problem) {
    assertThatThrownBy(() -> new ContentLoader().load(content))
        .isInstanceOf(ContentException.class)
        .hasMessageContaining(problem);
  }
}
