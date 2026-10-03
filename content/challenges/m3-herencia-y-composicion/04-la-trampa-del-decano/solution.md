# La trampa del Decano

## Por qué `extends Profesor` está mal

`CargoDeDecano extends Profesor` dice que un cargo **es** un profesor. Pero el decanato es
un cargo por un período (4 años, art. 86) que ocupa una persona que ya era profesora y lo
sigue siendo después (art. 30). Con herencia:

- Nombrar a Ana decana obliga a crear **otro** objeto que repite sus datos: en el programa
  hay dos "Anas", y la del cargo no es la profesora.
- Cuando termina el mandato, no se puede "convertir" el decano en profesor común: Java no
  cambia la clase de un objeto ya creado.
- Cada cargo nuevo cuenta como un profesor más que no existe.

## Composición: el cargo tiene un titular

```java
public class CargoDeDecano {
  Profesor titular;
  int desde;

  CargoDeDecano(Profesor titular, int desde) {
    this.titular = titular;
    this.desde = desde;
  }
}
```

```java
resistencia.decano = new CargoDeDecano(ana, 2018);
resistencia.decano = new CargoDeDecano(luis, 2022);
System.out.println(ana.nombre + " sigue siendo profesora");
```

El cargo guarda una **referencia** a la persona. Ana y Luis son los mismos dos objetos
antes, durante y después de sus mandatos; lo que cambia es qué cargo apunta a quién. En la
maqueta, el pedestal del decano cambia de titular y las figuras siguen donde estaban.

## La pregunta que decide

Antes de escribir `extends`, preguntate: ¿X **es** Y de forma permanente, o X **tiene** /
**ocupa** / **usa** Y? Una FR es una unidad académica (herencia). Un cargo lo ocupa una
persona (composición). Heredar para reutilizar un par de atributos es la trampa más común
de este módulo.

## Para llevarte

- Herencia solo cuando "es un" es permanente y real.
- Un cargo, un rol o un período se modelan con composición: tienen una persona.
- Java no cambia la clase de un objeto: si algo puede dejar de serlo, no es herencia.
