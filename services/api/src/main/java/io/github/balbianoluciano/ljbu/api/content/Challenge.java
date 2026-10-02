package io.github.balbianoluciano.ljbu.api.content;

import io.github.balbianoluciano.ljbu.api.checks.Check;
import java.util.List;
import java.util.Map;

/**
 * A challenge ready to use: its definition, its checks with their parameters parsed, and its files.
 *
 * @param variants code variants of the tests folder, by name, used by the content tests
 */
public record Challenge(
    ChallengeSpec spec,
    List<Check> checks,
    List<SourceFile> starter,
    List<SourceFile> solution,
    String solutionExplanation,
    Map<String, Variant> variants) {

  public String id() {
    return spec.id();
  }

  public boolean isEditable(String path) {
    return spec.editable().contains(path);
  }

  /** A variant of the learner's code and what the result must say about it. */
  public record Variant(List<SourceFile> files, Expected expected) {}

  public record Expected(String outcome, List<ExpectedEntry> log) {}

  /**
   * @param title text the title of the entry must contain; null to accept any
   */
  public record ExpectedEntry(String check, String state, String title) {}
}
