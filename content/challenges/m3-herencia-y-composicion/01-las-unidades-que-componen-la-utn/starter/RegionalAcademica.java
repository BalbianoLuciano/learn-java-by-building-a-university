// Hacé que extienda UnidadAcademica y borrá lo que ya hereda.
public class RegionalAcademica {
  String nombre;
  String ciudad;
  String provincia;
  FacultadRegional facultadDeQueDepende;

  String describir() {
    return nombre + " (" + ciudad + ", " + provincia + ")";
  }
}
