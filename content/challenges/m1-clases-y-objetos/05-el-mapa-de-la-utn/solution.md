# Integrador: el mapa de la UTN

## Un método de instancia

```java
String describir() {
  return "FR " + nombre + " (" + ciudad + ", " + provincia + ")";
}
```

El método va dentro de la clase y no es `static`: se llama **sobre un objeto**
(`rosario.describir()`) y, mientras corre, `nombre`, `ciudad` y `provincia` son los
atributos de ese objeto. Por eso el mismo método devuelve un texto distinto para cada
facultad: la lógica está escrita una vez, el estado lo pone cada objeto.

## Un arreglo de referencias

```java
FacultadRegional[] facultades = {resistencia, cordoba, mendoza, rosario, santaFe};
```

El arreglo no copia las facultades: guarda cinco referencias a los objetos que ya existían.
Siguen siendo cinco objetos (y un solo Rectorado al que todos apuntan).

## El recorrido

```java
for (FacultadRegional facultad : facultades) {
  System.out.println(facultad.describir());
}
```

En cada vuelta, `facultad` apunta a la siguiente posición del arreglo y `describir()` se
ejecuta sobre ese objeto. Cinco vueltas, cinco líneas, sin repetir código. Con las 30
Facultades Regionales de la UTN sería el mismo `for`.

## Para llevarte

- Un método de instancia combina la lógica (escrita una vez) con el estado de cada objeto.
- Un arreglo de objetos guarda referencias, no copias.
- Recorrer el arreglo y llamar al mismo método es la base de lo que viene: pedirle lo mismo
  a objetos distintos.
