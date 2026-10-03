// ¿Un cargo ES un profesor? Refactorizá: el cargo TIENE un titular.
public class CargoDeDecano extends Profesor {
  int desde;

  CargoDeDecano(String nombre, int antiguedadDocente, int desde) {
    super(nombre, antiguedadDocente);
    this.desde = desde;
  }
}
