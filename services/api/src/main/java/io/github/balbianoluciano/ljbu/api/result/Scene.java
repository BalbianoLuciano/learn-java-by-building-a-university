package io.github.balbianoluciano.ljbu.api.result;

import io.github.balbianoluciano.ljbu.api.checks.CheckParams.FieldCondition;
import io.github.balbianoluciano.ljbu.api.checks.RunFacts;
import io.github.balbianoluciano.ljbu.api.checks.RunFacts.LearnerObject;
import io.github.balbianoluciano.ljbu.api.checks.Values;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.Binding;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.ExpectedPiece;
import io.github.balbianoluciano.ljbu.api.result.RunResult.Piece;
import io.github.balbianoluciano.ljbu.api.result.RunResult.Slot;
import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.trace.HeapObject;
import io.github.balbianoluciano.ljbu.api.trace.Step;
import io.github.balbianoluciano.ljbu.api.trace.Value;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The pieces of one execution (docs/ARCHITECTURE.md §7): the ones the brief asks for, matched with
 * the objects the code created; the other objects; and the variables of main. Every piece starts as
 * passed; the checks and the exception change that afterwards.
 */
public final class Scene {

  private static final String GENERIC_ARCHETYPE = "generic-block";
  private static final String VARIABLE_ARCHETYPE = "variable-sign";
  private static final String MAIN_METHOD = "Main.main";

  private final Map<String, Piece> pieces = new LinkedHashMap<>();
  private final Map<String, String> pieceByObject = new LinkedHashMap<>();

  public Scene(ChallengeSpec.Scene scene, RunFacts facts, String language) {
    List<LearnerObject> unmatched = new ArrayList<>(facts.objects());
    for (ExpectedPiece expected : scene.pieces()) {
      FieldCondition condition =
          expected.where() == null
              ? null
              : new FieldCondition(expected.where().field(), expected.where().expected(), null);
      Optional<LearnerObject> match =
          facts.objectsOf(expected.type(), condition).stream()
              .filter(unmatched::contains)
              .findFirst();
      match.ifPresent(
          object -> {
            unmatched.remove(object);
            pieceByObject.put(object.id(), expected.id());
          });
      pieces.put(expected.id(), null);
    }
    for (LearnerObject extra : unmatched) {
      pieceByObject.put(extra.id(), numberedId(extra.type().toLowerCase(Locale.ROOT)));
      pieces.put(pieceByObject.get(extra.id()), null);
    }

    // Slots refer to other pieces: built once every object knows its piece.
    for (ExpectedPiece expected : scene.pieces()) {
      Optional<LearnerObject> object = objectOf(expected.id(), facts);
      Binding binding = bindingFor(scene, object.map(LearnerObject::type).orElse(expected.type()));
      pieces.put(
          expected.id(),
          object
              .map(
                  built ->
                      piece(expected.id(), built, binding, expected.label().in(language), facts))
              .orElse(
                  new Piece(
                      expected.id(),
                      archetype(binding),
                      RunResult.INCOMPLETE,
                      false,
                      expected.label().in(language),
                      null,
                      null,
                      null)));
    }
    for (LearnerObject extra : unmatched) {
      String id = pieceByObject.get(extra.id());
      pieces.put(id, piece(id, extra, bindingFor(scene, extra.type()), extra.type(), facts));
    }
    if (scene.showsVariables()) {
      addVariables(facts);
    }
  }

  public List<Piece> pieces() {
    return List.copyOf(pieces.values());
  }

  public boolean has(String pieceId) {
    return pieces.containsKey(pieceId);
  }

  /** The piece that draws the object, or null if the object is not drawn. */
  public String pieceOf(String objectId) {
    return objectId == null ? null : pieceByObject.get(objectId);
  }

  /** The piece of the value if it is a reference to a drawn object, or null. */
  public String pieceOf(Value value) {
    return pieceOf(Values.referenceOf(value));
  }

  public static String variableId(String name) {
    return "var-" + name;
  }

  /** Marks the piece with the state, unless it already has a worse one. */
  public void mark(String pieceId, String state) {
    Piece piece = pieces.get(pieceId);
    if (piece == null || piece.state().equals(RunResult.FAILED)) {
      return;
    }
    if (state.equals(RunResult.FAILED) || piece.state().equals(RunResult.PASSED)) {
      pieces.put(pieceId, piece.withState(state));
    }
  }

  private Piece piece(
      String id, LearnerObject object, Binding binding, String fallbackLabel, RunFacts facts) {
    String label = fallbackLabel;
    if (binding != null
        && binding.label() != null
        && object.fields().get(binding.label()) instanceof Value.StringValue text) {
      label = text.value();
    }
    Map<String, Slot> slots = null;
    if (binding != null && binding.slots() != null && !binding.slots().isEmpty()) {
      slots = new LinkedHashMap<>();
      for (Map.Entry<String, String> slot : binding.slots().entrySet()) {
        List<String> occupants = occupants(object.fields().get(slot.getKey()), facts);
        slots.put(slot.getValue(), new Slot(occupants.isEmpty() ? "missing" : "filled", occupants));
      }
    }
    return new Piece(
        id, archetype(binding), RunResult.PASSED, true, label, object.created(), slots, null);
  }

  /** The pieces a field holds: one for a reference, several for an array or a collection. */
  private List<String> occupants(Value value, RunFacts facts) {
    String reference = Values.referenceOf(value);
    if (reference == null) {
      return List.of();
    }
    if (pieceByObject.containsKey(reference)) {
      return List.of(pieceByObject.get(reference));
    }
    if (facts.trace().heap().get(reference) instanceof HeapObject.Sequence sequence) {
      return sequence.elements().stream().map(this::pieceOf).filter(id -> id != null).toList();
    }
    return List.of();
  }

  /** Variables of main that hold a reference to a drawn object, or to nothing. */
  private void addVariables(RunFacts facts) {
    for (Step.LocalSet write : facts.lastLocalWrites(MAIN_METHOD).values()) {
      String target = pieceOf(write.value());
      if (target == null && !Values.isNull(write.value())) {
        continue;
      }
      String id =
          pieces.containsKey(variableId(write.name()))
              ? numberedId(variableId(write.name()))
              : variableId(write.name());
      pieces.put(
          id,
          new Piece(
              id,
              VARIABLE_ARCHETYPE,
              RunResult.PASSED,
              true,
              write.name(),
              new SourceRef(write.file(), write.line()),
              null,
              target));
    }
  }

  private Optional<LearnerObject> objectOf(String pieceId, RunFacts facts) {
    return pieceByObject.entrySet().stream()
        .filter(entry -> entry.getValue().equals(pieceId))
        .findFirst()
        .flatMap(entry -> facts.object(entry.getKey()));
  }

  private static Binding bindingFor(ChallengeSpec.Scene scene, String type) {
    return scene.bindings().stream()
        .filter(binding -> binding.type().equals(type))
        .findFirst()
        .orElse(null);
  }

  private static String archetype(Binding binding) {
    return binding == null ? GENERIC_ARCHETYPE : binding.archetype();
  }

  /** The first free id of the form base-1, base-2, … */
  private String numberedId(String base) {
    for (int n = 1; ; n++) {
      String candidate = base + "-" + n;
      if (!pieces.containsKey(candidate)) {
        return candidate;
      }
    }
  }
}
