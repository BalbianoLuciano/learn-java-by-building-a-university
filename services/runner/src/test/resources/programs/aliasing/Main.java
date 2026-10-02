public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";
    resistencia.ciudad = "Resistencia";
    resistencia.provincia = "Chaco";

    FacultadRegional miFacultad = resistencia;
    miFacultad.ciudad = "Resistencia, Chaco";

    System.out.println(resistencia.ciudad);
  }
}
