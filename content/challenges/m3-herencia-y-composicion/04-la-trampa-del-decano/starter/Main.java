public class Main {
  public static void main(String[] args) {
    FacultadRegional resistencia = new FacultadRegional("Resistencia");

    Profesor ana = new Profesor("Ana Lema", 12);
    Profesor luis = new Profesor("Luis Ferro", 8);

    // Con herencia, el decano es OTRO objeto, que repite los datos de Ana.
    resistencia.decano = new CargoDeDecano("Ana Lema", 12, 2018);

    // En 2022 asume Luis. ¿Y Ana? Sigue siendo profesora: imprimilo.
  }
}
