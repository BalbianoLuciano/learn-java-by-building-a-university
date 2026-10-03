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
    for (Rol rol : roles) {
      if (rol.claustro.equals(claustro)) {
        claustroElectoral = claustro;
        return;
      }
    }
    throw new IllegalArgumentException(nombre + " no tiene ningún rol del claustro " + claustro);
  }
}
