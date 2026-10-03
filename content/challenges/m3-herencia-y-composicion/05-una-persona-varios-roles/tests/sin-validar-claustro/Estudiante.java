public class Estudiante extends Rol {
  String carrera;

  Estudiante(String carrera) {
    super("estudiantes");
    this.carrera = carrera;
  }
}
