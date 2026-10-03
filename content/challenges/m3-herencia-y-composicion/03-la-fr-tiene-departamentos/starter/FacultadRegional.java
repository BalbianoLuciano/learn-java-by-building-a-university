public class FacultadRegional extends UnidadAcademica {
  // Reemplazá estos tres atributos por una List<Departamento>.
  Departamento departamento1;
  Departamento departamento2;
  Departamento departamento3;

  FacultadRegional(String nombre, String ciudad, String provincia) {
    super(nombre, ciudad, provincia);
  }

  void agregarDepartamento(Departamento departamento) {
    if (departamento1 == null) {
      departamento1 = departamento;
    } else if (departamento2 == null) {
      departamento2 = departamento;
    } else if (departamento3 == null) {
      departamento3 = departamento;
    }
    // Con un cuarto departamento, no hay dónde guardarlo.
  }

  int cantidadDeDepartamentos() {
    int cantidad = 0;
    if (departamento1 != null) {
      cantidad++;
    }
    if (departamento2 != null) {
      cantidad++;
    }
    if (departamento3 != null) {
      cantidad++;
    }
    return cantidad;
  }
}
