# Integrador: una persona, varios roles

## Herencia entre roles

```java
public class Estudiante extends Rol {
  String carrera;

  Estudiante(String carrera) {
    super("estudiantes");
    this.carrera = carrera;
  }
}
```

Acá la herencia es legítima: un `Estudiante` **es** un `Rol`, siempre. Lo común (el
claustro) está en `Rol`; cada subclase fija el suyo con `super(...)` y agrega lo propio
(`carrera`, `categoria`). `Auxiliar` es del claustro docente aunque la persona sea alumna:
así lo define el art. 36 para el Ayudante de 2ª.

## Composición entre persona y roles

```java
List<Rol> roles = new ArrayList<>();

void agregarRol(Rol rol) {
  roles.add(rol);
}
```

`Persona` no hereda de `Estudiante` ni de `Auxiliar`: los **tiene**. Por eso Juana puede
ser las dos cosas a la vez, sumar un rol de graduada cuando se reciba y dejar de ser
estudiante sin cambiar de clase. Es la misma lección del decano, ahora con una lista.

## La regla vive adentro

```java
void elegirClaustro(String claustro) {
  for (Rol rol : roles) {
    if (rol.claustro.equals(claustro)) {
      claustroElectoral = claustro;
      return;
    }
  }
  throw new IllegalArgumentException(nombre + " no tiene ningún rol del claustro " + claustro);
}
```

El art. 114 dice que nadie vota en dos claustros ni en uno ajeno. El método recorre los
roles (un `for` sobre la lista, como en el mapa de la UTN), guarda la elección si alguno
coincide y, si no, rechaza. Como `claustroElectoral` se escribe solo acá, la regla no se
puede saltear.

## Para llevarte

- Herencia: categorías permanentes (`Estudiante` es un `Rol`).
- Composición: lo que se tiene, se suma o se pierde (`Persona` tiene roles).
- Las invariantes se cuidan en el método que cambia el estado.
