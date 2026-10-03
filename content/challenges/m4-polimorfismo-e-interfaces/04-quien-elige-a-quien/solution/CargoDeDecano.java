public class CargoDeDecano implements CargoElectivo {
  String nombre = "Decano";

  @Override
  public String nombre() {
    return nombre;
  }

  @Override
  public String organoQueElige() {
    return "la Asamblea de la Facultad Regional";
  }

  @Override
  public int mandatoEnAnios() {
    return 4;
  }
}
