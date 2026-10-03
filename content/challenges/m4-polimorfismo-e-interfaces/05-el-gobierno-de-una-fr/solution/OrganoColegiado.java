import java.util.ArrayList;
import java.util.List;

public class OrganoColegiado extends OrganoDeGobierno {
  List<String> integrantes = new ArrayList<>();

  OrganoColegiado(String nombre, int mandatoEnAnios) {
    super(nombre, mandatoEnAnios);
  }

  @Override
  String describirComposicion() {
    return "Órgano colegiado de " + integrantes.size() + " integrantes";
  }
}
