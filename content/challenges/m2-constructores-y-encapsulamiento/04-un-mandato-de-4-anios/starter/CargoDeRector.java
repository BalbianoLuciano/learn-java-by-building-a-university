public class CargoDeRector {
  private Persona titular;
  private int desde;
  // Reemplazá este atributo por la constante MANDATO_EN_ANIOS.
  int mandatoEnAnios = 4;

  CargoDeRector(Persona titular, int desde) {
    this.titular = titular;
    this.desde = desde;
  }

  int anioDeFin() {
    // Devolvé el año en que termina el mandato.
    return 0;
  }

  public Persona getTitular() {
    return titular;
  }
}
