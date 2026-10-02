package io.github.balbianoluciano.ljbu.api.result;

import io.github.balbianoluciano.ljbu.api.checks.Check;
import io.github.balbianoluciano.ljbu.api.checks.CheckEngine;
import io.github.balbianoluciano.ljbu.api.checks.CheckOutcome;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams;
import io.github.balbianoluciano.ljbu.api.checks.RunFacts;
import io.github.balbianoluciano.ljbu.api.content.Challenge;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.Message;
import io.github.balbianoluciano.ljbu.api.content.Localized;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.RunResult.LogEntry;
import io.github.balbianoluciano.ljbu.api.result.RunResult.Progress;
import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.result.RunResult.TimelineStep;
import io.github.balbianoluciano.ljbu.api.trace.Step;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the trace of an execution into what the learner sees: pieces, the log and the timeline
 * (docs/FEEDBACK.md).
 */
public final class ResultBuilder {

  /** A program with many errors is explained a few at a time. */
  private static final int MAX_DIAGNOSTICS = 10;

  private final FeedbackCatalogs catalogs;

  public ResultBuilder(FeedbackCatalogs catalogs) {
    this.catalogs = catalogs;
  }

  /**
   * @param files the code that ran
   */
  public RunResult build(
      Challenge challenge, Trace trace, List<SourceFile> files, String language) {
    String runId = UUID.randomUUID().toString();
    Progress nothingChecked = new Progress(0, challenge.checks().size());
    return switch (trace) {
      case Trace.CompileError error ->
          new RunResult(
              runId,
              RunResult.FAILED,
              nothingChecked,
              List.of(),
              compilation(error),
              List.of(),
              "");
      case Trace.Rejected rejected ->
          new RunResult(
              runId,
              RunResult.FAILED,
              nothingChecked,
              List.of(),
              rejection(rejected),
              List.of(),
              "");
      case Trace.Executed executed -> executed(runId, challenge, executed, files, language);
    };
  }

  // --- Nothing ran

  private List<LogEntry> compilation(Trace.CompileError error) {
    return error.diagnostics().stream()
        .limit(MAX_DIAGNOSTICS)
        .map(
            diagnostic -> {
              FeedbackCatalogs.Message message =
                  catalogs.javac().explain(diagnostic.code(), diagnostic.message(), Map.of());
              // An error nobody wrote an explanation for shows what javac said.
              String detail =
                  catalogs.javac().knows(diagnostic.code()) ? null : diagnostic.message();
              return failure(
                  message, new SourceRef(diagnostic.file(), diagnostic.line()), null, detail);
            })
        .toList();
  }

  private List<LogEntry> rejection(Trace.Rejected rejected) {
    return rejected.violations().stream()
        .map(
            violation ->
                failure(
                    catalogs.rejections().explain(violation.symbol()),
                    new SourceRef(violation.file(), violation.line()),
                    null,
                    null))
        .toList();
  }

  private static LogEntry failure(
      FeedbackCatalogs.Message message, SourceRef where, String pieceId, String detail) {
    return new LogEntry(
        RunResult.FAILED,
        message.what(),
        message.why(),
        where,
        message.hint(),
        pieceId,
        null,
        detail);
  }

  // --- The program ran

  private RunResult executed(
      String runId,
      Challenge challenge,
      Trace.Executed trace,
      List<SourceFile> files,
      String language) {
    Map<String, String> sources = new LinkedHashMap<>();
    files.forEach(file -> sources.put(file.path(), file.content()));
    RunFacts facts = new RunFacts(trace, sources);
    Scene scene = new Scene(challenge.spec().scene(), facts, language);

    Map<Check, CheckOutcome> outcomes = new LinkedHashMap<>();
    challenge
        .checks()
        .forEach(check -> outcomes.put(check, CheckEngine.evaluate(check.params(), facts)));

    List<LogEntry> problems = new ArrayList<>();
    boolean broke = false;
    if (trace.exception() != null && !isExpected(trace, challenge)) {
      broke = true;
      String piece = scene.pieceOf(receiverAtTheEnd(facts));
      if (piece != null) {
        scene.mark(piece, RunResult.FAILED);
      }
      problems.add(uncaughtException(trace.exception(), piece));
    }
    if (!trace.completed() && trace.exception() == null) {
      broke = true;
      problems.add(limit(trace, facts));
    }

    List<LogEntry> unmet = new ArrayList<>();
    List<LogEntry> met = new ArrayList<>();
    for (Map.Entry<Check, CheckOutcome> evaluated : outcomes.entrySet()) {
      Check check = evaluated.getKey();
      CheckOutcome outcome = evaluated.getValue();
      SourceRef where = highlight(check, outcome, facts);
      String piece = check.spec().piece();
      if (outcome.passed()) {
        Message message = check.spec().feedback().pass();
        met.add(
            entry(RunResult.PASSED, message, null, outcome, where, piece, check.id(), language));
      } else {
        if (piece != null) {
          scene.mark(piece, RunResult.INCOMPLETE);
        }
        unmet.add(unmetEntry(check, outcome, where, facts, language));
      }
    }

    // docs/FEEDBACK.md §2: what broke first, then what is missing, then what is right.
    List<LogEntry> log = new ArrayList<>(problems);
    log.addAll(unmet);
    log.addAll(met);
    String result =
        broke ? RunResult.FAILED : unmet.isEmpty() ? RunResult.PASSED : RunResult.INCOMPLETE;
    return new RunResult(
        runId,
        result,
        new Progress(met.size(), challenge.checks().size()),
        scene.pieces(),
        log,
        timeline(trace, scene),
        trace.stdout());
  }

  /** An exception is expected when a check of the challenge asks for it to end the program. */
  private static boolean isExpected(Trace.Executed trace, Challenge challenge) {
    if (trace.steps().isEmpty()
        || !(trace.steps().getLast() instanceof Step.ExceptionThrown thrown)) {
      return false;
    }
    return challenge.checks().stream()
        .map(Check::params)
        .anyMatch(
            params ->
                params instanceof CheckParams.Throws wanted && CheckEngine.matches(thrown, wanted));
  }

  /** The object whose method was running when the program stopped, or null. */
  private static String receiverAtTheEnd(RunFacts facts) {
    List<Step.Call> running = facts.callsInProgressAt(facts.trace().steps().size());
    for (Step.Call call : running.reversed()) {
      if (call.target() != null) {
        return call.target();
      }
    }
    return null;
  }

  private LogEntry uncaughtException(Trace.ExceptionInfo exception, String pieceId) {
    String simpleName = exception.type().substring(exception.type().lastIndexOf('.') + 1);
    Map<String, String> builtIn = new LinkedHashMap<>();
    builtIn.put("type", simpleName);
    if (exception.message() != null) {
      builtIn.put("message", exception.message());
    }
    FeedbackCatalogs.Message message =
        catalogs.exceptions().explain(exception.type(), exception.message(), builtIn);
    String detail = null;
    if (!catalogs.exceptions().knows(exception.type())) {
      detail =
          exception.message() == null
              ? exception.type()
              : exception.type() + ": " + exception.message();
    }
    return failure(message, new SourceRef(exception.file(), exception.line()), pieceId, detail);
  }

  private LogEntry limit(Trace.Executed trace, RunFacts facts) {
    String key =
        switch (trace.status()) {
          case "timeout" -> "timeout";
          case "limit_exceeded" -> trace.limits().exceeded();
          default -> "crash";
        };
    FeedbackCatalogs.Entry entry =
        catalogs.limits().getOrDefault(key, catalogs.limits().get("crash"));
    SourceRef where =
        trace.steps().isEmpty()
            ? facts.mainDeclaration()
            : RunFacts.lineOf(trace.steps().getLast());
    return failure(entry.explain(null, Map.of()), where, null, null);
  }

  private LogEntry unmetEntry(
      Check check, CheckOutcome outcome, SourceRef where, RunFacts facts, String language) {
    Message fail = check.spec().feedback().fail();
    for (Check.Trap trap : check.traps()) {
      if (CheckEngine.evaluate(trap.when(), facts).passed()) {
        Localized hint = trap.spec().hint() != null ? trap.spec().hint() : fail.hint();
        Message message = new Message(trap.spec().what(), trap.spec().why(), hint);
        return entry(
            RunResult.INCOMPLETE,
            message,
            hint,
            outcome,
            where,
            check.spec().piece(),
            check.id(),
            language);
      }
    }
    return entry(
        RunResult.INCOMPLETE,
        fail,
        fail.hint(),
        outcome,
        where,
        check.spec().piece(),
        check.id(),
        language);
  }

  private static LogEntry entry(
      String state,
      Message message,
      Localized hint,
      CheckOutcome outcome,
      SourceRef where,
      String pieceId,
      String checkId,
      String language) {
    return new LogEntry(
        state,
        Templates.render(message.what().in(language), outcome.placeholders()),
        Templates.render(message.why().in(language), outcome.placeholders()),
        where,
        hint == null ? null : Templates.render(hint.in(language), outcome.placeholders()),
        pieceId,
        checkId,
        null);
  }

  /** The line the check asks to highlight; failing that, any line it knows; failing that, main. */
  private static SourceRef highlight(Check check, CheckOutcome outcome, RunFacts facts) {
    CheckOutcome.Lines lines = outcome.lines();
    SourceRef wanted =
        switch (check.spec().highlight() == null ? "" : check.spec().highlight()) {
          case "object_creation" -> lines.objectCreation();
          case "declaration" -> lines.declaration();
          case "last_write" -> lines.lastWrite();
          default -> null;
        };
    if (wanted != null) {
      return wanted;
    }
    for (SourceRef known :
        new SourceRef[] {lines.lastWrite(), lines.objectCreation(), lines.declaration()}) {
      if (known != null) {
        return known;
      }
    }
    return facts.mainDeclaration();
  }

  // --- Timeline

  private static List<TimelineStep> timeline(Trace.Executed trace, Scene scene) {
    return trace.steps().stream().map(step -> timelineStep(step, scene)).toList();
  }

  private static TimelineStep timelineStep(Step step, Scene scene) {
    SourceRef where = RunFacts.lineOf(step);
    int index = step.index();
    return switch (step) {
      case Step.ObjectCreated created ->
          new TimelineStep(
              index,
              where,
              "object_created",
              scene.pieceOf(created.object().id()),
              null,
              created.object().type(),
              null,
              null,
              null);
      case Step.FieldSet write ->
          new TimelineStep(
              index,
              where,
              "field_set",
              scene.pieceOf(write.target()),
              scene.pieceOf(write.value()),
              write.field(),
              write.value(),
              null,
              null);
      case Step.LocalSet write -> {
        String variable = Scene.variableId(write.name());
        boolean drawn = write.method().equals("Main.main") && scene.has(variable);
        yield new TimelineStep(
            index,
            where,
            "local_set",
            drawn ? variable : null,
            scene.pieceOf(write.value()),
            write.name(),
            write.value(),
            null,
            null);
      }
      case Step.Call call ->
          new TimelineStep(
              index,
              where,
              "call",
              scene.pieceOf(call.target()),
              null,
              call.method(),
              null,
              null,
              null);
      case Step.Return returned ->
          new TimelineStep(
              index,
              where,
              "return",
              null,
              scene.pieceOf(returned.value()),
              returned.method(),
              returned.value(),
              null,
              null);
      case Step.Output output ->
          new TimelineStep(index, where, "output", null, null, null, null, output.text(), null);
      case Step.ExceptionThrown thrown ->
          new TimelineStep(
              index,
              where,
              "exception",
              null,
              null,
              thrown.exception().type(),
              null,
              thrown.exception().message(),
              thrown.caught());
    };
  }
}
