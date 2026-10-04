package io.github.balbianoluciano.ljbu.api.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** A challenge.yaml file, as written (docs/specs/challenge-format.md). */
public record ChallengeSpec(
    String id,
    String module,
    int order,
    Localized title,
    String concept,
    List<String> rules,
    Localized brief,
    List<Criterion> criteria,
    List<String> editable,
    Limits limits,
    Scene scene,
    List<CheckSpec> checks,
    List<Localized> hints,
    Analogy analogy) {

  /** What happened, said without code (docs/FEEDBACK.md §3). */
  public record Analogy(Localized passed, Localized incomplete, Localized failed) {}

  public record Criterion(List<String> checks, String es, String en) {
    public Localized text() {
      return new Localized(es, en);
    }
  }

  public record Limits(Integer timeoutMs) {}

  public record Scene(
      List<Binding> bindings,
      List<ExpectedPiece> pieces,
      Boolean showVariables,
      List<String> context,
      RealReference realReference) {

    public boolean showsVariables() {
      return Boolean.TRUE.equals(showVariables);
    }
  }

  /**
   * @param label field of the object shown on its sign
   * @param slots composition fields drawn as slots: field name to slot name
   */
  public record Binding(String type, String archetype, String label, Map<String, String> slots) {}

  /** An object the brief asks for; checks refer to it by id. */
  public record ExpectedPiece(String id, String type, FieldCondition where, Localized label) {}

  public record FieldCondition(String field, @JsonProperty("equals") Object expected) {}

  public record RealReference(List<String> regionalFaculties, List<String> governingBodies) {}

  public record CheckSpec(
      String id,
      String type,
      JsonNode params,
      String piece,
      String slot,
      String highlight,
      Feedback feedback,
      List<TrapSpec> traps) {}

  public record Feedback(Message pass, Message fail) {}

  public record Message(Localized what, Localized why, Localized hint) {}

  public record TrapSpec(Condition when, Localized what, Localized why, Localized hint) {}

  public record Condition(String type, JsonNode params) {}
}
