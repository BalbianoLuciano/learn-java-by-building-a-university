public class Persona {
  String nombre;
  String claustroElectoral;
  // Reemplazá estos dos atributos por una List<Rol> roles,
  // y los dos agregarRol por uno solo que reciba un Rol.
  Estudiante comoEstudiante;
  Auxiliar comoAuxiliar;

  Persona(String nombre) {
    this.nombre = nombre;
  }

  void agregarRol(Estudiante estudiante) {
    comoEstudiante = estudiante;
  }

  void agregarRol(Auxiliar auxiliar) {
    comoAuxiliar = auxiliar;
  }

  void elegirClaustro(String claustro) {
    // Si algún rol es de ese claustro, guardalo en claustroElectoral.
    // Si no, lanzá IllegalArgumentException.
  }
}
