public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("Resistencia");

    Persona lucia = new Persona("Lucía Benítez", 12);
    if (resistencia.asignarDecano(lucia, 2024)) {
      System.out.println("Decana: " + resistencia.getDecano().getTitular().getNombre());
    }

    Persona tomas = new Persona("Tomás Ríos", 1);
    if (resistencia.asignarDecano(tomas, 2026)) {
      System.out.println("Decano: " + tomas.getNombre());
    } else {
      System.out.println("Rechazado: " + tomas.getNombre());
    }

    CargoDeDecano interino = new CargoDeDecano(lucia);
    System.out.println("Un cargo que empieza hoy termina en " + interino.anioDeFin());
  }
}
