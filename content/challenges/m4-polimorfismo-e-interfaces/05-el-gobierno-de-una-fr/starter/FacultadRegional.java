public class FacultadRegional {
  String nombre;
  // Reemplazá estos tres atributos por una List<OrganoDeGobierno> organos.
  OrganoColegiado asamblea;
  OrganoColegiado consejoDirectivo;
  OrganoUnipersonal decanato;

  FacultadRegional(String nombre) {
    this.nombre = nombre;
  }

  void agregarOrgano(OrganoDeGobierno organo) {
    if (organo instanceof OrganoUnipersonal unipersonal) {
      decanato = unipersonal;
    } else if (organo instanceof OrganoColegiado colegiado && asamblea == null) {
      asamblea = colegiado;
    } else if (organo instanceof OrganoColegiado colegiado) {
      consejoDirectivo = colegiado;
    }
  }

  void describirGobierno() {
    // Preguntar qué es cada órgano y describirlo desde afuera repite lo que cada clase ya sabe.
    if (asamblea != null) {
      System.out.println(asamblea.nombre + ": colegiado de " + asamblea.integrantes.size());
    }
    if (consejoDirectivo != null) {
      System.out.println(consejoDirectivo.nombre + ": colegiado de " + consejoDirectivo.integrantes.size());
    }
    if (decanato != null) {
      System.out.println(decanato.nombre + ": a cargo de " + decanato.titular);
    }
  }
}
