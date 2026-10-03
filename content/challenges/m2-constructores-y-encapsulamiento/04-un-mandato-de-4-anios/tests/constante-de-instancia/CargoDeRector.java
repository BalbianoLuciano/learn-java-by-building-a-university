public class CargoDeRector {
  int MANDATO_EN_ANIOS = 4;

  private Persona titular;
  private int desde;

  CargoDeRector(Persona titular, int desde) {
    this.titular = titular;
    this.desde = desde;
  }

  int anioDeFin() {
    return desde + MANDATO_EN_ANIOS;
  }

  public Persona getTitular() {
    return titular;
  }
}
