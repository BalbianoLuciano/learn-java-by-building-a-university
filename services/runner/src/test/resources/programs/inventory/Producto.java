public class Producto {
  private final String codigo;
  private final String nombre;
  private final double precio;
  private int stock;

  public Producto(String codigo, String nombre, double precio, int stock) {
    if (precio < 0) {
      throw new IllegalArgumentException("El precio no puede ser negativo: " + precio);
    }
    this.codigo = codigo;
    this.nombre = nombre;
    this.precio = precio;
    this.stock = stock;
  }

  public String getCodigo() {
    return codigo;
  }

  public int getStock() {
    return stock;
  }

  public boolean retirar(int cantidad) {
    if (cantidad <= 0 || cantidad > stock) {
      return false;
    }
    stock -= cantidad;
    return true;
  }

  public double valorEnStock() {
    return precio * stock;
  }

  @Override
  public String toString() {
    return codigo + " " + nombre + " x" + stock;
  }
}
