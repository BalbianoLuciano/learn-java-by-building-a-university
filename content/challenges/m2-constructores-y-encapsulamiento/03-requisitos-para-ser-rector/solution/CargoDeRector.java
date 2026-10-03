public class CargoDeRector {
  private Persona titular;

  CargoDeRector(Persona candidato) {
    if (!candidato.getNacionalidad().equals("argentina")) {
      throw new IllegalArgumentException("Para ser Rector hay que ser argentino");
    }
    if (candidato.getEdad() < 30) {
      throw new IllegalArgumentException("Para ser Rector hay que tener al menos 30 años");
    }
    if (!candidato.fueProfesorUniversitario()) {
      throw new IllegalArgumentException("Para ser Rector hay que ser o haber sido profesor");
    }
    this.titular = candidato;
  }

  public Persona getTitular() {
    return titular;
  }
}
