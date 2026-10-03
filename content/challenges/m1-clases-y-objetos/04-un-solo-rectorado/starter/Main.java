public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";
    resistencia.ciudad = "Resistencia";
    resistencia.provincia = "Chaco";

    FacultadRegional cordoba = new FacultadRegional();
    cordoba.nombre = "Córdoba";
    cordoba.ciudad = "Córdoba";
    cordoba.provincia = "Córdoba";

    FacultadRegional mendoza = new FacultadRegional();
    mendoza.nombre = "Mendoza";
    mendoza.ciudad = "Mendoza";
    mendoza.provincia = "Mendoza";

    // Creá el único Rectorado, con direccion "Sarmiento 440".

    // Hacé que las tres facultades apunten a él.

    // Imprimí "Mismo Rectorado" solo si resistencia.rectorado == cordoba.rectorado.
  }
}
