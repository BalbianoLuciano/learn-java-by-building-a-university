public class Persona {
  private String nombre;
  private int antiguedadDocente;

  Persona(String nombre, int antiguedadDocente) {
    this.nombre = nombre;
    this.antiguedadDocente = antiguedadDocente;
  }

  public String getNombre() {
    return nombre;
  }

  public int getAntiguedadDocente() {
    return antiguedadDocente;
  }
}
