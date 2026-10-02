import java.util.ArrayList;
import java.util.List;

public class FacultadRegional extends UnidadAcademica {
  Decano decano;
  List<Departamento> departamentos = new ArrayList<>();

  void asignar(Decano nuevo) {
    if (nuevo == null) {
      throw new IllegalArgumentException("La facultad no puede quedar sin decano");
    }
    decano = nuevo;
  }
}
