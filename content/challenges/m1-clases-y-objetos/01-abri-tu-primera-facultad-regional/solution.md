# Abrí tu primera Facultad Regional

## La clase: el molde

```java
public class FacultadRegional {
  String nombre;
  String ciudad;
  String provincia;
}
```

`FacultadRegional` no es ninguna facultad en particular. Dice qué datos va a tener **cada**
facultad que se cree: un nombre, una ciudad y una provincia, los tres de tipo `String`.

## El objeto: una facultad concreta

```java
FacultadRegional resistencia = new FacultadRegional();
```

`new FacultadRegional()` construye un objeto con ese molde. Recién ahora existe una
facultad. La variable `resistencia` guarda una **referencia** a ese objeto: es la forma de
llegar a él.

Al crearse, sus tres atributos valen `null`, porque todavía nadie les dio un valor.

## Los datos

```java
resistencia.nombre = "Resistencia";
resistencia.ciudad = "Resistencia";
resistencia.provincia = "Chaco";
```

El punto significa "del objeto al que apunta `resistencia`, el atributo…". Cada línea le
da valor a un atributo de **ese** objeto.

## Para llevarte

- Una clase declara atributos; un objeto los tiene con valores.
- `new` crea el objeto. Sin `new` hay una variable, pero no hay facultad.
- Un atributo `String` sin valor vale `null`.
