package io.github.balbianoluciano.ljbu.runner.execution.structure;

import java.lang.classfile.Signature;
import java.lang.constant.ClassDesc;
import java.util.stream.Collectors;

/** Writes types the way a learner reads them: simple names, with their type arguments. */
public final class TypeNames {

  private TypeNames() {}

  /** {@code java/util/List} becomes {@code List}; {@code Outer$Inner} stays as it is. */
  public static String simple(String internalOrQualifiedName) {
    int cut =
        Math.max(
            internalOrQualifiedName.lastIndexOf('/'), internalOrQualifiedName.lastIndexOf('.'));
    return internalOrQualifiedName.substring(cut + 1);
  }

  public static String of(ClassDesc type) {
    if (type.isArray()) {
      return of(type.componentType()) + "[]";
    }
    return type.displayName();
  }

  public static String of(Signature signature) {
    return switch (signature) {
      case Signature.BaseTypeSig base ->
          ClassDesc.ofDescriptor(String.valueOf(base.baseType())).displayName();
      case Signature.ArrayTypeSig array -> of(array.componentSignature()) + "[]";
      case Signature.TypeVarSig variable -> variable.identifier();
      case Signature.ClassTypeSig type -> {
        String name = simple(type.className());
        if (type.typeArgs().isEmpty()) {
          yield name;
        }
        yield name
            + type.typeArgs().stream()
                .map(TypeNames::of)
                .collect(Collectors.joining(", ", "<", ">"));
      }
    };
  }

  private static String of(Signature.TypeArg argument) {
    return switch (argument) {
      case Signature.TypeArg.Unbounded unbounded -> "?";
      case Signature.TypeArg.Bounded bounded ->
          switch (bounded.wildcardIndicator()) {
            case NONE -> of(bounded.boundType());
            case EXTENDS -> "? extends " + of(bounded.boundType());
            case SUPER -> "? super " + of(bounded.boundType());
          };
    };
  }
}
