// Tiene los métodos, pero no firma el contrato: agregá implements CargoElectivo.
public class CargoDeDecano {
  String nombre = "Decano";

  public String nombre() {
    return nombre;
  }

  public String organoQueElige() {
    return "la Asamblea de la Facultad Regional";
  }

  public int mandatoEnAnios() {
    return 4;
  }
}
