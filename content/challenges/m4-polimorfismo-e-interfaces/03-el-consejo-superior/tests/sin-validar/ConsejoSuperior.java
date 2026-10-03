public class ConsejoSuperior extends OrganoColegiado {
  int decanos;
  int docentes;
  int graduados;
  int estudiantes;
  int noDocentes;

  ConsejoSuperior(int decanos, int docentes, int graduados, int estudiantes, int noDocentes) {
    super("Consejo Superior", 2);
    this.decanos = decanos;
    this.docentes = docentes;
    this.graduados = graduados;
    this.estudiantes = estudiantes;
    this.noDocentes = noDocentes;
  }

  @Override
  String describirComposicion() {
    return "Rector, "
        + decanos
        + " decanos, "
        + docentes
        + " docentes, "
        + graduados
        + " graduados, "
        + estudiantes
        + " estudiantes y "
        + noDocentes
        + " no docentes";
  }
}
