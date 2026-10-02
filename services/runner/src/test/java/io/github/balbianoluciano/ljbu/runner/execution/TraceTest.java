package io.github.balbianoluciano.ljbu.runner.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.runner.execution.trace.HeapObject;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Step;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** What the trace says about programs that run. Every trace is also checked against the schema. */
class TraceTest {

  @Test
  void showsAliasingAsTwoVariablesPointingToOneObject() {
    Trace.Executed trace = Traces.runToTheEnd(Programs.named("aliasing"));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.COMPLETED);
    assertThat(steps(trace, Step.ObjectCreated.class)).hasSize(1);
    assertThat(localsOf(trace, "Main.main"))
        .containsEntry("resistencia", new Value.RefValue("o1"))
        .containsEntry("miFacultad", new Value.RefValue("o1"));
    assertThat(fields(trace, "o1"))
        .containsEntry("ciudad", new Value.StringValue("Resistencia, Chaco"));
    assertThat(trace.stdout()).isEqualTo("Resistencia, Chaco\n");
  }

  @Test
  void placesAFieldWriteOnTheLineThatMadeIt() {
    Trace.Executed trace = Traces.runToTheEnd(Programs.named("aliasing"));

    Step.FieldSet lastWrite = steps(trace, Step.FieldSet.class).getLast();

    assertThat(lastWrite.file()).isEqualTo("Main.java");
    assertThat(lastWrite.line()).isEqualTo(9);
    assertThat(lastWrite.target()).isEqualTo("o1");
    assertThat(lastWrite.field()).isEqualTo("ciudad");
  }

  @Test
  void runsTheInventoryProgramToTheEnd() {
    Trace.Executed trace = Traces.runToTheEnd(Programs.named("inventory"));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.COMPLETED);
    assertThat(trace.limits().truncated()).isFalse();
    assertThat(trace.exception()).isNull();
    assertThat(trace.stdout())
        .isEqualTo(
            """
            Rechazado: El precio no puede ser negativo: -1.0
            B-2 Lapicera x1 retirado=true
            Valor total: 80300.5
            Con stock bajo: 2
            """);
  }

  @Test
  void readsTheElementsOfAListFromTheFinalHeap() {
    Trace.Executed trace = Traces.runToTheEnd(Programs.named("inventory"));

    Value productos = fields(trace, "o1").get("productos");
    HeapObject list = trace.heap().get(((Value.RefValue) productos).id());

    assertThat(list)
        .isInstanceOfSatisfying(
            HeapObject.Sequence.class,
            sequence -> {
              assertThat(sequence.type()).isEqualTo("ArrayList");
              assertThat(sequence.size()).isEqualTo(3);
              assertThat(sequence.elements()).allMatch(Value.RefValue.class::isInstance);
            });
  }

  @Test
  void recordsAnExceptionTheLearnerCatchesAndGoesOn() {
    Trace.Executed trace = Traces.runToTheEnd(Programs.named("inventory"));

    List<Step.ExceptionThrown> thrown = steps(trace, Step.ExceptionThrown.class);

    assertThat(thrown).hasSize(1);
    assertThat(thrown.getFirst().caught()).isTrue();
    assertThat(thrown.getFirst().exception().type())
        .isEqualTo("java.lang.IllegalArgumentException");
    assertThat(thrown.getFirst().exception().message())
        .isEqualTo("El precio no puede ser negativo: -1.0");
    assertThat(thrown.getFirst().file()).isEqualTo("Producto.java");
  }

  @Test
  void tiesTheTextOfAPrintWithoutNewlineToItsOwnLine() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                System.out.print("Nombre: ");
                System.out.println("Resistencia");
                """));

    assertThat(steps(trace, Step.Output.class))
        .extracting(Step.Output::line, Step.Output::text)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(3, "Nombre: "),
            org.assertj.core.groups.Tuple.tuple(4, "Resistencia\n"));
  }

  @Test
  void keepsAccentsInTheOutput() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                System.out.println("Tecnológica · año 2026 · ñandú");
                """));

    assertThat(trace.stdout()).isEqualTo("Tecnológica · año 2026 · ñandú\n");
  }

  @Test
  void buildsTheSuperclassPartBeforeTheSubclassPart() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "UnidadAcademica.java",
                    """
                    public class UnidadAcademica {
                      protected String nombre;

                      public UnidadAcademica(String nombre) {
                        this.nombre = nombre;
                      }
                    }
                    """),
                Programs.file(
                    "FacultadRegional.java",
                    """
                    public class FacultadRegional extends UnidadAcademica {
                      private int departamentos;

                      public FacultadRegional(String nombre, int departamentos) {
                        super(nombre);
                        this.departamentos = departamentos;
                      }
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {
                        FacultadRegional fr = new FacultadRegional("Resistencia", 5);
                      }
                    }
                    """)));

    assertThat(steps(trace, Step.ObjectCreated.class))
        .singleElement()
        .satisfies(
            created -> {
              assertThat(created.object().type()).isEqualTo("FacultadRegional");
              assertThat(created.file()).isEqualTo("Main.java");
              assertThat(created.line()).isEqualTo(3);
            });
    assertThat(steps(trace, Step.Call.class))
        .extracting(Step.Call::method)
        .containsExactly("Main.main", "FacultadRegional.<init>", "UnidadAcademica.<init>");
    assertThat(steps(trace, Step.Return.class))
        .extracting(Step.Return::method)
        .containsExactly("UnidadAcademica.<init>", "FacultadRegional.<init>", "Main.main");
    assertThat(steps(trace, Step.FieldSet.class))
        .extracting(Step.FieldSet::field)
        .containsExactly("nombre", "departamentos");
    assertThat(fields(trace, "o1").keySet()).containsExactly("nombre", "departamentos");
  }

  @Test
  void namesTheImplementationThatAPolymorphicCallRuns() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "OrganoDeGobierno.java",
                    """
                    public abstract class OrganoDeGobierno {
                      public abstract String describirComposicion();
                    }
                    """),
                Programs.file(
                    "OrganoColegiado.java",
                    """
                    public class OrganoColegiado extends OrganoDeGobierno {
                      @Override
                      public String describirComposicion() {
                        return "varios integrantes";
                      }
                    }
                    """),
                Programs.file(
                    "OrganoUnipersonal.java",
                    """
                    public class OrganoUnipersonal extends OrganoDeGobierno {
                      @Override
                      public String describirComposicion() {
                        return "una persona";
                      }
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {
                        OrganoDeGobierno[] organos = {new OrganoColegiado(), new OrganoUnipersonal()};
                        for (OrganoDeGobierno organo : organos) {
                          System.out.println(organo.describirComposicion());
                        }
                      }
                    }
                    """)));

    assertThat(steps(trace, Step.Call.class))
        .filteredOn(call -> call.method().endsWith(".describirComposicion"))
        .extracting(Step.Call::method, Step.Call::target)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("OrganoColegiado.describirComposicion", "o1"),
            org.assertj.core.groups.Tuple.tuple("OrganoUnipersonal.describirComposicion", "o2"));
    assertThat(trace.stdout()).isEqualTo("varios integrantes\nuna persona\n");
  }

  @Test
  void readsTheElementsOfAnArrayFromTheFinalHeap() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                int[] cupos = new int[3];
                cupos[1] = 40;
                String[] sedes = {"Resistencia", null};
                """));

    Map<String, Value> locals = localsOf(trace, "Main.main");
    HeapObject cupos = trace.heap().get(((Value.RefValue) locals.get("cupos")).id());
    HeapObject sedes = trace.heap().get(((Value.RefValue) locals.get("sedes")).id());

    assertThat(cupos)
        .isEqualTo(
            new HeapObject.Sequence(
                "int[]",
                3,
                List.of(new Value.IntValue(0), new Value.IntValue(40), new Value.IntValue(0))));
    assertThat(sedes)
        .isEqualTo(
            new HeapObject.Sequence(
                "String[]",
                2,
                List.of(new Value.StringValue("Resistencia"), new Value.NullValue())));
  }

  @Test
  void readsTheEntriesOfAMapFromTheFinalHeap() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                java.util.Map<String, Integer> mandatos = new java.util.HashMap<>();
                mandatos.put("Rector", 4);
                """));

    Value mandatos = localsOf(trace, "Main.main").get("mandatos");

    assertThat(trace.heap().get(((Value.RefValue) mandatos).id()))
        .isEqualTo(
            new HeapObject.MapObject(
                "HashMap",
                1,
                List.of(
                    new HeapObject.Entry(new Value.StringValue("Rector"), new Value.IntValue(4)))));
  }

  @Test
  void treatsStringsAndWrappersAsValues() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                String nombre = "UTN";
                Integer anio = 1948;
                Double promedio = 8.5;
                char inicial = 'U';
                boolean activa = true;
                long alumnos = 85000L;
                Object nada = null;
                """));

    assertThat(localsOf(trace, "Main.main"))
        .containsEntry("nombre", new Value.StringValue("UTN"))
        .containsEntry("anio", new Value.IntValue(1948))
        .containsEntry("promedio", new Value.DoubleValue(8.5))
        .containsEntry("inicial", new Value.CharValue("U"))
        .containsEntry("activa", new Value.BooleanValue(true))
        .containsEntry("alumnos", new Value.IntValue(85000))
        .containsEntry("nada", new Value.NullValue());
    assertThat(trace.heap()).isEmpty();
  }

  @Test
  void writesDoublesThatAreNotFiniteAsText() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                double cero = 0.0;
                double indefinido = cero / cero;
                double infinito = 1 / cero;
                """));

    assertThat(localsOf(trace, "Main.main"))
        .containsEntry("indefinido", new Value.DoubleValue("NaN"))
        .containsEntry("infinito", new Value.DoubleValue("Infinity"));
  }

  @Test
  void recordsStaticFieldsByTheirClass() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "Cargo.java",
                    """
                    public class Cargo {
                      static int creados = 0;

                      public Cargo() {
                        creados++;
                      }
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {
                        new Cargo();
                        new Cargo();
                      }
                    }
                    """)));

    assertThat(steps(trace, Step.FieldSet.class))
        .extracting(Step.FieldSet::ownerClass, Step.FieldSet::field, Step.FieldSet::value)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Cargo", "creados", new Value.IntValue(0)),
            org.assertj.core.groups.Tuple.tuple("Cargo", "creados", new Value.IntValue(1)),
            org.assertj.core.groups.Tuple.tuple("Cargo", "creados", new Value.IntValue(2)));
    assertThat(trace.statics())
        .isEqualTo(Map.of("Cargo", Map.of("creados", new Value.IntValue(2))));
  }

  @Test
  void endsWithTheUncaughtExceptionAndTheLearnerLine() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "FacultadRegional.java",
                    """
                    public class FacultadRegional {
                      String nombre;
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    public class Main {
                      public static void main(String[] args) {
                        FacultadRegional fr = null;
                        System.out.println("antes");
                        fr.nombre = "Resistencia";
                        System.out.println("después");
                      }
                    }
                    """)));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.RUNTIME_ERROR);
    assertThat(trace.stdout()).isEqualTo("antes\n");
    assertThat(trace.exception().type()).isEqualTo("java.lang.NullPointerException");
    assertThat(trace.exception().message()).contains("\"fr\" is null");
    assertThat(trace.exception().file()).isEqualTo("Main.java");
    assertThat(trace.exception().line()).isEqualTo(5);
    assertThat(trace.steps().getLast())
        .isInstanceOfSatisfying(
            Step.ExceptionThrown.class, thrown -> assertThat(thrown.caught()).isFalse());
    assertThat(trace.limits().truncated()).isFalse();
  }

  @Test
  void pointsToTheLearnerLineWhenTheJdkThrows() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.mainWithBody(
                """
                java.util.List<String> sedes = new java.util.ArrayList<>();
                String primera = sedes.get(0);
                """));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.RUNTIME_ERROR);
    assertThat(trace.exception().type()).isEqualTo("java.lang.IndexOutOfBoundsException");
    assertThat(trace.exception().file()).isEqualTo("Main.java");
    assertThat(trace.exception().line()).isEqualTo(4);
  }

  @Test
  void reportsAnExceptionInAStaticInitializer() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.main(
                """
                public class Main {
                  static int divisor = 0;
                  static int resultado = 10 / divisor;

                  public static void main(String[] args) {}
                }
                """));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.RUNTIME_ERROR);
    assertThat(trace.exception().type()).isEqualTo("java.lang.ArithmeticException");
    assertThat(trace.exception().line()).isEqualTo(3);
  }

  @Test
  void tracesCodeCalledBackFromTheJdk() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "Departamento.java",
                    """
                    public class Departamento {
                      private final String nombre;

                      public Departamento(String nombre) {
                        this.nombre = nombre;
                      }

                      @Override
                      public String toString() {
                        String texto = "Depto. " + nombre;
                        return texto;
                      }
                    }
                    """),
                Programs.file(
                    "Main.java",
                    """
                    import java.util.List;

                    public class Main {
                      public static void main(String[] args) {
                        List<Departamento> departamentos = List.of(new Departamento("Sistemas"));
                        departamentos.forEach(departamento -> System.out.println(departamento));
                      }
                    }
                    """)));

    assertThat(trace.stdout()).isEqualTo("Depto. Sistemas\n");
    assertThat(steps(trace, Step.Call.class))
        .extracting(Step.Call::method)
        .containsExactly("Main.main", "Departamento.<init>", "Departamento.toString");
    assertThat(localsOf(trace, "Departamento.toString"))
        .containsEntry("texto", new Value.StringValue("Depto. Sistemas"));
  }

  @Test
  void runsEnumsRecordsAndLambdas() {
    Trace.Executed trace =
        Traces.runToTheEnd(
            Programs.files(
                Programs.file(
                    "Claustro.java",
                    """
                    public enum Claustro {
                      DOCENTES,
                      GRADUADOS,
                      ESTUDIANTES
                    }
                    """),
                Programs.file(
                    "Banca.java",
                    """
                    public record Banca(Claustro claustro, int numero) {}
                    """),
                Programs.file(
                    "Main.java",
                    """
                    import java.util.ArrayList;
                    import java.util.List;

                    public class Main {
                      public static void main(String[] args) {
                        List<Banca> bancas = new ArrayList<>();
                        for (Claustro claustro : Claustro.values()) {
                          bancas.add(new Banca(claustro, bancas.size() + 1));
                        }
                        bancas.removeIf(banca -> banca.claustro() == Claustro.GRADUADOS);
                        switch (bancas.get(1).claustro()) {
                          case ESTUDIANTES -> System.out.println(bancas.get(1));
                          default -> System.out.println("otro");
                        }
                      }
                    }
                    """)));

    assertThat(trace.executionStatus()).isEqualTo(Trace.Status.COMPLETED);
    assertThat(trace.stdout()).isEqualTo("Banca[claustro=ESTUDIANTES, numero=3]\n");
    assertThat(trace.statics().get("Claustro").keySet())
        .containsExactly("DOCENTES", "GRADUADOS", "ESTUDIANTES");
  }

  // --- Helpers

  private static <T extends Step> List<T> steps(Trace.Executed trace, Class<T> type) {
    return trace.steps().stream().filter(type::isInstance).map(type::cast).toList();
  }

  /** The last value each local variable of the method received. */
  private static Map<String, Value> localsOf(Trace.Executed trace, String method) {
    Map<String, Value> locals = new java.util.LinkedHashMap<>();
    for (Step.LocalSet write : steps(trace, Step.LocalSet.class)) {
      if (write.method().equals(method)) {
        locals.put(write.name(), write.value());
      }
    }
    return locals;
  }

  private static Map<String, Value> fields(Trace.Executed trace, String id) {
    return ((HeapObject.Instance) trace.heap().get(id)).fields();
  }
}
