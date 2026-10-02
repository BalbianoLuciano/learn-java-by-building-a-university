package io.github.balbianoluciano.ljbu.api.checks;

import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import java.util.Map;

/**
 * What a check found.
 *
 * @param placeholders values for the {placeholders} of the feedback
 * @param lines lines of the learner's code the check can point to
 */
public record CheckOutcome(boolean passed, Map<String, String> placeholders, Lines lines) {

  /** Candidate lines to highlight; any of them may be unknown. */
  public record Lines(SourceRef declaration, SourceRef objectCreation, SourceRef lastWrite) {
    public static final Lines NONE = new Lines(null, null, null);

    public static Lines declaration(SourceRef line) {
      return new Lines(line, null, null);
    }
  }

  public static CheckOutcome of(boolean passed, Lines lines) {
    return new CheckOutcome(passed, Map.of(), lines);
  }

  public static CheckOutcome of(boolean passed) {
    return of(passed, Lines.NONE);
  }
}
