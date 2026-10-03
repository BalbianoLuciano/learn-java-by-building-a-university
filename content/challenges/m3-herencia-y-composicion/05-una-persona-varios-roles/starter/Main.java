public class Main {
  public static void main(String[] args) {
    Persona juana = new Persona("Juana Soto");
    juana.agregarRol(new Estudiante("Ingeniería en Sistemas de Información"));
    juana.agregarRol(new Auxiliar("Ayudante de 2ª"));

    juana.elegirClaustro("estudiantes");
    System.out.println(juana.nombre + " vota en el claustro de " + juana.claustroElectoral);

    try {
      juana.elegirClaustro("graduados");
      System.out.println("Eligió un claustro que no le corresponde");
    } catch (IllegalArgumentException e) {
      System.out.println("Rechazado: " + e.getMessage());
    }
  }
}
