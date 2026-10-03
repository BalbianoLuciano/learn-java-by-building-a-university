public class UnidadAcademica {
  String nombre;
  String ciudad;
  String provincia;

  UnidadAcademica(String nombre, String ciudad, String provincia) {
    this.nombre = nombre;
    this.ciudad = ciudad;
    this.provincia = provincia;
  }

  String describir() {
    return nombre + " (" + ciudad + ", " + provincia + ")";
  }
}
