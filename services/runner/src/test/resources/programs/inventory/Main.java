public class Main {
  public static void main(String[] args) {
    Inventario inventario = new Inventario();
    inventario.agregar(new Producto("A-1", "Cuaderno", 1500.0, 20));
    inventario.agregar(new Producto("B-2", "Lapicera", 300.5, 4));
    inventario.agregar(new Producto("C-3", "Mochila", 25000.0, 2));

    try {
      inventario.agregar(new Producto("D-4", "Regla", -1.0, 1));
    } catch (IllegalArgumentException e) {
      System.out.println("Rechazado: " + e.getMessage());
    }

    Producto lapicera = inventario.buscar("B-2");
    boolean retirado = lapicera.retirar(3);
    System.out.println(lapicera + " retirado=" + retirado);

    System.out.print("Valor total: ");
    System.out.println(inventario.valorTotal());
    System.out.println("Con stock bajo: " + inventario.cantidadConStockBajo(5));
  }
}
