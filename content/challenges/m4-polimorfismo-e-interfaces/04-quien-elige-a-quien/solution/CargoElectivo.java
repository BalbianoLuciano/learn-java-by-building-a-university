/** Un cargo que se cubre por elección de un órgano de gobierno. */
public interface CargoElectivo {
  String nombre();

  String organoQueElige();

  int mandatoEnAnios();
}
