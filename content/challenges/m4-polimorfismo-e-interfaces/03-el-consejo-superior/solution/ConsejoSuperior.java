public class ConsejoSuperior extends OrganoColegiado {
  int decanos;
  int docentes;
  int graduados;
  int estudiantes;
  int noDocentes;

  ConsejoSuperior(int decanos, int docentes, int graduados, int estudiantes, int noDocentes) {
    super("Consejo Superior", 2);
    if (docentes != 15) {
      throw new IllegalArgumentException("El Consejo Superior tiene 15 docentes, no " + docentes);
    }
    if (graduados != 5) {
      throw new IllegalArgumentException("El Consejo Superior tiene 5 graduados, no " + graduados);
    }
    if (estudiantes != 5) {
      throw new IllegalArgumentException("El Consejo Superior tiene 5 estudiantes, no " + estudiantes);
    }
    if (noDocentes != 5) {
      throw new IllegalArgumentException("El Consejo Superior tiene 5 no docentes, no " + noDocentes);
    }
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
