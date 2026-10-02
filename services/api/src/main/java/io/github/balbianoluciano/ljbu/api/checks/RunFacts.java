package io.github.balbianoluciano.ljbu.api.checks;

import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.trace.HeapObject;
import io.github.balbianoluciano.ljbu.api.trace.Step;
import io.github.balbianoluciano.ljbu.api.trace.Structure;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import io.github.balbianoluciano.ljbu.api.trace.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Questions the checks and the scene ask about one execution, answered from its trace. */
public final class RunFacts {

  /** An object of a learner class: its final fields and where it was created. */
  public record LearnerObject(
      String id, String type, Map<String, Value> fields, SourceRef created) {}

  private final Trace.Executed trace;
  private final Map<String, String> sources;
  private final List<LearnerObject> objects = new ArrayList<>();
  private final Map<String, LearnerObject> objectsById = new LinkedHashMap<>();
  private final Map<String, Map<String, Step.LocalSet>> lastLocalWrites = new LinkedHashMap<>();

  /**
   * @param sources the code that ran, by file name
   */
  public RunFacts(Trace.Executed trace, Map<String, String> sources) {
    this.trace = trace;
    this.sources = sources;
    Map<String, SourceRef> creations = new LinkedHashMap<>();
    for (Step step : trace.steps()) {
      if (step instanceof Step.ObjectCreated created) {
        creations.put(created.object().id(), new SourceRef(created.file(), created.line()));
      } else if (step instanceof Step.LocalSet write) {
        lastLocalWrites
            .computeIfAbsent(write.method(), method -> new LinkedHashMap<>())
            .put(write.name(), write);
      }
    }
    trace.heap().entrySet().stream()
        .filter(entry -> entry.getValue() instanceof HeapObject.Instance)
        .filter(entry -> structure().find(entry.getValue().type()).isPresent())
        .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey().substring(1))))
        .forEach(
            entry -> {
              HeapObject.Instance instance = (HeapObject.Instance) entry.getValue();
              LearnerObject object =
                  new LearnerObject(
                      entry.getKey(),
                      instance.type(),
                      instance.fields(),
                      creations.get(entry.getKey()));
              objects.add(object);
              objectsById.put(object.id(), object);
            });
  }

  public Trace.Executed trace() {
    return trace;
  }

  public Structure structure() {
    return trace.structure();
  }

  /** Objects of learner classes, in the order they were created. */
  public List<LearnerObject> objects() {
    return objects;
  }

  public Optional<LearnerObject> object(String id) {
    return Optional.ofNullable(objectsById.get(id));
  }

  /** Objects of the type or of its subtypes that meet the condition, if there is one. */
  public List<LearnerObject> objectsOf(String type, CheckParams.FieldCondition condition) {
    return objects.stream()
        .filter(object -> structure().isSubtype(object.type(), type))
        .filter(object -> condition == null || meets(object, condition))
        .toList();
  }

  private static boolean meets(LearnerObject object, CheckParams.FieldCondition condition) {
    Value value = object.fields().get(condition.field());
    if (condition.expected() != null) {
      return Values.matches(value, condition.expected());
    }
    return !Values.matches(value, condition.notEquals());
  }

  /** The last value each local variable of the method received. */
  public Map<String, Step.LocalSet> lastLocalWrites(String method) {
    return lastLocalWrites.getOrDefault(method, Map.of());
  }

  public Optional<Step.FieldSet> lastWrite(String objectId, String field) {
    Step.FieldSet last = null;
    for (Step step : trace.steps()) {
      if (step instanceof Step.FieldSet write
          && objectId.equals(write.target())
          && write.field().equals(field)) {
        last = write;
      }
    }
    return Optional.ofNullable(last);
  }

  /**
   * The calls that had started and not finished when the step happened, outermost first. A frame
   * left by an exception has no return step, so it is dropped when a frame below it returns.
   */
  public List<Step.Call> callsInProgressAt(int stepIndex) {
    Deque<Step.Call> stack = new ArrayDeque<>();
    for (Step step : trace.steps()) {
      if (step.index() >= stepIndex) {
        break;
      }
      if (step instanceof Step.Call call) {
        stack.push(call);
      } else if (step instanceof Step.Return returned) {
        while (!stack.isEmpty() && !stack.pop().method().equals(returned.method())) {
          // Keep unwinding frames that an exception left behind.
        }
      }
    }
    return new ArrayList<>(stack).reversed();
  }

  /** The text of a line of the code that ran, or an empty text if there is no such line. */
  public String sourceLine(String file, int line) {
    String content = sources.get(file);
    if (content == null) {
      return "";
    }
    return content.lines().skip(line - 1L).findFirst().orElse("");
  }

  /** Where {@code Main.main} is declared: the place to point to when nothing better is known. */
  public SourceRef mainDeclaration() {
    return structure()
        .find("Main")
        .map(
            main ->
                main.methods().stream()
                    .filter(method -> method.name().equals("main"))
                    .findFirst()
                    .map(method -> new SourceRef(main.file(), method.line()))
                    .orElse(new SourceRef(main.file(), main.line())))
        .orElse(new SourceRef("Main.java", 1));
  }

  public static SourceRef lineOf(Step step) {
    return new SourceRef(step.file(), step.line());
  }
}
