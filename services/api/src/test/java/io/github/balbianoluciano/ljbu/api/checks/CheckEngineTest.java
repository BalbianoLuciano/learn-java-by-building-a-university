package io.github.balbianoluciano.ljbu.api.checks;

import static io.github.balbianoluciano.ljbu.api.support.Programs.file;
import static io.github.balbianoluciano.ljbu.api.support.Programs.program;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.balbianoluciano.ljbu.api.checks.CheckParams.Expectation;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams.FieldCondition;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams.FieldOfAll;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams.LocalRef;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams.ObjectSelector;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams.Reference;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.RunResult.SourceRef;
import io.github.balbianoluciano.ljbu.api.support.Programs;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Every check type, evaluated on what the real runner reports for small programs. */
class CheckEngineTest {

  private static final SourceFile FACULTAD =
      file(
          "FacultadRegional.java",
          """
          public class FacultadRegional {
            String nombre;
            String provincia;
            private final int fundacion = 1953;
            static int creadas;
            Rectorado rectorado;

            void cargarProvincia(String provincia) {
              this.provincia = provincia;
            }
          }
          """);

  private static final SourceFile RECTORADO = file("Rectorado.java", "public class Rectorado {}");

  /** Two faculties that share one Rectorado, and an alias of the first one. */
  private static RunFacts faculties;

  /** A small hierarchy of governing bodies with constructors that delegate. */
  private static RunFacts bodies;

  @BeforeAll
  static void runPrograms() {
    faculties =
        Programs.facts(
            program(
                """
                Rectorado rectorado = new Rectorado();
                FacultadRegional resistencia = new FacultadRegional();
                resistencia.nombre = "Resistencia";
                resistencia.rectorado = rectorado;
                FacultadRegional cordoba = new FacultadRegional();
                cordoba.nombre = "Córdoba";
                cordoba.rectorado = rectorado;
                FacultadRegional miFacultad = resistencia;
                miFacultad.provincia = "Chaco";
                cordoba.cargarProvincia("Córdoba");
                System.out.println("Listo: " + resistencia.nombre);
                """,
                FACULTAD,
                RECTORADO));
    bodies =
        Programs.facts(
            program(
                """
                OrganoDeGobierno[] organos = {new ConsejoSuperior(), new Rector("Rector")};
                for (OrganoDeGobierno organo : organos) {
                  organo.describir();
                }
                try {
                  new Rector("");
                } catch (IllegalArgumentException e) {
                  System.out.println(e.getMessage());
                }
                """,
                file(
                    "Electivo.java",
                    """
                    public interface Electivo {
                      int mandatoEnAnios();
                    }
                    """),
                file(
                    "OrganoDeGobierno.java",
                    """
                    public abstract class OrganoDeGobierno {
                      protected final String nombre;

                      protected OrganoDeGobierno(String nombre) {
                        if (nombre.isEmpty()) {
                          throw new IllegalArgumentException("El nombre no puede estar vacío");
                        }
                        this.nombre = nombre;
                      }

                      public abstract String describir();

                      public String nombre() {
                        return nombre;
                      }
                    }
                    """),
                file(
                    "ConsejoSuperior.java",
                    """
                    public class ConsejoSuperior extends OrganoDeGobierno {
                      public ConsejoSuperior() {
                        this("Consejo Superior");
                      }

                      public ConsejoSuperior(String nombre) {
                        super(nombre);
                      }

                      @Override
                      public String describir() {
                        return "colegiado";
                      }
                    }
                    """),
                file(
                    "Rector.java",
                    """
                    public class Rector extends OrganoDeGobierno implements Electivo {
                      public Rector(String nombre) {
                        super(nombre);
                      }

                      @Override
                      public String describir() {
                        return "unipersonal";
                      }

                      @Override
                      public int mandatoEnAnios() {
                        return 4;
                      }
                    }
                    """)));
  }

  // --- Structure

  @Test
  void classExistsFindsADeclaredClass() {
    assertThat(passes(new CheckParams.ClassExists("FacultadRegional"), faculties)).isTrue();
    assertThat(passes(new CheckParams.ClassExists("Departamento"), faculties)).isFalse();
  }

  @Test
  void extendsLooksAtTheDeclaredSuperclass() {
    assertThat(passes(new CheckParams.Extends("Rector", "OrganoDeGobierno"), bodies)).isTrue();
    assertThat(passes(new CheckParams.Extends("Rector", "ConsejoSuperior"), bodies)).isFalse();
    assertThat(passes(new CheckParams.Extends("OrganoDeGobierno", "Object"), bodies)).isFalse();
  }

  @Test
  void implementsLooksAtTheDeclaredInterfaces() {
    assertThat(passes(new CheckParams.Implements("Rector", "Electivo"), bodies)).isTrue();
    assertThat(passes(new CheckParams.Implements("ConsejoSuperior", "Electivo"), bodies)).isFalse();
  }

  @Test
  void isAbstractWorksForClassesAndForMethods() {
    assertThat(passes(new CheckParams.IsAbstract("OrganoDeGobierno", null), bodies)).isTrue();
    assertThat(passes(new CheckParams.IsAbstract("Rector", null), bodies)).isFalse();
    assertThat(passes(new CheckParams.IsAbstract("OrganoDeGobierno", "describir"), bodies))
        .isTrue();
    assertThat(passes(new CheckParams.IsAbstract("OrganoDeGobierno", "nombre"), bodies)).isFalse();
  }

  @Test
  void fieldChecksTypeAndModifiers() {
    assertThat(passes(field("nombre", "String", null, null, null), faculties)).isTrue();
    assertThat(passes(field("nombre", "int", null, null, null), faculties)).isFalse();
    assertThat(passes(field("fundacion", "int", "private", true, false), faculties)).isTrue();
    assertThat(passes(field("fundacion", null, "public", null, null), faculties)).isFalse();
    assertThat(passes(field("creadas", null, null, null, true), faculties)).isTrue();
  }

  @Test
  void fieldNamesTheFieldsThatAreMissing() {
    CheckOutcome outcome =
        CheckEngine.evaluate(
            new CheckParams.Field(
                "FacultadRegional",
                null,
                List.of("nombre", "ciudad", "sigla"),
                "String",
                null,
                null,
                null),
            faculties);

    assertThat(outcome.passed()).isFalse();
    assertThat(outcome.placeholders()).containsEntry("missing", "`ciudad`, `sigla`");
    assertThat(outcome.lines().declaration()).isEqualTo(new SourceRef("FacultadRegional.java", 2));
  }

  @Test
  void constructorChecksParametersAndVisibility() {
    assertThat(passes(constructor("Rector", List.of("String"), "public", null), bodies)).isTrue();
    assertThat(passes(constructor("Rector", List.of(), null, null), bodies)).isFalse();
    assertThat(passes(constructor("OrganoDeGobierno", List.of("String"), "public", null), bodies))
        .isFalse();
  }

  @Test
  void constructorSeesDelegationToThisAndToSuper() {
    assertThat(passes(constructor("ConsejoSuperior", List.of(), null, "this"), bodies)).isTrue();
    assertThat(passes(constructor("ConsejoSuperior", List.of("String"), null, "super"), bodies))
        .isTrue();
    assertThat(passes(constructor("ConsejoSuperior", List.of("String"), null, "this"), bodies))
        .isFalse();
    assertThat(passes(constructor("Rector", List.of("String"), null, "super"), bodies)).isTrue();
  }

  @Test
  void methodChecksSignatureAndOverride() {
    assertThat(passes(method("Rector", "describir", List.of(), "String", true), bodies)).isTrue();
    assertThat(passes(method("Rector", "describir", List.of("int"), null, null), bodies)).isFalse();
    assertThat(passes(method("OrganoDeGobierno", "nombre", null, null, true), bodies)).isFalse();
    assertThat(passes(method("Rector", "mandatoEnAnios", null, "int", null), bodies)).isTrue();
    assertThat(passes(method("Rector", "jubilar", null, null, null), bodies)).isFalse();
  }

  // --- Execution

  @Test
  void objectCountCountsObjectsOfATypeAndOfItsSubtypes() {
    assertThat(count("FacultadRegional", null, 2, null, null, faculties).passed()).isTrue();
    assertThat(count("FacultadRegional", null, null, 3, null, faculties).passed()).isFalse();
    assertThat(count("FacultadRegional", null, null, null, 2, faculties).passed()).isTrue();
    // Two bodies of the array plus the Rector that was rejected while it was being built.
    assertThat(count("OrganoDeGobierno", null, 3, null, null, bodies).passed()).isTrue();
  }

  @Test
  void objectCountCanCountOnlyTheObjectsThatMeetACondition() {
    FieldCondition named = new FieldCondition("nombre", "Córdoba", null);
    CheckOutcome outcome = count("FacultadRegional", named, 0, null, null, faculties);

    assertThat(outcome.passed()).isFalse();
    assertThat(outcome.placeholders()).containsEntry("count", "1");
  }

  @Test
  void objectFieldLooksAtTheFinalValueOfTheSelectedObject() {
    ObjectSelector resistencia =
        new ObjectSelector("FacultadRegional", "nombre", "Resistencia", null);

    assertThat(passes(objectField(resistencia, expectEquals("provincia", "Chaco")), faculties))
        .isTrue();
    assertThat(passes(objectField(resistencia, expectEquals("provincia", "Córdoba")), faculties))
        .isFalse();
    assertThat(passes(objectField(resistencia, expectEquals("fundacion", 1953)), faculties))
        .isTrue();
    assertThat(
            passes(
                objectField(resistencia, new Expectation("rectorado", null, true, null)),
                faculties))
        .isTrue();
    assertThat(
            passes(
                objectField(resistencia, new Expectation("rectorado", null, null, true)),
                faculties))
        .isFalse();
  }

  @Test
  void objectFieldNamesWhatIsNotAsExpectedAndPointsToTheLastWrite() {
    ObjectSelector cordoba = new ObjectSelector("FacultadRegional", "nombre", "Córdoba", null);
    CheckOutcome outcome =
        CheckEngine.evaluate(
            new CheckParams.ObjectField(
                cordoba,
                null,
                List.of(expectEquals("nombre", "Córdoba"), expectEquals("provincia", "Mendoza")),
                null),
            faculties);

    assertThat(outcome.passed()).isFalse();
    assertThat(outcome.placeholders()).containsEntry("missing", "`provincia`");
    // Written inside cargarProvincia.
    assertThat(outcome.lines().lastWrite()).isEqualTo(new SourceRef("FacultadRegional.java", 9));
    assertThat(outcome.lines().objectCreation()).isEqualTo(new SourceRef("Main.java", 7));
  }

  @Test
  void objectFieldFailsWhenNoObjectIsSelected() {
    ObjectSelector mendoza = new ObjectSelector("FacultadRegional", "nombre", "Mendoza", null);
    CheckOutcome outcome =
        CheckEngine.evaluate(objectField(mendoza, expectEquals("provincia", "Mendoza")), faculties);

    assertThat(outcome.passed()).isFalse();
    assertThat(outcome.placeholders()).containsEntry("missing", "`provincia`");
  }

  @Test
  void objectFieldCanRequireEveryObjectToMeetTheExpectation() {
    ObjectSelector every = new ObjectSelector("FacultadRegional", null, null, null);
    ObjectSelector others = new ObjectSelector("FacultadRegional", "nombre", null, "Resistencia");

    assertThat(passes(all(every, new Expectation("rectorado", null, true, null)), faculties))
        .isTrue();
    assertThat(passes(all(every, expectEquals("provincia", "Chaco")), faculties)).isFalse();
    assertThat(passes(all(others, expectEquals("provincia", "Córdoba")), faculties)).isTrue();
  }

  @Test
  void objectFieldKnowsThroughWhichVariableAFieldWasWritten() {
    ObjectSelector resistencia =
        new ObjectSelector("FacultadRegional", "nombre", "Resistencia", null);
    ObjectSelector cordoba = new ObjectSelector("FacultadRegional", "nombre", "Córdoba", null);

    assertThat(passes(through(resistencia, "provincia", "Chaco", "miFacultad"), faculties))
        .isTrue();
    assertThat(passes(through(resistencia, "provincia", "Chaco", "resistencia"), faculties))
        .isFalse();
    // The write happens inside a method; what counts is the variable of the call in main.
    assertThat(passes(through(cordoba, "provincia", "Córdoba", "cordoba"), faculties)).isTrue();
    assertThat(passes(through(cordoba, "provincia", "Córdoba", "miFacultad"), faculties)).isFalse();
  }

  @Test
  void sharedReferenceComparesTwoVariables() {
    Reference resistencia = new Reference("resistencia", null, null, null);
    Reference alias = new Reference("miFacultad", "Main.main", null, null);
    Reference cordoba = new Reference("cordoba", null, null, null);
    Reference missing = new Reference("mendoza", null, null, null);

    CheckOutcome shared =
        CheckEngine.evaluate(new CheckParams.SharedReference(resistencia, alias, null), faculties);

    assertThat(shared.passed()).isTrue();
    assertThat(shared.lines().declaration()).isEqualTo(new SourceRef("Main.java", 10));
    assertThat(passes(new CheckParams.SharedReference(resistencia, cordoba, null), faculties))
        .isFalse();
    assertThat(passes(new CheckParams.SharedReference(resistencia, missing, null), faculties))
        .isFalse();
  }

  @Test
  void sharedReferenceComparesAVariableWithTheFieldOfAnObject() {
    Reference rectorado = new Reference("rectorado", null, null, null);
    Reference ofCordoba =
        new Reference(
            null,
            null,
            new ObjectSelector("FacultadRegional", "nombre", "Córdoba", null),
            "rectorado");

    assertThat(passes(new CheckParams.SharedReference(rectorado, ofCordoba, null), faculties))
        .isTrue();
  }

  @Test
  void sharedReferenceCanRequireEveryObjectToPointToTheSameOne() {
    assertThat(passes(sharedByAll("FacultadRegional", "rectorado"), faculties)).isTrue();
    assertThat(passes(sharedByAll("FacultadRegional", "nombre"), faculties)).isFalse();
  }

  @Test
  void callDispatchTellsWhichImplementationRan() {
    assertThat(passes(new CheckParams.CallDispatch("describir", "Rector", "Rector"), bodies))
        .isTrue();
    assertThat(
            passes(
                new CheckParams.CallDispatch("describir", "ConsejoSuperior", "ConsejoSuperior"),
                bodies))
        .isTrue();
    assertThat(
            passes(new CheckParams.CallDispatch("describir", "Rector", "ConsejoSuperior"), bodies))
        .isFalse();
    assertThat(
            passes(new CheckParams.CallDispatch("describir", "Rector", "OrganoDeGobierno"), bodies))
        .isFalse();
  }

  @Test
  void stdoutContainsLooksForATextInTheOutput() {
    assertThat(passes(new CheckParams.StdoutContains("Listo: Resistencia", null), faculties))
        .isTrue();
    assertThat(passes(new CheckParams.StdoutContains("listo", null), faculties)).isFalse();
    assertThat(passes(new CheckParams.StdoutContains("listo", true), faculties)).isTrue();
  }

  @Test
  void noExceptionPassesWhenTheProgramEndsOnItsOwn() {
    RunFacts broken = Programs.facts(program("String texto = null;\ntexto.length();"));

    assertThat(passes(new CheckParams.NoException(), faculties)).isTrue();
    assertThat(passes(new CheckParams.NoException(), broken)).isFalse();
  }

  @Test
  void throwsFindsAnExceptionByTypeMessageAndFate() {
    assertThat(passes(new CheckParams.Throws("IllegalArgumentException", null, null), bodies))
        .isTrue();
    assertThat(
            passes(
                new CheckParams.Throws("java.lang.IllegalArgumentException", "vacío", true),
                bodies))
        .isTrue();
    assertThat(passes(new CheckParams.Throws("IllegalArgumentException", null, false), bodies))
        .isFalse();
    assertThat(passes(new CheckParams.Throws("IllegalStateException", null, null), bodies))
        .isFalse();
    assertThat(passes(new CheckParams.Throws("IllegalArgumentException", "negativo", null), bodies))
        .isFalse();
  }

  // --- Helpers

  private static boolean passes(CheckParams params, RunFacts facts) {
    params.validate();
    return CheckEngine.evaluate(params, facts).passed();
  }

  private static CheckParams.Field field(
      String name, String type, String visibility, Boolean isFinal, Boolean isStatic) {
    return new CheckParams.Field(
        "FacultadRegional", name, null, type, visibility, isFinal, isStatic);
  }

  private static CheckParams.Constructor constructor(
      String type, List<String> parameterTypes, String visibility, String delegatesTo) {
    return new CheckParams.Constructor(type, parameterTypes, visibility, delegatesTo);
  }

  private static CheckParams.Method method(
      String type, String name, List<String> parameterTypes, String returnType, Boolean override) {
    return new CheckParams.Method(
        type, name, parameterTypes, returnType, null, null, null, override);
  }

  private static CheckOutcome count(
      String type,
      FieldCondition where,
      Integer exactly,
      Integer atLeast,
      Integer atMost,
      RunFacts facts) {
    return CheckEngine.evaluate(
        new CheckParams.ObjectCount(type, where, exactly, atLeast, atMost), facts);
  }

  private static Expectation expectEquals(String field, Object value) {
    return new Expectation(field, value, null, null);
  }

  private static CheckParams.ObjectField objectField(
      ObjectSelector where, Expectation expectation) {
    return new CheckParams.ObjectField(where, null, List.of(expectation), null);
  }

  private static CheckParams.ObjectField all(ObjectSelector where, Expectation expectation) {
    return new CheckParams.ObjectField(where, true, List.of(expectation), null);
  }

  private static CheckParams.ObjectField through(
      ObjectSelector where, String field, Object value, String variable) {
    return new CheckParams.ObjectField(
        where, null, List.of(expectEquals(field, value)), new LocalRef(variable, null));
  }

  private static CheckParams.SharedReference sharedByAll(String type, String field) {
    return new CheckParams.SharedReference(null, null, new FieldOfAll(type, field));
  }
}
