# Tres regionales, tres estados

## Tres objetos de la misma clase

```java
FacultadRegional cordoba = new FacultadRegional();
cordoba.nombre = "Córdoba";
cordoba.ciudad = "Córdoba";
cordoba.provincia = "Córdoba";

FacultadRegional mendoza = new FacultadRegional();
mendoza.nombre = "Mendoza";
mendoza.ciudad = "Mendoza";
mendoza.provincia = "Mendoza";
```

Cada `new FacultadRegional()` crea un objeto distinto. Los tres salen del mismo molde, así
que los tres tienen `nombre`, `ciudad`, `provincia` y `consultas`, pero cada uno guarda sus
propios valores.

## Un cambio, un objeto

```java
cordoba.consultas = 1;
```

`consultas` es un `int`, y un atributo `int` empieza valiendo 0. Esta línea cambia el
atributo del objeto al que apunta `cordoba`, y de ningún otro: `resistencia.consultas` y
`mendoza.consultas` siguen en 0.

## Para llevarte

- El **estado** de un objeto es el valor de sus atributos en un momento dado.
- Dos objetos de la misma clase no comparten estado.
- Lo que va antes del punto decide qué objeto cambia.
