public class RegionalAcademica extends UnidadAcademica {
  FacultadRegional facultadDeQueDepende;

  RegionalAcademica(String nombre, String ciudad, String provincia, FacultadRegional facultad) {
    super(nombre, ciudad, provincia);
    this.facultadDeQueDepende = facultad;
  }
}
