# Un solo Rectorado

## Un objeto, tres referencias

```java
Rectorado rectorado = new Rectorado();
rectorado.direccion = "Sarmiento 440";

resistencia.rectorado = rectorado;
cordoba.rectorado = rectorado;
mendoza.rectorado = rectorado;
```

Hay un solo `new Rectorado()`, así que hay un solo edificio. Las tres asignaciones no lo
copian: guardan en cada facultad una **referencia** al mismo objeto. En la maqueta, el
globo de cada facultad dice `rectorado → #1`: el mismo número en las tres, igual que en la
UTN real, donde las 30 facultades dependen de una sola sede central (Estatuto, art. 5).

Si en cambio escribieras `new Rectorado()` tres veces, tendrías tres edificios con la misma
dirección pero independientes: cambiar uno no cambiaría los otros.

## `==` compara referencias

```java
if (resistencia.rectorado == cordoba.rectorado) {
  System.out.println("Mismo Rectorado");
}
```

Entre objetos, `==` pregunta "¿apuntan al mismo objeto?", no "¿tienen los mismos datos?".
Dos rectorados distintos con `direccion = "Sarmiento 440"` darían `false`. Para comparar
contenido se usa `equals`, que vas a ver más adelante.

## Para llevarte

- Un atributo puede ser de tipo objeto: guarda una referencia, no una copia.
- Varias referencias al mismo objeto son la forma normal de representar algo compartido.
- `==` entre referencias es `true` solo si son el mismo objeto.
