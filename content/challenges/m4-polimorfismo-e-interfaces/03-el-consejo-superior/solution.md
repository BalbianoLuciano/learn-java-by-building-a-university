# El Consejo Superior

## Tres niveles de herencia

`ConsejoSuperior` extiende `OrganoColegiado`, que extiende `OrganoDeGobierno`. Cada nivel
aporta algo: la abstracta, nombre y mandato; el colegiado, la idea de integrantes; el
Consejo Superior, su composición fija del art. 105. En la maqueta son tres pisos.

## Una invariante en el constructor

```java
ConsejoSuperior(int decanos, int docentes, int graduados, int estudiantes, int noDocentes) {
  super("Consejo Superior", 2);
  if (docentes != 15) {
    throw new IllegalArgumentException("El Consejo Superior tiene 15 docentes, no " + docentes);
  }
  ...
}
```

Primero `super(...)`, siempre. Después, las verificaciones: el Estatuto fija 15 docentes,
5 graduados, 5 estudiantes y 5 no docentes, y un consejo con otra composición no existe.
Igual que con el Rector, rechazar en el constructor garantiza que todo `ConsejoSuperior`
del programa es válido. (La cantidad de decanos es una por FR; en este desafío la recibimos
sin verificarla contra la lista de facultades.)

## Sobrescribir otra vez

```java
@Override
String describirComposicion() {
  return "Rector, " + decanos + " decanos, " + docentes + " docentes, " + ...;
}
```

`OrganoColegiado` ya sobrescribía el método abstracto; `ConsejoSuperior` lo sobrescribe de
nuevo. Cuando `main` llama `consejo.describirComposicion()` sobre una variable de tipo
`OrganoDeGobierno`, corre la versión **más específica**: la del Consejo Superior.

## Para llevarte

- La herencia puede tener varios niveles; cada uno puede volver a sobrescribir.
- Las invariantes se defienden en el constructor, también en las subclases.
- `super(...)` primero; las validaciones después.
