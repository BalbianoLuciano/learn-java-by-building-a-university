public class Main {
  public static void main(String[] args) {
    OrganoDeGobierno organo = new ConsejoSuperior();
    System.out.println(organo.nombre + ": " + organo.describirComposicion());
  }
}
