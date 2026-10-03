public class RegionalAcademica extends UnidadAcademica {
  FacultadRegional facultadDeQueDepende;

  // Reemplazá este constructor por uno que delegue en super(...).
  RegionalAcademica(String nombre, String ciudad, String provincia, FacultadRegional facultad) {
    this.nombre = nombre;
    this.ciudad = ciudad;
    this.provincia = provincia;
    this.facultadDeQueDepende = facultad;
  }
}
