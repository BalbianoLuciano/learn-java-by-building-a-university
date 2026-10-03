import java.util.ArrayList;
import java.util.List;

public class FacultadRegional {
  String nombre;
  List<OrganoDeGobierno> organos = new ArrayList<>();

  FacultadRegional(String nombre) {
    this.nombre = nombre;
  }

  void agregarOrgano(OrganoDeGobierno organo) {
    organos.add(organo);
  }

  void describirGobierno() {
    for (OrganoDeGobierno organo : organos) {
      if (organo instanceof OrganoColegiado colegiado) {
        System.out.println(organo.nombre + ": Órgano colegiado de " + colegiado.integrantes.size() + " integrantes");
      } else if (organo instanceof OrganoUnipersonal unipersonal) {
        System.out.println(organo.nombre + ": Órgano unipersonal a cargo de " + unipersonal.titular);
      }
    }
  }
}
