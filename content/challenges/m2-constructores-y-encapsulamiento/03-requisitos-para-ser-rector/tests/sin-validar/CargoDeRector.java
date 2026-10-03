public class CargoDeRector {
  private Persona titular;

  CargoDeRector(Persona candidato) {
    // Rechazá con IllegalArgumentException a quien no sea argentino,
    // tenga menos de 30 años o no haya sido profesor universitario.
    this.titular = candidato;
  }

  public Persona getTitular() {
    return titular;
  }
}
