public class Main {
  public static void main(String[] args) {
    FacultadRegional buenosAires =
        new FacultadRegional("FR Buenos Aires", "Buenos Aires", "Ciudad Autónoma de Buenos Aires");

    buenosAires.agregarDepartamento(new Departamento("Ciencias Básicas"));
    buenosAires.agregarDepartamento(new Departamento("Sistemas"));
    buenosAires.agregarDepartamento(new Departamento("Química"));

    System.out.println(buenosAires.nombre + " tiene " + buenosAires.cantidadDeDepartamentos() + " departamentos");
  }
}
