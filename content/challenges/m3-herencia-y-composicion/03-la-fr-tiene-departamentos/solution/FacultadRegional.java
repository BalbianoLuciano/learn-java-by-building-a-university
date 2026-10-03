import java.util.ArrayList;
import java.util.List;

public class FacultadRegional extends UnidadAcademica {
  List<Departamento> departamentos = new ArrayList<>();

  FacultadRegional(String nombre, String ciudad, String provincia) {
    super(nombre, ciudad, provincia);
  }

  void agregarDepartamento(Departamento departamento) {
    departamentos.add(departamento);
  }

  int cantidadDeDepartamentos() {
    return departamentos.size();
  }
}
