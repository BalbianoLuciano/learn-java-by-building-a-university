package io.github.balbianoluciano.ljbu.runner.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace;
import io.github.balbianoluciano.ljbu.runner.support.Programs;
import io.github.balbianoluciano.ljbu.runner.support.Traces;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** What the course needs must pass the allowlist and run inside the restricted child JVM. */
class AllowedCodeTest {

  static Stream<Arguments> allowedPrograms() {
    return Stream.of(
        arguments(
            "string concatenation and formatting",
            """
            String sede = "Resistencia";
            int anio = 1953;
            System.out.println(sede + " (" + anio + ")");
            System.out.printf("%s: %d%n", sede.toUpperCase(), sede.length());
            """,
            "Resistencia (1953)\nRESISTENCIA: 11\n"),
        arguments(
            "a string builder and math",
            """
            StringBuilder texto = new StringBuilder();
            texto.append(Math.max(3, 7)).append('-').append(Math.round(2.5));
            System.out.println(texto);
            """,
            "7-3\n"),
        arguments(
            "wrappers and parsing",
            """
            Integer cupo = Integer.parseInt("40");
            double promedio = Double.parseDouble("8.5");
            System.out.println(cupo + 1);
            System.out.println(promedio > 8 && Character.isLetter('a'));
            """,
            "41\ntrue\n"),
        arguments(
            "a list walked with for-each",
            """
            java.util.List<String> sedes = new java.util.ArrayList<>();
            sedes.add("Córdoba");
            sedes.add("Mendoza");
            for (String sede : sedes) {
              System.out.println(sede);
            }
            """,
            "Córdoba\nMendoza\n"),
        arguments(
            "a map walked by its entries",
            """
            java.util.Map<String, Integer> mandatos = new java.util.HashMap<>();
            mandatos.put("Rector", 4);
            for (java.util.Map.Entry<String, Integer> mandato : mandatos.entrySet()) {
              System.out.println(mandato.getKey() + "=" + mandato.getValue());
            }
            """,
            "Rector=4\n"),
        arguments(
            "a set and its iterator",
            """
            java.util.Set<Integer> anios = new java.util.HashSet<>();
            anios.add(2026);
            anios.add(2026);
            java.util.Iterator<Integer> cursor = anios.iterator();
            while (cursor.hasNext()) {
              System.out.println(cursor.next());
            }
            """,
            "2026\n"),
        arguments(
            "lambdas and method references",
            """
            java.util.List<String> sedes = new java.util.ArrayList<>(java.util.List.of("b", "a"));
            sedes.sort((uno, otro) -> uno.compareTo(otro));
            sedes.forEach(System.out::println);
            """,
            "a\nb\n"),
        arguments(
            "arrays and their helpers",
            """
            int[] cupos = {30, 10, 20};
            int[] copia = cupos.clone();
            java.util.Arrays.sort(copia);
            System.out.println(java.util.Arrays.toString(copia) + " " + cupos.length);
            """,
            "[10, 20, 30] 3\n"),
        arguments(
            "comparing with Objects",
            """
            String uno = null;
            System.out.println(java.util.Objects.equals(uno, null));
            """,
            "true\n"),
        arguments(
            "throwing and catching an exception",
            """
            try {
              throw new IllegalArgumentException("antigüedad insuficiente");
            } catch (IllegalArgumentException e) {
              System.out.println(e.getMessage());
            }
            """,
            "antigüedad insuficiente\n"),
        arguments(
            "a switch over a string",
            """
            String claustro = "graduados";
            switch (claustro) {
              case "docentes" -> System.out.println(1);
              case "graduados" -> System.out.println(2);
              default -> System.out.println(0);
            }
            """,
            "2\n"),
        arguments(
            "comparing references and contents",
            """
            String a = new String("UTN");
            String b = new String("UTN");
            System.out.println(a == b);
            System.out.println(a.equals(b));
            """,
            "false\ntrue\n"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("allowedPrograms")
  void runsWhatTheCourseNeeds(String feature, String body, String expectedOutput) {
    Trace.Executed trace = Traces.runToTheEnd(Programs.mainWithBody(body));

    assertThat(trace.executionStatus()).as(Traces.toJson(trace)).isEqualTo(Trace.Status.COMPLETED);
    assertThat(trace.stdout()).isEqualTo(expectedOutput);
  }
}
