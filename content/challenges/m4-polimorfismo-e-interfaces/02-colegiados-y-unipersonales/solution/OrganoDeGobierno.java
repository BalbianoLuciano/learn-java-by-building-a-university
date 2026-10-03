public abstract class OrganoDeGobierno {
  String nombre;
  int mandatoEnAnios;

  OrganoDeGobierno(String nombre, int mandatoEnAnios) {
    this.nombre = nombre;
    this.mandatoEnAnios = mandatoEnAnios;
  }

  String describirComposicion() {
    return "Órgano de gobierno";
  }
}
