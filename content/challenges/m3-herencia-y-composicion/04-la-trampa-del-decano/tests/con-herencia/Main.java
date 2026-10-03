public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("Resistencia");

    Profesor ana = new Profesor("Ana Lema", 12);
    Profesor luis = new Profesor("Luis Ferro", 8);

    resistencia.decano = new CargoDeDecano("Ana Lema", 12, 2018);
    resistencia.decano = new CargoDeDecano("Luis Ferro", 8, 2022);
    System.out.println(ana.nombre + " sigue siendo profesora");
  }
}
