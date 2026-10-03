package io.github.balbianoluciano.ljbu.api.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.balbianoluciano.ljbu.api.trace.Value;
import java.util.List;
import java.util.Map;

/**
 * Result of a run, returned to the web. Its JSON form is the contract in
 * packages/contracts/result.schema.json.
 *
 * @param outcome passed, incomplete or failed (docs/FEEDBACK.md §1)
 */
public record RunResult(
    String runId,
    String outcome,
    Progress progress,
    List<Piece> pieces,
    List<LogEntry> log,
    List<TimelineStep> timeline,
    String stdout) {

  public static final String PASSED = "passed";
  public static final String INCOMPLETE = "incomplete";
  public static final String FAILED = "failed";

  public record Progress(int passed, int total) {}

  public record SourceRef(String file, int line) {}

  /**
   * @param built false when the brief asks for the piece and the code did not create it
   * @param target for a variable, the piece it points to
   * @param floors for inheritance-floors, the classes of the object from the top of its hierarchy
   *     down to its own class
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Piece(
      String id,
      String archetype,
      String state,
      boolean built,
      String label,
      SourceRef sourceRef,
      Map<String, Slot> slots,
      String target,
      List<String> floors,
      List<String> interfaces) {

    public Piece withState(String newState) {
      return new Piece(
          id, archetype, newState, built, label, sourceRef, slots, target, floors, interfaces);
    }
  }

  public record Slot(String state, List<String> pieceIds) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LogEntry(
      String state,
      String title,
      String why,
      SourceRef sourceRef,
      String hint,
      String pieceId,
      String checkId,
      String detail) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TimelineStep(
      int index,
      SourceRef sourceRef,
      String event,
      String pieceId,
      String targetPieceId,
      String name,
      Value value,
      String text,
      Boolean caught) {}
}
