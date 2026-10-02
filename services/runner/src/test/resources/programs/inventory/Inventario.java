import java.util.ArrayList;
import java.util.List;

public class Inventario {
  private final List<Producto> productos = new ArrayList<>();

  public void agregar(Producto producto) {
    productos.add(producto);
  }

  public Producto buscar(String codigo) {
    for (Producto producto : productos) {
      if (producto.getCodigo().equals(codigo)) {
        return producto;
      }
    }
    return null;
  }

  public double valorTotal() {
    double total = 0;
    for (Producto producto : productos) {
      total += producto.valorEnStock();
    }
    return total;
  }

  public int cantidadConStockBajo(int minimo) {
    int cantidad = 0;
    for (int i = 0; i < productos.size(); i++) {
      if (productos.get(i).getStock() < minimo) {
        cantidad++;
      }
    }
    return cantidad;
  }
}
