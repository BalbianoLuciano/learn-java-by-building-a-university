public class Main {
  public static void main(String[] args) {
    Persona marta = new Persona("Marta Quiroga");
    CargoDeRector rectora = new CargoDeRector(marta, 2022);

    System.out.println("Rectora: " + rectora.getTitular().getNombre());
    System.out.println("Fin del mandato: " + rectora.anioDeFin());
  }
}
