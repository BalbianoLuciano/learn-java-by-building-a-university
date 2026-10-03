public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "FR Resistencia";
    resistencia.ciudad = "Resistencia";
    resistencia.provincia = "Chaco";

    // Una Regional Académica de ejemplo: la UTN no publica un listado con fuente.
    RegionalAcademica regional = new RegionalAcademica();
    regional.nombre = "Regional Académica de ejemplo";
    regional.ciudad = "Sáenz Peña";
    regional.provincia = "Chaco";
    regional.facultadDeQueDepende = resistencia;

    System.out.println(resistencia.describir());
    System.out.println(regional.describir() + ", depende de " + regional.facultadDeQueDepende.nombre);
  }
}
