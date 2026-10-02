# La facultad donde estudiás

## Dos variables, un objeto

```java
FacultadRegional miFacultad = resistencia;
```

Esta línea no tiene `new`: no crea ninguna facultad. Copia la **referencia** que guarda
`resistencia`. Desde ahora las dos variables apuntan al mismo objeto. A eso se lo llama
*aliasing*: un objeto con más de un nombre.

## El cambio se ve desde las dos

```java
miFacultad.provincia = "Chaco";
```

La provincia se carga a través de `miFacultad`, pero el objeto es el mismo de siempre. Si
después leés `resistencia.provincia`, también vale `"Chaco"`.

## Qué habría pasado con `new`

```java
FacultadRegional miFacultad = new FacultadRegional();
miFacultad.provincia = "Chaco";
```

Acá sí hay dos facultades. La segunda queda con provincia `"Chaco"` y sin nombre ni ciudad,
y la FR Resistencia sigue sin provincia.

## Para llevarte

- Una variable de tipo objeto guarda una referencia, no el objeto.
- `=` entre dos variables de tipo objeto copia la referencia.
- Un objeto puede tener varias variables que apunten a él; los cambios se ven desde todas.
