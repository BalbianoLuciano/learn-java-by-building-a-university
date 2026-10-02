package io.github.balbianoluciano.ljbu.runner.execution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InputValidatorTest {

  private static final SourceFile MAIN = new SourceFile("Main.java", "public class Main {}");

  @Test
  void acceptsAMainFileWithOtherClasses() {
    List<SourceFile> files = List.of(MAIN, new SourceFile("FacultadRegional.java", ""));

    assertThatCode(() -> InputValidator.validate(files)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../Main.java",
        "src/Otra.java",
        "/etc/passwd",
        "module-info.java",
        "package-info.java",
        "minuscula.java",
        "Clase.class",
        "Clase.java ",
        ""
      })
  void rejectsFileNamesThatAreNotASimpleClassName(String name) {
    List<SourceFile> files = List.of(MAIN, new SourceFile(name, ""));

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(
            InvalidInputException.class, e -> assertCode(e, "invalid_file_name"));
  }

  @Test
  void rejectsMoreThanSixtyFourKilobytesOfCode() {
    String big = "//" + "x".repeat(InputValidator.MAX_TOTAL_BYTES);
    List<SourceFile> files = List.of(MAIN, new SourceFile("Grande.java", big));

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(InvalidInputException.class, e -> assertCode(e, "files_too_large"));
  }

  @Test
  void countsTheSizeInBytesNotInCharacters() {
    // 22 000 characters of three bytes each.
    String accents = "//" + "€".repeat(22_000);
    List<SourceFile> files = List.of(MAIN, new SourceFile("Acentos.java", accents));

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(InvalidInputException.class, e -> assertCode(e, "files_too_large"));
  }

  @Test
  void rejectsMoreThanTenFiles() {
    List<SourceFile> files = new ArrayList<>(List.of(MAIN));
    for (int i = 0; i < InputValidator.MAX_FILES; i++) {
      files.add(new SourceFile("Clase" + i + ".java", ""));
    }

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(InvalidInputException.class, e -> assertCode(e, "too_many_files"));
  }

  @Test
  void rejectsTwoFilesWithTheSameName() {
    List<SourceFile> files = List.of(MAIN, MAIN);

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(InvalidInputException.class, e -> assertCode(e, "duplicate_file"));
  }

  @Test
  void requiresMainJava() {
    List<SourceFile> files = List.of(new SourceFile("Otra.java", ""));

    assertThatThrownBy(() -> InputValidator.validate(files))
        .isInstanceOfSatisfying(
            InvalidInputException.class, e -> assertCode(e, "missing_main_file"));
  }

  @Test
  void rejectsAnEmptyRequest() {
    assertThatThrownBy(() -> InputValidator.validate(List.of()))
        .isInstanceOfSatisfying(InvalidInputException.class, e -> assertCode(e, "no_files"));
  }

  private static void assertCode(InvalidInputException exception, String code) {
    org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo(code);
  }
}
