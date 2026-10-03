public class Main {
  public static void main(String[] args) {
    // Un Consejo Directivo reducido, de ejemplo: el real se arma según el art. 108.
    OrganoColegiado consejoDirectivo = new OrganoColegiado("Consejo Directivo", 2);
    consejoDirectivo.integrantes.add("Decano");
    consejoDirectivo.integrantes.add("Docente por Materias Básicas");
    consejoDirectivo.integrantes.add("Docente por Sistemas");
    consejoDirectivo.integrantes.add("Estudiante");
    consejoDirectivo.integrantes.add("No docente");

    OrganoUnipersonal decanato = new OrganoUnipersonal("Decanato", 4, "Ana Lema");

    OrganoDeGobierno[] organos = {consejoDirectivo, decanato};
    for (OrganoDeGobierno organo : organos) {
      System.out.println(organo.nombre + ": " + organo.describirComposicion());
    }
  }
}
