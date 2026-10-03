# ¿Quién elige a quién?

## Un contrato, no una familia

```java
public interface CargoElectivo {
  String nombre();

  String organoQueElige();

  int mandatoEnAnios();
}
```

Rector, Decano y Director de Departamento no tienen una superclase razonable en común: no
"son" la misma cosa. Pero comparten una **capacidad**: ser elegidos por un órgano, por un
mandato. Una interfaz describe exactamente eso: una lista de métodos, sin atributos ni
cuerpos, que una clase promete tener.

```java
public class CargoDeDecano implements CargoElectivo {
  ...
  @Override
  public String organoQueElige() {
    return "la Asamblea de la Facultad Regional";
  }
}
```

`implements` firma el contrato. Los métodos tienen que ser `public` (así los declara la
interfaz) y `@Override` vale igual que con una superclase.

## Por qué no alcanza con tener los métodos

En el código base, `CargoDeDecano` ya tenía `organoQueElige()` y `mandatoEnAnios()`. Pero
sin `implements`, Java no lo considera un `CargoElectivo`: no se lo puede guardar en una
variable ni en un arreglo de ese tipo. El contrato tiene que ser explícito.

## El recorrido que no sabe con quién habla

```java
CargoElectivo[] cargos = {rector, decano, director};
for (CargoElectivo cargo : cargos) {
  System.out.println(
      cargo.nombre() + ": lo elige " + cargo.organoQueElige() + " por " + cargo.mandatoEnAnios() + " años");
}
```

El `for` solo sabe que cada elemento es un `CargoElectivo`. No le importa la clase: le pide
los tres métodos y cada objeto responde con su versión. Si mañana aparece
`CargoDeVicedecano implements CargoElectivo`, entra en el arreglo sin tocar el `for`.

## Para llevarte

- Interfaz: capacidades compartidas por clases que no son parientes.
- `implements` es el contrato explícito; los métodos son `public`.
- Programar contra la interfaz permite agregar clases sin cambiar el código que las usa.
