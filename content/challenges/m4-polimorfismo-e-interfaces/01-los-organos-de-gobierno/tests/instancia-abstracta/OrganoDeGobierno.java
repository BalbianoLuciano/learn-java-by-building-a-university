public abstract class OrganoDeGobierno {
  String nombre;
  int mandatoEnAnios;

  OrganoDeGobierno(String nombre, int mandatoEnAnios) {
    this.nombre = nombre;
    this.mandatoEnAnios = mandatoEnAnios;
  }

  abstract String describirComposicion();
}
