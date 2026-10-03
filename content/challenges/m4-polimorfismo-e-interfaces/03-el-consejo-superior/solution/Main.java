public class Main {
  public static void main(String[] args) {
    OrganoDeGobierno consejo = new ConsejoSuperior(30, 15, 5, 5, 5);
    System.out.println(consejo.nombre + ": " + consejo.describirComposicion());

    try {
      new ConsejoSuperior(30, 14, 5, 5, 5);
      System.out.println("Se formó un consejo con 14 docentes");
    } catch (IllegalArgumentException e) {
      System.out.println("Rechazado: " + e.getMessage());
    }
  }
}
