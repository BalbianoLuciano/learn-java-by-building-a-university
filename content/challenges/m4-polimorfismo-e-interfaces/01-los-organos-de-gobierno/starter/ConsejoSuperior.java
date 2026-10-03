public class ConsejoSuperior extends OrganoDeGobierno {
  ConsejoSuperior() {
    super("Consejo Superior", 4);
  }

  @Override
  String describirComposicion() {
    return "Rector, decanos de todas las FR, 15 docentes, 5 graduados, 5 estudiantes y 5 no docentes";
  }
}
