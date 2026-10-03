import java.util.ArrayList;
import java.util.List;

public class Persona {
  String nombre;
  String claustroElectoral;
  List<Rol> roles = new ArrayList<>();

  Persona(String nombre) {
    this.nombre = nombre;
  }

  void agregarRol(Rol rol) {
    roles.add(rol);
  }

  void elegirClaustro(String claustro) {
    claustroElectoral = claustro;
  }
}
