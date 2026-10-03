public class Main {
  public static void main(String[] args) {
    CargoDeRector rector = new CargoDeRector();
    CargoDeDecano decano = new CargoDeDecano();
    CargoDeDirectorDeDepartamento director = new CargoDeDirectorDeDepartamento();

    // Tres veces lo mismo. Reemplazalo por un arreglo CargoElectivo[] y un for.
    System.out.println(rector.nombre() + ": lo elige " + rector.organoQueElige() + " por " + rector.mandatoEnAnios() + " años");
    System.out.println(decano.nombre() + ": lo elige " + decano.organoQueElige() + " por " + decano.mandatoEnAnios() + " años");
  }
}
