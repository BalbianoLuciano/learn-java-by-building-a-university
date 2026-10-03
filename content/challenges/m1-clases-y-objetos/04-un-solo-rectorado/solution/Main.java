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

    Rectorado rectorado = new Rectorado();
    rectorado.direccion = "Sarmiento 440";

    resistencia.rectorado = rectorado;
    cordoba.rectorado = rectorado;
    mendoza.rectorado = rectorado;

    if (resistencia.rectorado == cordoba.rectorado) {
      System.out.println("Mismo Rectorado");
    }
  }
}
