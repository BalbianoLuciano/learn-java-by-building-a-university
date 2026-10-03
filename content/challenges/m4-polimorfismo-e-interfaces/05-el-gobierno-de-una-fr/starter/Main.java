public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("Resistencia");

    // Integrantes de ejemplo: las composiciones reales están en los arts. 106 y 108.
    OrganoColegiado asamblea = new OrganoColegiado("Asamblea de la Facultad Regional", 4);
    asamblea.integrantes.add("Decana");
    asamblea.integrantes.add("Consejo Directivo");
    asamblea.integrantes.add("Directores de Departamento");
    asamblea.integrantes.add("Consejos Departamentales");

    OrganoColegiado consejoDirectivo = new OrganoColegiado("Consejo Directivo", 2);
    consejoDirectivo.integrantes.add("Decana");
    consejoDirectivo.integrantes.add("Docente por Materias Básicas");
    consejoDirectivo.integrantes.add("Estudiante");
    consejoDirectivo.integrantes.add("Graduado");
    consejoDirectivo.integrantes.add("No docente");

    OrganoUnipersonal decanato = new OrganoUnipersonal("Decanato", 4, "Ana Lema");

    resistencia.agregarOrgano(asamblea);
    resistencia.agregarOrgano(consejoDirectivo);
    resistencia.agregarOrgano(decanato);

    resistencia.describirGobierno();
  }
}
