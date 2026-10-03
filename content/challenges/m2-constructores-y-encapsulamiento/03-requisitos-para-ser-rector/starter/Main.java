public class Main {
  public static void main(String[] args) {
    Persona marta = new Persona("Marta Quiroga", "argentina", 52, true);
    CargoDeRector rectora = new CargoDeRector(marta);
    System.out.println("Rectora: " + rectora.getTitular().getNombre());

    Persona julian = new Persona("Julián Páez", "argentina", 29, true);
    try {
      new CargoDeRector(julian);
      System.out.println("Aceptado: " + julian.getNombre());
    } catch (IllegalArgumentException e) {
      System.out.println("Rechazado: " + e.getMessage());
    }
  }
}
