// Hacé que extienda UnidadAcademica y borrá lo que ya hereda.
public class FacultadRegional {
  String nombre;
  String ciudad;
  String provincia;

  String describir() {
    return nombre + " (" + ciudad + ", " + provincia + ")";
  }
}
