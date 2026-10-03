public class OrganoUnipersonal extends OrganoDeGobierno {
  String titular;

  OrganoUnipersonal(String nombre, int mandatoEnAnios, String titular) {
    super(nombre, mandatoEnAnios);
    this.titular = titular;
  }

  // Sobrescribí describirComposicion(): "Órgano unipersonal a cargo de <titular>".
}
