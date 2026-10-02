package io.github.balbianoluciano.ljbu.api.checks;

import io.github.balbianoluciano.ljbu.api.checks.CheckOutcome.Lines;
import io.github.balbianoluciano.ljbu.api.checks.RunFacts.LearnerObject;
import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.trace.HeapObject;
import io.github.balbianoluciano.ljbu.api.trace.Step;
import io.github.balbianoluciano.ljbu.api.trace.Structure.ClassInfo;
import io.github.balbianoluciano.ljbu.api.trace.Structure.ConstructorInfo;
import io.github.balbianoluciano.ljbu.api.trace.Structure.FieldInfo;
import io.github.balbianoluciano.ljbu.api.trace.Structure.MethodInfo;
import io.github.balbianoluciano.ljbu.api.trace.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Evaluates the declarative checks of a challenge on the structure and the trace of an execution
 * (ADR 0012). It never runs code: everything it knows comes from what the runner reported.
 */
public final class CheckEngine {

  private CheckEngine() {}

  public static CheckOutcome evaluate(CheckParams params, RunFacts facts) {
    return switch (params) {
      case CheckParams.ClassExists check -> classExists(check, facts);
      case CheckParams.Extends check -> extendsClass(check, facts);
      case CheckParams.Implements check -> implementsInterface(check, facts);
      case CheckParams.IsAbstract check -> isAbstract(check, facts);
      case CheckParams.Field check -> field(check, facts);
      case CheckParams.Constructor check -> constructor(check, facts);
      case CheckParams.Method check -> method(check, facts);
      case CheckParams.ObjectCount check -> objectCount(check, facts);
      case CheckParams.ObjectField check -> objectField(check, facts);
      case CheckParams.SharedReference check -> sharedReference(check, facts);
      case CheckParams.CallDispatch check -> callDispatch(check, facts);
      case CheckParams.StdoutContains check -> stdoutContains(check, facts);
      case CheckParams.NoException check -> noException(facts);
      case CheckParams.Throws check -> throwsException(check, facts);
    };
  }

  // --- Structure

  private static CheckOutcome classExists(CheckParams.ClassExists check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    return CheckOutcome.of(type.isPresent(), declaration(type));
  }

  private static CheckOutcome extendsClass(CheckParams.Extends check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    boolean passed = type.filter(info -> check.superclass().equals(info.superclass())).isPresent();
    return CheckOutcome.of(passed, declaration(type));
  }

  private static CheckOutcome implementsInterface(CheckParams.Implements check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    boolean passed =
        type.filter(info -> info.interfaces().contains(check.interfaceName())).isPresent();
    return CheckOutcome.of(passed, declaration(type));
  }

  private static CheckOutcome isAbstract(CheckParams.IsAbstract check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    if (check.method() == null) {
      return CheckOutcome.of(type.filter(ClassInfo::isAbstract).isPresent(), declaration(type));
    }
    Optional<MethodInfo> method =
        type.flatMap(
            info ->
                info.methods().stream()
                    .filter(candidate -> candidate.name().equals(check.method()))
                    .findFirst());
    Lines lines =
        method
            .map(found -> Lines.declaration(new SourceRef(type.get().file(), found.line())))
            .orElse(declaration(type));
    return CheckOutcome.of(method.filter(MethodInfo::isAbstract).isPresent(), lines);
  }

  private static CheckOutcome field(CheckParams.Field check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    List<String> missing = new ArrayList<>();
    SourceRef firstDeclared = null;
    for (String name : check.fieldNames()) {
      Optional<FieldInfo> declared =
          type.flatMap(
              info ->
                  info.fields().stream().filter(field -> field.name().equals(name)).findFirst());
      if (declared.filter(field -> matches(field, check)).isEmpty()) {
        missing.add(name);
      } else if (firstDeclared == null) {
        firstDeclared = new SourceRef(type.get().file(), declared.get().line());
      }
    }
    Lines lines = firstDeclared != null ? Lines.declaration(firstDeclared) : declaration(type);
    return new CheckOutcome(missing.isEmpty(), Map.of("missing", codeList(missing)), lines);
  }

  private static boolean matches(FieldInfo field, CheckParams.Field check) {
    return (check.type() == null || check.type().equals(field.type()))
        && (check.visibility() == null || check.visibility().equals(field.visibility()))
        && (check.isFinal() == null || check.isFinal() == field.isFinal())
        && (check.isStatic() == null || check.isStatic() == field.isStatic());
  }

  private static CheckOutcome constructor(CheckParams.Constructor check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    Optional<ConstructorInfo> declared =
        type.flatMap(
            info ->
                info.constructors().stream()
                    .filter(candidate -> candidate.parameterTypes().equals(check.parameterTypes()))
                    .filter(
                        candidate ->
                            check.visibility() == null
                                || check.visibility().equals(candidate.visibility()))
                    .findFirst());
    Lines lines =
        declared
            .map(found -> Lines.declaration(new SourceRef(type.get().file(), found.line())))
            .orElse(declaration(type));
    boolean passed = declared.isPresent();
    if (passed && check.delegatesTo() != null) {
      String delegate =
          check.delegatesTo().equals("this") ? check.className() : type.get().superclass();
      passed = delegate != null && delegates(check, delegate + ".<init>", facts);
    }
    return CheckOutcome.of(passed, lines);
  }

  /** Whether a run of the constructor called the other constructor on the same object. */
  private static boolean delegates(CheckParams.Constructor check, String delegate, RunFacts facts) {
    String constructor = check.className() + ".<init>";
    List<Step> steps = facts.trace().steps();
    for (int i = 0; i < steps.size(); i++) {
      if (!(steps.get(i) instanceof Step.Call outer)
          || !outer.method().equals(constructor)
          || outer.args().size() != check.parameterTypes().size()) {
        continue;
      }
      int depth = 1;
      for (int j = i + 1; j < steps.size() && depth > 0; j++) {
        if (steps.get(j) instanceof Step.Call inner) {
          if (depth == 1
              && inner.method().equals(delegate)
              && outer.target() != null
              && outer.target().equals(inner.target())) {
            return true;
          }
          depth++;
        } else if (steps.get(j) instanceof Step.Return) {
          depth--;
        }
      }
    }
    return false;
  }

  private static CheckOutcome method(CheckParams.Method check, RunFacts facts) {
    Optional<ClassInfo> type = facts.structure().find(check.className());
    List<MethodInfo> named =
        type.map(
                info ->
                    info.methods().stream()
                        .filter(candidate -> candidate.name().equals(check.name()))
                        .toList())
            .orElse(List.of());
    Optional<MethodInfo> matching =
        named.stream().filter(candidate -> matches(candidate, check)).findFirst();
    Lines lines =
        matching
            .or(() -> named.stream().findFirst())
            .map(found -> Lines.declaration(new SourceRef(type.get().file(), found.line())))
            .orElse(declaration(type));
    return CheckOutcome.of(matching.isPresent(), lines);
  }

  private static boolean matches(MethodInfo method, CheckParams.Method check) {
    return (check.parameterTypes() == null
            || check.parameterTypes().equals(method.parameterTypes()))
        && (check.returnType() == null || check.returnType().equals(method.returnType()))
        && (check.visibility() == null || check.visibility().equals(method.visibility()))
        && (check.isStatic() == null || check.isStatic() == method.isStatic())
        && (check.isAbstract() == null || check.isAbstract() == method.isAbstract())
        && (check.isOverride() == null || check.isOverride() == method.isOverride());
  }

  private static Lines declaration(Optional<ClassInfo> type) {
    return type.map(info -> Lines.declaration(new SourceRef(info.file(), info.line())))
        .orElse(Lines.NONE);
  }

  // --- Execution

  private static CheckOutcome objectCount(CheckParams.ObjectCount check, RunFacts facts) {
    List<LearnerObject> objects = facts.objectsOf(check.type(), check.where());
    int count = objects.size();
    boolean passed =
        (check.exactly() == null || count == check.exactly())
            && (check.atLeast() == null || count >= check.atLeast())
            && (check.atMost() == null || count <= check.atMost());
    SourceRef created = objects.isEmpty() ? null : objects.getLast().created();
    return new CheckOutcome(
        passed, Map.of("count", Integer.toString(count)), new Lines(null, created, null));
  }

  private static CheckOutcome objectField(CheckParams.ObjectField check, RunFacts facts) {
    List<LearnerObject> selected = facts.objectsOf(check.where().type(), check.where().condition());
    List<String> expectedFields =
        check.expect().stream().map(CheckParams.Expectation::field).toList();
    if (selected.isEmpty()) {
      // With "all", having no object to look at breaks nothing.
      return new CheckOutcome(
          check.everyObject(),
          Map.of("missing", codeList(check.everyObject() ? List.of() : expectedFields)),
          Lines.NONE);
    }
    List<LearnerObject> inspected = check.everyObject() ? selected : List.of(selected.getFirst());
    LearnerObject shown = inspected.getFirst();
    List<String> missing = List.of();
    for (LearnerObject object : inspected) {
      List<String> unmet =
          check.expect().stream()
              .filter(expectation -> !meets(object, expectation))
              .map(CheckParams.Expectation::field)
              .toList();
      if (!unmet.isEmpty()) {
        shown = object;
        missing = unmet;
        break;
      }
    }
    String field = missing.isEmpty() ? expectedFields.getFirst() : missing.getFirst();
    Optional<Step.FieldSet> lastWrite = facts.lastWrite(shown.id(), field);
    Lines lines = new Lines(null, shown.created(), lastWrite.map(RunFacts::lineOf).orElse(null));
    boolean passed = missing.isEmpty();
    if (passed && check.writtenThrough() != null) {
      passed =
          lastWrite.filter(write -> wentThrough(write, check.writtenThrough(), facts)).isPresent();
    }
    return new CheckOutcome(passed, Map.of("missing", codeList(missing)), lines);
  }

  private static boolean meets(LearnerObject object, CheckParams.Expectation expectation) {
    Value value = object.fields().get(expectation.field());
    if (expectation.expected() != null) {
      return Values.matches(value, expectation.expected());
    }
    if (expectation.notNull() != null) {
      return value != null && expectation.notNull() != Values.isNull(value);
    }
    return value != null && expectation.isNull() == Values.isNull(value);
  }

  /**
   * Whether the write was made through the local variable. The trace knows which object was
   * written, not which variable was used to reach it; that is read from the line of the method that
   * made the write, or that started the call that made it.
   */
  private static boolean wentThrough(
      Step.FieldSet write, CheckParams.LocalRef local, RunFacts facts) {
    String owner = local.method().substring(0, local.method().lastIndexOf('.'));
    Optional<ClassInfo> type = facts.structure().find(owner);
    if (type.isEmpty()) {
      return false;
    }
    String file = type.get().file();
    String variable = Pattern.quote(local.local());
    if (write.file().equals(file)) {
      String direct = "\\b" + variable + "\\s*\\.\\s*" + Pattern.quote(write.field()) + "\\b";
      return Pattern.compile(direct).matcher(code(facts, file, write.line())).find();
    }
    // The write happened inside a method: look at the line that called it.
    for (Step.Call call : facts.callsInProgressAt(write.index()).reversed()) {
      if (call.file().equals(file)) {
        String throughCall = "\\b" + variable + "\\s*\\.";
        return Pattern.compile(throughCall).matcher(code(facts, file, call.line())).find();
      }
    }
    return false;
  }

  /** The line without its trailing comment. */
  private static String code(RunFacts facts, String file, int line) {
    String text = facts.sourceLine(file, line);
    int comment = text.indexOf("//");
    return comment < 0 ? text : text.substring(0, comment);
  }

  private static CheckOutcome sharedReference(CheckParams.SharedReference check, RunFacts facts) {
    if (check.all() != null) {
      List<LearnerObject> objects = facts.objectsOf(check.all().type(), null);
      List<String> targets =
          objects.stream()
              .map(object -> Values.referenceOf(object.fields().get(check.all().field())))
              .distinct()
              .toList();
      boolean passed = !objects.isEmpty() && targets.size() == 1 && targets.getFirst() != null;
      SourceRef created =
          passed ? facts.object(targets.getFirst()).map(LearnerObject::created).orElse(null) : null;
      return CheckOutcome.of(passed, new Lines(null, created, null));
    }
    Resolved a = resolve(check.a(), facts);
    Resolved b = resolve(check.b(), facts);
    boolean passed = a.objectId() != null && a.objectId().equals(b.objectId());
    SourceRef created =
        a.objectId() == null
            ? null
            : facts.object(a.objectId()).map(LearnerObject::created).orElse(null);
    SourceRef declared = b.line() != null ? b.line() : a.line();
    return CheckOutcome.of(passed, new Lines(declared, created, null));
  }

  /** The object a reference points to and the line where the reference got its value. */
  private record Resolved(String objectId, SourceRef line) {}

  private static Resolved resolve(CheckParams.Reference reference, RunFacts facts) {
    if (reference.local() != null) {
      Step.LocalSet write = facts.lastLocalWrites(reference.method()).get(reference.local());
      return write == null
          ? new Resolved(null, null)
          : new Resolved(Values.referenceOf(write.value()), RunFacts.lineOf(write));
    }
    List<LearnerObject> owners =
        facts.objectsOf(reference.object().type(), reference.object().condition());
    if (owners.isEmpty()) {
      return new Resolved(null, null);
    }
    LearnerObject owner = owners.getFirst();
    SourceRef line =
        facts
            .lastWrite(owner.id(), reference.field())
            .map(RunFacts::lineOf)
            .orElse(owner.created());
    return new Resolved(Values.referenceOf(owner.fields().get(reference.field())), line);
  }

  private static CheckOutcome callDispatch(CheckParams.CallDispatch check, RunFacts facts) {
    String implementation = check.implementation() + "." + check.method();
    for (Step step : facts.trace().steps()) {
      if (step instanceof Step.Call call
          && call.method().equals(implementation)
          && call.target() != null
          && facts.trace().heap().get(call.target()) instanceof HeapObject.Instance receiver
          && receiver.type().equals(check.receiverType())) {
        return CheckOutcome.of(true, Lines.declaration(RunFacts.lineOf(call)));
      }
    }
    return CheckOutcome.of(false);
  }

  private static CheckOutcome stdoutContains(CheckParams.StdoutContains check, RunFacts facts) {
    boolean ignoreCase = Boolean.TRUE.equals(check.ignoreCase());
    String wanted = ignoreCase ? check.text().toLowerCase(Locale.ROOT) : check.text();
    String stdout = facts.trace().stdout();
    boolean passed = (ignoreCase ? stdout.toLowerCase(Locale.ROOT) : stdout).contains(wanted);
    SourceRef printed = null;
    for (Step step : facts.trace().steps()) {
      if (step instanceof Step.Output output
          && (ignoreCase ? output.text().toLowerCase(Locale.ROOT) : output.text())
              .contains(wanted)) {
        printed = RunFacts.lineOf(output);
        break;
      }
    }
    return CheckOutcome.of(passed, Lines.declaration(printed));
  }

  private static CheckOutcome noException(RunFacts facts) {
    SourceRef thrown =
        facts.trace().exception() == null
            ? null
            : new SourceRef(facts.trace().exception().file(), facts.trace().exception().line());
    return CheckOutcome.of(facts.trace().completed(), Lines.declaration(thrown));
  }

  private static CheckOutcome throwsException(CheckParams.Throws check, RunFacts facts) {
    for (Step step : facts.trace().steps()) {
      if (step instanceof Step.ExceptionThrown thrown && matches(thrown, check)) {
        return CheckOutcome.of(true, Lines.declaration(RunFacts.lineOf(thrown)));
      }
    }
    return CheckOutcome.of(false);
  }

  /** The type can be written with or without its package. */
  public static boolean matches(Step.ExceptionThrown thrown, CheckParams.Throws check) {
    String type = thrown.exception().type();
    boolean sameType = type.equals(check.type()) || type.endsWith("." + check.type());
    boolean sameMessage =
        check.messageContains() == null
            || (thrown.exception().message() != null
                && thrown.exception().message().contains(check.messageContains()));
    boolean sameFate = check.caught() == null || check.caught() == thrown.caught();
    return sameType && sameMessage && sameFate;
  }

  /** Names as inline code, separated by commas: {@code `ciudad`, `provincia`}. */
  private static String codeList(List<String> names) {
    return names.stream().map(name -> "`" + name + "`").collect(Collectors.joining(", "));
  }
}
