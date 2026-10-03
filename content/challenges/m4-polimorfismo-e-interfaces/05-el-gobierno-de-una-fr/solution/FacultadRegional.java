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
      System.out.println(organo.nombre + ": " + organo.describirComposicion());
    }
  }
}
