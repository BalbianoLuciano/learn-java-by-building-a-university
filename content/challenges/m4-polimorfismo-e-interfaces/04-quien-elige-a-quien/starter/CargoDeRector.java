public class CargoDeRector implements CargoElectivo {
  String nombre = "Rector";

  @Override
  public String nombre() {
    return nombre;
  }

  @Override
  public String organoQueElige() {
    return "la Asamblea Universitaria";
  }

  @Override
  public int mandatoEnAnios() {
    return 4;
  }
}
