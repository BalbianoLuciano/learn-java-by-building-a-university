public class FacultadRegional {
  String nombre;
  String ciudad;
  String provincia;
  Rectorado rectorado;

  String describir() {
    return "FR " + nombre + " (" + ciudad + ", " + provincia + ")";
  }
}
