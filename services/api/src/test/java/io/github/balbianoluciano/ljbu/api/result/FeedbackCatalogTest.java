package io.github.balbianoluciano.ljbu.api.result;

import static io.github.balbianoluciano.ljbu.api.support.Programs.file;
import static io.github.balbianoluciano.ljbu.api.support.Programs.mainWithBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.RunResult.LogEntry;
import io.github.balbianoluciano.ljbu.api.support.Programs;
import io.github.balbianoluciano.ljbu.api.support.TestContent;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The catalogs of content/feedback against what javac and the JVM really say: every entry must
 * recognise its error and fill its placeholders.
 */
class FeedbackCatalogTest {

  private static final Content CONTENT = TestContent.real();
  private static final ResultBuilder RESULTS = new ResultBuilder(CONTENT.feedback());

  private static final SourceFile ORGANO =
      file(
          "Organo.java",
          """
          public abstract class Organo {
            private String nombre;
            final int mandato = 4;

            Organo(String nombre) {
              this.nombre = nombre;
            }

            abstract String describir();
          }
          """);

  static Stream<Arguments> compilationErrors() {
    return Stream.of(
        arguments(
            "unknown name", main("Rectorado rectorado = null;"), "Java no conoce `Rectorado`."),
        arguments("unknown method", main("\"UTN\".gritar();"), "Java no conoce `gritar`."),
        arguments("missing semicolon", main("int cupos = 40"), "Falta un `;` cerca de acá."),
        arguments(
            "wrong type",
            main("int cupos = \"cuarenta\";"),
            "Acá va un `int` y recibió un `String`."),
        arguments(
            "wrong arguments", main("\"UTN\".charAt();"), "`charAt` no recibe esos argumentos."),
        arguments(
            "private member",
            List.of(mainWithBody("new Concreto().nombre = \"x\";"), ORGANO, concreto("")),
            "`nombre` es privado de `Organo`."),
        arguments(
            "abstract method not implemented",
            List.of(
                mainWithBody(""),
                ORGANO,
                file(
                    "Concreto.java",
                    "public class Concreto extends Organo { Concreto() { super(\"x\"); } }")),
            "`Concreto` no implementa `describir`."),
        arguments(
            "text outside the class",
            List.of(
                file(
                    "Main.java",
                    "Main.java\npublic class Main {\n  public static void main(String[] args) {}\n}\n")),
            "Hay texto fuera de cualquier clase en esta línea."),
        arguments(
            "pasted code fence",
            List.of(
                file(
                    "Main.java",
                    "```java\npublic class Main {\n  public static void main(String[] args) {}\n}\n```\n")),
            "Java no reconoce el carácter '`' en esta línea."),
        arguments(
            "override of nothing",
            List.of(
                mainWithBody(""),
                ORGANO,
                concreto("@Override String describirr() { return \"x\"; }")),
            "Este método lleva `@Override`, pero no sobrescribe ningún método."),
        arguments(
            "abstract class instantiated",
            List.of(mainWithBody("new Organo(\"x\");"), ORGANO),
            "No se puede crear un objeto de `Organo` con `new`."),
        arguments(
            "final reassigned",
            List.of(mainWithBody("new Concreto().mandato = 5;"), ORGANO, concreto("")),
            "`mandato` es `final`: no se le puede volver a asignar."),
        arguments(
            "missing return",
            List.of(
                mainWithBody(""), file("Cuenta.java", "public class Cuenta { int total() { } }")),
            "A este método le falta un `return`."),
        arguments(
            "object used before super",
            List.of(mainWithBody(""), ORGANO, concreto("Concreto(int x) { super(describir()); }")),
            "Usaste `describir` antes de llamar al constructor de la superclase."),
        arguments(
            "instance member from static",
            List.of(
                file(
                    "Main.java",
                    "public class Main {\n  int cupos;\n  public static void main(String[] args) {\n    cupos = 1;\n  }\n}\n")),
            "`cupos` necesita un objeto."),
        arguments(
            "local without value",
            main("String sede;\nSystem.out.println(sede);"),
            "`sede` puede no tener valor todavía."),
        arguments(
            "public class in another file",
            List.of(mainWithBody(""), file("Otra.java", "public class Facultad {}")),
            "La clase `Facultad` tiene que estar en `Facultad.java`."),
        arguments(
            "name declared twice",
            main("int cupos = 1;\nint cupos = 2;"),
            "`cupos` ya está declarado."),
        arguments(
            "missing closing brace",
            List.of(
                file(
                    "Main.java",
                    "public class Main {\n  public static void main(String[] args) {\n")),
            "El archivo termina antes de lo esperado."),
        arguments(
            "unclosed text", main("String sede = \"Resistencia;"), "Hay un texto sin cerrar."),
        arguments("loose value", main("5;"), "Esta línea no es una instrucción completa."),
        arguments("missing value", main("int cupos = ;"), "Acá falta un valor o sobra un símbolo."),
        arguments(
            "package declaration",
            List.of(
                file(
                    "Main.java",
                    "package utn;\npublic class Main {\n  public static void main(String[] args) {}\n}\n")),
            "En este curso los archivos no llevan `package`."),
        arguments(
            "no main method",
            List.of(file("Main.java", "public class Main {}")),
            "`Main` necesita el método `public static void main(String[] args)`."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("compilationErrors")
  void explainsCompilationErrors(String error, List<SourceFile> files, String title) {
    LogEntry entry = firstEntry(files);

    assertThat(entry.title()).isEqualTo(title);
    assertThat(entry.state()).isEqualTo(RunResult.FAILED);
    assertThat(entry.detail()).as("javac text is shown only for unknown codes").isNull();
    assertThat(entry.sourceRef()).isNotNull();
    assertThat(entry.hint()).isNotBlank();
  }

  @Test
  void showsWhatJavacSaidForAnErrorWithoutAnEntry() {
    LogEntry entry = firstEntry(main("int cupos = 1;\nbreak;"));

    assertThat(entry.title()).isEqualTo("Java no pudo compilar esta línea.");
    assertThat(entry.detail()).isEqualTo("break outside switch or loop");
  }

  static Stream<Arguments> exceptions() {
    return Stream.of(
        arguments(
            "null reference",
            "String sede = null;\nsede.length();",
            "Usaste `sede`, pero apunta a `null`."),
        arguments(
            "division by zero",
            "int cero = 0;\nint x = 10 / cero;",
            "Una división por cero frenó la obra."),
        arguments(
            "array index",
            "int[] cupos = new int[3];\ncupos[5] = 1;",
            "Pediste la posición `5` y solo hay `3`."),
        arguments(
            "list index",
            "new java.util.ArrayList<String>().get(0);",
            "Pediste la posición `0` y solo hay `0`."),
        arguments(
            "bad cast",
            "Object texto = \"UTN\";\nInteger numero = (Integer) texto;",
            "Un `String` no es un `Integer`."),
        arguments(
            "illegal argument with its own message",
            "throw new IllegalArgumentException(\"La antigüedad mínima es de 3 años\");",
            "La antigüedad mínima es de 3 años"),
        arguments(
            "illegal argument without message",
            "throw new IllegalArgumentException();",
            "El programa se frenó con `IllegalArgumentException`."),
        arguments(
            "number format",
            "Integer.parseInt(\"tres\");",
            "`\"tres\"` no se puede convertir a número."),
        arguments(
            "out of memory",
            "long[] enorme = new long[50_000_000];",
            "Se pidió más memoria de la que el programa tiene."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("exceptions")
  void explainsUncaughtExceptions(String exception, String body, String title) {
    LogEntry entry = firstEntry(main(body));

    assertThat(entry.title()).isEqualTo(title);
    assertThat(entry.state()).isEqualTo(RunResult.FAILED);
    assertThat(entry.sourceRef().file()).isEqualTo("Main.java");
  }

  @Test
  void showsTheTypeAndTheMessageOfAnExceptionWithoutAnEntry() {
    LogEntry entry = firstEntry(main("throw new UnsupportedOperationException(\"todavía no\");"));

    assertThat(entry.title())
        .isEqualTo("El programa se frenó con `UnsupportedOperationException`.");
    assertThat(entry.detail()).isEqualTo("java.lang.UnsupportedOperationException: todavía no");
  }

  static Stream<Arguments> limits() {
    return Stream.of(
        arguments(
            "runaway recursion",
            List.of(
                file(
                    "Main.java",
                    "public class Main {\n  static void bajar() { bajar(); }\n  public static void main(String[] args) { bajar(); }\n}\n")),
            "Un método se llamó a sí mismo sin parar."),
        arguments(
            "too many objects",
            List.of(
                mainWithBody("while (true) new Banca();"),
                file("Banca.java", "public class Banca {}")),
            "El programa creó demasiados objetos y lo frenamos."),
        arguments(
            "too much output",
            main("String linea = \"x\".repeat(500);\nwhile (true) System.out.println(linea);"),
            "El programa imprimió demasiado texto y lo frenamos."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("limits")
  void explainsProgramsCutByALimit(String limit, List<SourceFile> files, String title) {
    assertThat(firstEntry(files).title()).isEqualTo(title);
  }

  static Stream<Arguments> rejections() {
    return Stream.of(
        arguments("System.exit(0);", "`System.exit` no está disponible."),
        arguments(
            "new java.util.Scanner(\"x\");",
            "En este curso el programa no lee datos del teclado: `java.util.Scanner` no está disponible."),
        arguments(
            "new java.io.File(\"notas.txt\");",
            "En este curso no hace falta leer ni escribir archivos: `java.io.File` no está disponible."),
        arguments(
            "new Thread().start();",
            "En este curso el programa hace una cosa por vez: `java.lang.Thread` no está disponible."),
        arguments(
            "new RuntimeException().printStackTrace();", "`printStackTrace()` no está disponible."),
        arguments(
            "new java.util.Random();", "`java.util.Random` no está disponible en este curso."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejections")
  void explainsRejectedCode(String statement, String title) {
    LogEntry entry = firstEntry(main(statement));

    assertThat(entry.title()).isEqualTo(title);
    assertThat(entry.sourceRef()).isEqualTo(new RunResult.SourceRef("Main.java", 3));
  }

  // --- Helpers

  private static List<SourceFile> main(String body) {
    return List.of(mainWithBody(body));
  }

  private static SourceFile concreto(String extraMembers) {
    return file(
        "Concreto.java",
        """
        public class Concreto extends Organo {
          Concreto() {
            super("concreto");
          }

          %s

          String describir() {
            return "concreto";
          }
        }
        """
            .formatted(extraMembers));
  }

  /** The first log entry of running the files as if they were an attempt at challenge 1.3. */
  private static LogEntry firstEntry(List<SourceFile> files) {
    RunResult result =
        RESULTS.build(CONTENT.challenge("m1-03").orElseThrow(), Programs.trace(files), files, "es");
    return result.log().getFirst();
  }
}
