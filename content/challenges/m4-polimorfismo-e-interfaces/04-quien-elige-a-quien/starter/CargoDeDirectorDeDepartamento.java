// Tiene los métodos, pero no firma el contrato: agregá implements CargoElectivo.
public class CargoDeDirectorDeDepartamento {
  String nombre = "Director de Departamento";

  public String nombre() {
    return nombre;
  }

  public String organoQueElige() {
    return "el Consejo Departamental";
  }

  public int mandatoEnAnios() {
    return 4;
  }
}
