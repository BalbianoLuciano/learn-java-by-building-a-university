public class CargoDeDirectorDeDepartamento implements CargoElectivo {
  String nombre = "Director de Departamento";

  @Override
  public String nombre() {
    return nombre;
  }

  @Override
  public String organoQueElige() {
    return "el Consejo Departamental";
  }

  @Override
  public int mandatoEnAnios() {
    return 4;
  }
}
