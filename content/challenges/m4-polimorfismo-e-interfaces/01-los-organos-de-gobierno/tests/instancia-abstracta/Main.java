public class Main {
  public static void main(String[] args) {
    // Un "órgano de gobierno" genérico no existe en la UTN: reemplazalo por el Consejo Superior.
    OrganoDeGobierno organo = new OrganoDeGobierno("Órgano de gobierno", 4);
    System.out.println(organo.nombre + ": " + organo.describirComposicion());
  }
}
