public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("FR Resistencia", "Resistencia", "Chaco");

    // Una Regional Académica de ejemplo: la UTN no publica un listado con fuente.
    RegionalAcademica regional =
        new RegionalAcademica("Regional Académica de ejemplo", "Sáenz Peña", "Chaco", resistencia);

    System.out.println(resistencia.describir());
    System.out.println(regional.describir() + ", depende de " + regional.facultadDeQueDepende.nombre);
  }
}
