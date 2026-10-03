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
    if (persona.getAntiguedadDocente() < CargoDeDecano.ANTIGUEDAD_MINIMA) {
      return false;
    }
    decano = new CargoDeDecano(persona, desde);
    return true;
  }
}
