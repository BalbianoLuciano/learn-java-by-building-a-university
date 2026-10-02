public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";

    Decano decano = new Decano();
    decano.nombre = "Decana de prueba";
    resistencia.asignar(decano);

    Departamento basicas = new Departamento();
    basicas.nombre = "Materias Básicas";
    resistencia.departamentos.add(basicas);

    resistencia.asignar(null);
  }
}
