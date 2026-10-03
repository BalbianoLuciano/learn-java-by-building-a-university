public class ConsejoSuperior extends OrganoColegiado {
  int decanos;
  int docentes;
  int graduados;
  int estudiantes;
  int noDocentes;

  ConsejoSuperior(int decanos, int docentes, int graduados, int estudiantes, int noDocentes) {
    super("Consejo Superior", 2);
    // Rechazá las cantidades que no sean las del art. 105 (15, 5, 5, 5).
    this.decanos = decanos;
    this.docentes = docentes;
    this.graduados = graduados;
    this.estudiantes = estudiantes;
    this.noDocentes = noDocentes;
  }

  // Sobrescribí describirComposicion() con la composición completa.
}
