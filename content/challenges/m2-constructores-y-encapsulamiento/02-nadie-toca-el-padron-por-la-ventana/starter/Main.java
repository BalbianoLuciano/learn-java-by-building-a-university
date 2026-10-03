public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("Resistencia", "Resistencia", "Chaco");

    // Esta línea entra "por la ventana": con el atributo private ya no va a compilar.
    System.out.println(resistencia.nombre);
  }
}
