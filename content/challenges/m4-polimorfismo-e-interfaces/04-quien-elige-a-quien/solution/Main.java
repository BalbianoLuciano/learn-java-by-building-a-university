public class Main {
  public static void main(String[] args) {
    CargoDeRector rector = new CargoDeRector();
    CargoDeDecano decano = new CargoDeDecano();
    CargoDeDirectorDeDepartamento director = new CargoDeDirectorDeDepartamento();

    CargoElectivo[] cargos = {rector, decano, director};
    for (CargoElectivo cargo : cargos) {
      System.out.println(
          cargo.nombre() + ": lo elige " + cargo.organoQueElige() + " por " + cargo.mandatoEnAnios() + " años");
    }
  }
}
