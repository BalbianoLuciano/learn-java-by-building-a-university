public class FacultadRegional extends UnidadAcademica {
  Departamento[] departamentos = new Departamento[3];
  int cantidad;

  FacultadRegional(String nombre, String ciudad, String provincia) {
    super(nombre, ciudad, provincia);
  }

  void agregarDepartamento(Departamento departamento) {
    departamentos[cantidad] = departamento;
    cantidad++;
  }

  int cantidadDeDepartamentos() {
    return cantidad;
  }
}
