package io.github.balbianoluciano.ljbu.runner.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.execution.compile.InMemoryCompiler;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.ClassInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.FieldInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.MethodInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Structure.Visibility;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Compilation errors and the structure read from the compiled classes. */
class CompilationTest {

  @Test
  void reportsACompilationErrorWithItsJavacCodeAndPosition() {
    Trace trace =
        Traces.run(
            Programs.mainWithBody(
                """
                FacultadRegional fr = new FacultadRegional();
                """));

    assertThat(trace)
        .isInstanceOfSatisfying(
            Trace.CompileError.class,
            error -> {
              Trace.Diagnostic first = error.diagnostics().getFirst();
              assertThat(first.code()).isEqualTo("compiler.err.cant.resolve.location");
              assertThat(first.file()).isEqualTo("Main.java");
              assertThat(first.line()).isEqualTo(3);
              assertThat(first.column()).isEqualTo(5);
              assertThat(first.message()).contains("FacultadRegional");
            });
  }

  @Test
  void reportsASyntaxError() {
    Trace trace = Traces.run(Programs.mainWithBody("int cupos = 3"));

    assertThat(trace)
        .isInstanceOfSatisfying(
            Trace.CompileError.class,
            error ->
                assertThat(error.diagnostics())
                    .extracting(Trace.Diagnostic::code)
                    .containsExactly("compiler.err.expected"));
  }

  @Test
  void rejectsAPackageDeclaration() {
    Trace trace =
        Traces.run(
            Programs.main(
                """
                package utn;

                public class Main {
                  public static void main(String[] args) {}
                }
                """));

    assertThat(trace)
        .isInstanceOfSatisfying(
            Trace.CompileError.class,
            error ->
                assertThat(error.diagnostics())
                    .extracting(Trace.Diagnostic::code, Trace.Diagnostic::line)
                    .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                            InMemoryCompiler.PACKAGE_NOT_ALLOWED, 1)));
  }

  @Test
  void requiresTheClassicMainMethod() {
    Trace trace =
        Traces.run(
            Programs.main(
                """
                public class Main {
                  void main() {}
                }
                """));

    assertThat(trace)
        .isInstanceOfSatisfying(
            Trace.CompileError.class,
            error ->
                assertThat(error.diagnostics())
                    .extracting(Trace.Diagnostic::code)
                    .containsExactly(Executor.MAIN_NOT_FOUND));
  }

  @Test
  void describesFieldsWithTheirTypeArgumentsAndModifiers() {
    ClassInfo inventario = structureOf("inventory", "Inventario");

    assertThat(inventario.fields())
        .containsExactly(
            new FieldInfo("productos", "List<Producto>", Visibility.PRIVATE, true, false, 5));
  }

  @Test
  void describesMethodsWithTheirDeclarationLineAndOverride() {
    ClassInfo producto = structureOf("inventory", "Producto");

    assertThat(producto.methods())
        .contains(
            new MethodInfo(
                "retirar", "boolean", List.of("int"), Visibility.PUBLIC, false, false, false, 25),
            // The line of the declaration, not the line of the annotation above it.
            new MethodInfo(
                "toString", "String", List.of(), Visibility.PUBLIC, false, false, true, 38));
    assertThat(producto.constructors())
        .singleElement()
        .satisfies(
            constructor -> {
              assertThat(constructor.parameterTypes())
                  .containsExactly("String", "String", "double", "int");
              assertThat(constructor.line()).isEqualTo(7);
            });
  }

  @Test
  void describesInheritanceAbstractClassesAndInterfaces() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "CargoElectivo.java",
                    """
                    public interface CargoElectivo {
                      int mandatoEnAnios();
                    }
                    """),
                Programs.file(
                    "OrganoDeGobierno.java",
                    """
                    public abstract class OrganoDeGobierno {
                      public abstract String describirComposicion();
                    }
                    """),
                Programs.file(
                    "Rector.java",
                    """
                    public class Rector extends OrganoDeGobierno implements CargoElectivo {
                      public static final int MANDATO_EN_ANIOS = 4;

                      @Override
                      public String describirComposicion() {
                        return "una persona";
                      }

                      @Override
                      public int mandatoEnAnios() {
                        return MANDATO_EN_ANIOS;
                      }
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {}
                    }
                    """)));

    ClassInfo cargo = classNamed(trace.structure(), "CargoElectivo");
    ClassInfo organo = classNamed(trace.structure(), "OrganoDeGobierno");
    ClassInfo rector = classNamed(trace.structure(), "Rector");

    assertThat(cargo.kind()).isEqualTo(Structure.Kind.INTERFACE);
    assertThat(organo.isAbstract()).isTrue();
    assertThat(organo.methods()).singleElement().matches(MethodInfo::isAbstract);
    assertThat(rector.superclass()).isEqualTo("OrganoDeGobierno");
    assertThat(rector.interfaces()).containsExactly("CargoElectivo");
    assertThat(rector.methods()).allMatch(MethodInfo::isOverride);
    assertThat(rector.fields())
        .containsExactly(
            new FieldInfo("MANDATO_EN_ANIOS", "int", Visibility.PUBLIC, true, true, 2));
  }

  private static ClassInfo structureOf(String program, String className) {
    return classNamed(Traces.runToTheEnd(Programs.named(program)).structure(), className);
  }

  private static ClassInfo classNamed(Structure structure, String name) {
    return structure.classes().stream()
        .filter(type -> type.name().equals(name))
        .findFirst()
        .orElseThrow();
  }
}
