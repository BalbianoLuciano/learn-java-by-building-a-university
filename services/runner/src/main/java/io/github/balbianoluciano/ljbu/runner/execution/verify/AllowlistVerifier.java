package io.github.balbianoluciano.ljbu.runner.execution.verify;

import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Violation;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantDynamicEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.MethodTypeEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks every reference of the compiled classes against the {@link Allowlist} before anything
 * runs. The instructions are walked to report each forbidden use with its line; the constant pool
 * is then walked whole, so a member that no instruction mentions cannot slip through.
 */
public final class AllowlistVerifier {

  private AllowlistVerifier() {}

  public static List<Violation> verify(Map<String, byte[]> classes, SourceFacts facts) {
    Allowlist allowlist = new Allowlist(classes.keySet());
    Set<Violation> violations = new LinkedHashSet<>();
    for (byte[] bytes : classes.values()) {
      new ClassCheck(ClassFile.of().parse(bytes), allowlist, facts, violations).run();
    }
    return new ArrayList<>(violations);
  }

  private static final class ClassCheck {
    private final ClassModel model;
    private final Allowlist allowlist;
    private final Set<Violation> violations;
    private final String file;
    private final int classLine;
    private final Set<String> reported = new HashSet<>();

    ClassCheck(
        ClassModel model, Allowlist allowlist, SourceFacts facts, Set<Violation> violations) {
      this.model = model;
      this.allowlist = allowlist;
      this.violations = violations;
      String name = model.thisClass().asInternalName();
      this.file =
          model
              .findAttribute(Attributes.sourceFile())
              .map(attribute -> attribute.sourceFile().stringValue())
              .orElse(name + ".java");
      this.classLine = facts.classLine(name);
    }

    void run() {
      model.superclass().ifPresent(entry -> checkClass(entry, classLine));
      model.interfaces().forEach(entry -> checkClass(entry, classLine));
      for (MethodModel method : model.methods()) {
        if (method.flags().has(AccessFlag.NATIVE)) {
          report("native " + method.methodName().stringValue(), classLine);
        }
        method.code().ifPresent(code -> new InstructionCheck().run(code));
      }
      checkConstantPool();
    }

    private final class InstructionCheck {
      private final Map<Label, List<ClassEntry>> handlers = new HashMap<>();
      private final List<ClassEntry> caughtHere = new ArrayList<>();
      private int line = classLine;

      void run(CodeModel code) {
        for (CodeElement element : code) {
          if (element instanceof ExceptionCatch handler && handler.catchType().isPresent()) {
            handlers
                .computeIfAbsent(handler.handler(), label -> new ArrayList<>())
                .add(handler.catchType().get());
          }
        }
        code.forEach(this::accept);
        checkCaughtTypes();
        handlers.values().forEach(types -> types.forEach(type -> checkClass(type, classLine)));
      }

      private void accept(CodeElement element) {
        if (element instanceof Instruction) {
          checkCaughtTypes();
        }
        switch (element) {
          case LabelTarget target -> {
            // The type of a catch clause is reported on the line where the clause starts.
            List<ClassEntry> caught = handlers.remove(target.label());
            if (caught != null) {
              caughtHere.addAll(caught);
            }
          }
          case LineNumber number -> {
            line = number.line();
            checkCaughtTypes();
          }
          case InvokeInstruction invoke -> checkMember(invoke.method(), line);
          case FieldInstruction field -> checkMember(field.field(), line);
          case InvokeDynamicInstruction dynamic -> checkDynamic(dynamic.invokedynamic(), line);
          case NewObjectInstruction creation -> checkClass(creation.className(), line);
          case TypeCheckInstruction check -> checkClass(check.type(), line);
          case NewReferenceArrayInstruction array -> checkClass(array.componentType(), line);
          case NewMultiArrayInstruction array -> checkClass(array.arrayType(), line);
          case ConstantInstruction.LoadConstantInstruction load ->
              checkConstant(load.constantEntry(), line);
          default -> {}
        }
      }

      private void checkCaughtTypes() {
        caughtHere.forEach(type -> checkClass(type, line));
        caughtHere.clear();
      }
    }

    private void checkClass(ClassEntry entry, int line) {
      String name = entry.asInternalName();
      if (!allowlist.allowsClass(name)) {
        report(readable(name), line);
      }
    }

    private void checkMember(MemberRefEntry member, int line) {
      String owner = member.owner().asInternalName();
      String name = member.name().stringValue();
      if (!allowlist.allowsMember(owner, name)) {
        report(symbol(owner, name), line);
      }
    }

    private void checkDynamic(InvokeDynamicEntry entry, int line) {
      MemberRefEntry bootstrap = entry.bootstrap().bootstrapMethod().reference();
      String owner = bootstrap.owner().asInternalName();
      String name = bootstrap.name().stringValue();
      if (!allowlist.allowsBootstrap(owner, name)) {
        report(symbol(owner, name), line);
      }
      // A method reference such as Runtime::exec travels as a bootstrap argument.
      for (LoadableConstantEntry argument : entry.bootstrap().arguments()) {
        if (argument instanceof MethodHandleEntry handle) {
          checkMember(handle.reference(), line);
        } else if (argument instanceof ConstantDynamicEntry) {
          report("java.lang.invoke.ConstantBootstraps", line);
        }
      }
    }

    private void checkConstant(LoadableConstantEntry constant, int line) {
      switch (constant) {
        case ClassEntry entry -> checkClass(entry, line);
        case MethodHandleEntry handle -> report("java.lang.invoke.MethodHandle", line);
        case MethodTypeEntry type -> report("java.lang.invoke.MethodType", line);
        case ConstantDynamicEntry dynamic -> report("java.lang.invoke.ConstantBootstraps", line);
        default -> {}
      }
    }

    /** Anything linkable that the instruction walk did not see is reported on the class line. */
    private void checkConstantPool() {
      Set<MemberRefEntry> bootstraps = new HashSet<>();
      for (int i = 0; i < model.constantPool().bootstrapMethodCount(); i++) {
        bootstraps.add(model.constantPool().bootstrapMethodEntry(i).bootstrapMethod().reference());
      }
      for (PoolEntry entry : model.constantPool()) {
        switch (entry) {
          case MemberRefEntry member -> {
            String owner = member.owner().asInternalName();
            String name = member.name().stringValue();
            boolean allowed =
                bootstraps.contains(member)
                    ? allowlist.allowsBootstrap(owner, name) || allowlist.allowsMember(owner, name)
                    : allowlist.allowsMember(owner, name);
            if (!allowed) {
              report(symbol(owner, name), classLine);
            }
          }
          case ConstantDynamicEntry dynamic ->
              report("java.lang.invoke.ConstantBootstraps", classLine);
          default -> {}
        }
      }
    }

    /** One violation per symbol and class: the first line where it shows up. */
    private void report(String symbol, int line) {
      if (reported.add(symbol)) {
        violations.add(new Violation(file, Math.max(1, line), symbol));
      }
    }

    private static String symbol(String owner, String member) {
      return readable(owner) + "." + member;
    }

    private static String readable(String internalName) {
      String name = internalName;
      int dimensions = 0;
      while (name.startsWith("[")) {
        name = name.substring(1);
        dimensions++;
      }
      if (dimensions > 0 && name.startsWith("L")) {
        name = name.substring(1, name.length() - 1);
      }
      return name.replace('/', '.') + "[]".repeat(dimensions);
    }
  }
}
