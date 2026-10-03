public class Persona {
  private String nombre;
  private String nacionalidad;
  private int edad;
  private boolean fueProfesorUniversitario;

  Persona(String nombre, String nacionalidad, int edad, boolean fueProfesorUniversitario) {
    this.nombre = nombre;
    this.nacionalidad = nacionalidad;
    this.edad = edad;
    this.fueProfesorUniversitario = fueProfesorUniversitario;
  }

  public String getNombre() {
    return nombre;
  }

  public String getNacionalidad() {
    return nacionalidad;
  }

  public int getEdad() {
    return edad;
  }

  public boolean fueProfesorUniversitario() {
    return fueProfesorUniversitario;
  }
}
