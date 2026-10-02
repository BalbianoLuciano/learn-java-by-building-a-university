package io.github.balbianoluciano.ljbu.runner.execution.structure;

import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts;
import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts.MethodFacts;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.ClassInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.ConstructorInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.FieldInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.Kind;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.MethodInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.Visibility;
import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Reads the structure of the compiled classes with {@code java.lang.classfile}. Declaration lines
 * and {@code @Override} are not in the bytecode and come from {@link SourceFacts}.
 */
public final class StructureExtractor {

  private static final String CONSTRUCTOR = "<init>";
  private static final String STATIC_INITIALIZER = "<clinit>";

  private StructureExtractor() {}

  public static Structure extract(Collection<byte[]> classFiles, SourceFacts facts) {
    List<ClassInfo> classes = new ArrayList<>();
    for (byte[] bytes : classFiles) {
      classes.add(describe(ClassFile.of().parse(bytes), facts));
    }
    return new Structure(classes);
  }

  private static ClassInfo describe(ClassModel model, SourceFacts facts) {
    String name = model.thisClass().asInternalName();
    AccessFlags flags = model.flags();
    Kind kind = kind(model);
    String file =
        model
            .findAttribute(Attributes.sourceFile())
            .map(attribute -> attribute.sourceFile().stringValue())
            .orElse(name + ".java");

    List<FieldInfo> fields = new ArrayList<>();
    for (FieldModel field : model.fields()) {
      if (!field.flags().has(AccessFlag.SYNTHETIC)) {
        fields.add(describe(field, name, facts));
      }
    }

    List<ConstructorInfo> constructors = new ArrayList<>();
    List<MethodInfo> methods = new ArrayList<>();
    for (MethodModel method : model.methods()) {
      String methodName = method.methodName().stringValue();
      if (method.flags().has(AccessFlag.SYNTHETIC) || methodName.equals(STATIC_INITIALIZER)) {
        continue;
      }
      MethodFacts methodFacts = facts.method(name, methodName, method.methodType().stringValue());
      List<String> parameterTypes = parameterTypes(method, kind);
      Visibility visibility = visibility(method.flags());
      if (methodName.equals(CONSTRUCTOR)) {
        constructors.add(new ConstructorInfo(parameterTypes, visibility, methodFacts.line()));
      } else {
        methods.add(
            new MethodInfo(
                methodName,
                returnType(method),
                parameterTypes,
                visibility,
                method.flags().has(AccessFlag.STATIC),
                method.flags().has(AccessFlag.ABSTRACT),
                methodFacts.override(),
                methodFacts.line()));
      }
    }

    return new ClassInfo(
        name,
        kind,
        flags.has(AccessFlag.ABSTRACT),
        superclass(model, kind),
        model.interfaces().stream().map(entry -> TypeNames.simple(entry.asInternalName())).toList(),
        file,
        facts.classLine(name),
        fields,
        constructors,
        methods);
  }

  private static Kind kind(ClassModel model) {
    if (model.flags().has(AccessFlag.INTERFACE)) {
      return Kind.INTERFACE;
    }
    if (model.flags().has(AccessFlag.ENUM)) {
      return Kind.ENUM;
    }
    boolean isRecord =
        model
            .superclass()
            .map(ClassEntry::asInternalName)
            .filter("java/lang/Record"::equals)
            .isPresent();
    return isRecord ? Kind.RECORD : Kind.CLASS;
  }

  /** Null unless the source names a superclass: Object, Enum and Record are implicit. */
  private static String superclass(ClassModel model, Kind kind) {
    if (kind != Kind.CLASS) {
      return null;
    }
    return model
        .superclass()
        .map(ClassEntry::asInternalName)
        .filter(name -> !name.equals("java/lang/Object"))
        .map(TypeNames::simple)
        .orElse(null);
  }

  private static FieldInfo describe(FieldModel field, String className, SourceFacts facts) {
    String fieldName = field.fieldName().stringValue();
    Optional<String> generic =
        field
            .findAttribute(Attributes.signature())
            .map(
                attribute ->
                    TypeNames.of(Signature.parseFrom(attribute.signature().stringValue())));
    return new FieldInfo(
        fieldName,
        generic.orElseGet(() -> TypeNames.of(field.fieldTypeSymbol())),
        visibility(field.flags()),
        field.flags().has(AccessFlag.FINAL),
        field.flags().has(AccessFlag.STATIC),
        facts.fieldLine(className, fieldName));
  }

  private static List<String> parameterTypes(MethodModel method, Kind kind) {
    Optional<MethodSignature> generic = genericSignature(method);
    if (generic.isPresent()) {
      return generic.get().arguments().stream().map(TypeNames::of).toList();
    }
    List<String> types =
        method.methodTypeSymbol().parameterList().stream().map(TypeNames::of).toList();
    boolean enumConstructor =
        kind == Kind.ENUM && method.methodName().stringValue().equals(CONSTRUCTOR);
    // javac prepends the name and the ordinal to every enum constructor.
    return enumConstructor && types.size() >= 2 ? types.subList(2, types.size()) : types;
  }

  private static String returnType(MethodModel method) {
    return genericSignature(method)
        .map(signature -> TypeNames.of(signature.result()))
        .orElseGet(() -> TypeNames.of(method.methodTypeSymbol().returnType()));
  }

  private static Optional<MethodSignature> genericSignature(MethodModel method) {
    return method
        .findAttribute(Attributes.signature())
        .map(attribute -> MethodSignature.parseFrom(attribute.signature().stringValue()));
  }

  private static Visibility visibility(AccessFlags flags) {
    if (flags.has(AccessFlag.PUBLIC)) {
      return Visibility.PUBLIC;
    }
    if (flags.has(AccessFlag.PROTECTED)) {
      return Visibility.PROTECTED;
    }
    if (flags.has(AccessFlag.PRIVATE)) {
      return Visibility.PRIVATE;
    }
    return Visibility.PACKAGE;
  }
}
