public class Main {
  public static void main(String[] args) {
    Rectorado rectorado = new Rectorado();
    rectorado.direccion = "Sarmiento 440";

    FacultadRegional resistencia = new FacultadRegional();
    resistencia.nombre = "Resistencia";
    resistencia.ciudad = "Resistencia";
    resistencia.provincia = "Chaco";
    resistencia.rectorado = rectorado;

    FacultadRegional cordoba = new FacultadRegional();
    cordoba.nombre = "Córdoba";
    cordoba.ciudad = "Córdoba";
    cordoba.provincia = "Córdoba";
    cordoba.rectorado = rectorado;

    FacultadRegional mendoza = new FacultadRegional();
    mendoza.nombre = "Mendoza";
    mendoza.ciudad = "Mendoza";
    mendoza.provincia = "Mendoza";
    mendoza.rectorado = rectorado;

    FacultadRegional rosario = new FacultadRegional();
    rosario.nombre = "Rosario";
    rosario.ciudad = "Rosario";
    rosario.provincia = "Santa Fe";
    rosario.rectorado = rectorado;

    FacultadRegional santaFe = new FacultadRegional();
    santaFe.nombre = "Santa Fe";
    santaFe.ciudad = "Santa Fe";
    santaFe.provincia = "Santa Fe";
    santaFe.rectorado = rectorado;

    FacultadRegional[] facultades = {resistencia, cordoba, mendoza, rosario, santaFe};

    for (FacultadRegional facultad : facultades) {
      System.out.println(facultad.describir());
    }
  }
}
