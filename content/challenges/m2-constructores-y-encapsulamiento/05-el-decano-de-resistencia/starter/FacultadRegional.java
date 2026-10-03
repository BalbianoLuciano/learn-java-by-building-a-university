public class FacultadRegional {
  private String nombre;
  private CargoDeDecano decano;

  FacultadRegional(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }

  public CargoDeDecano getDecano() {
    return decano;
  }

  /** Nombra decano a la persona desde ese año. Devuelve si el cambio se hizo. */
  public boolean asignarDecano(Persona persona, int desde) {
    // Si la persona tiene menos de 3 años de antigüedad, no cambies nada y devolvé false.
    // Si cumple, creá el cargo, guardalo en decano y devolvé true.
    return false;
  }
}
