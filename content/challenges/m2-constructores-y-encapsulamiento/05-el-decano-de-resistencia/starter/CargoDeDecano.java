public class CargoDeDecano {
  static final int MANDATO_EN_ANIOS = 4;
  static final int ANTIGUEDAD_MINIMA = 3;
  static final int ANIO_ACTUAL = 2026;

  private Persona titular;
  private int desde;

  CargoDeDecano(Persona titular, int desde) {
    if (titular.getAntiguedadDocente() < ANTIGUEDAD_MINIMA) {
      throw new IllegalArgumentException("Para ser Decano hacen falta 3 años de antigüedad docente");
    }
    this.titular = titular;
    this.desde = desde;
  }

  // Este constructor repite la regla: hacé que delegue en el otro con this(...).
  CargoDeDecano(Persona titular) {
    if (titular.getAntiguedadDocente() < ANTIGUEDAD_MINIMA) {
      throw new IllegalArgumentException("Para ser Decano hacen falta 3 años de antigüedad docente");
    }
    this.titular = titular;
    this.desde = ANIO_ACTUAL;
  }

  public Persona getTitular() {
    return titular;
  }

  public int anioDeFin() {
    return desde + MANDATO_EN_ANIOS;
  }
}
