public class FacultadRegional {
  private String nombre;
  private String ciudad;
  private String provincia;

  FacultadRegional(String nombre, String ciudad, String provincia) {
    this.nombre = nombre;
    this.ciudad = ciudad;
    this.provincia = provincia;
  }

  public String getNombre() {
    return nombre;
  }

  public String getCiudad() {
    return ciudad;
  }

  public String getProvincia() {
    return provincia;
  }
}
