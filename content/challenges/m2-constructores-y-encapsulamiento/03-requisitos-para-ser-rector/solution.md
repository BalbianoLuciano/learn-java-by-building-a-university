# Requisitos para ser Rector

## Validar antes de guardar

```java
CargoDeRector(Persona candidato) {
  if (!candidato.getNacionalidad().equals("argentina")) {
    throw new IllegalArgumentException("Para ser Rector hay que ser argentino");
  }
  if (candidato.getEdad() < 30) {
    throw new IllegalArgumentException("Para ser Rector hay que tener al menos 30 años");
  }
  if (!candidato.fueProfesorUniversitario()) {
    throw new IllegalArgumentException("Para ser Rector hay que ser o haber sido profesor");
  }
  this.titular = candidato;
}
```

Los tres `if` traducen el art. 66 del Estatuto. Cada uno **rechaza** con `throw`: la
ejecución del constructor se corta ahí, el `new` no termina y no queda ningún
`CargoDeRector` a medias. Solo si las tres condiciones pasan se llega a la última línea y
el candidato queda como titular.

## Qué ve quien llama

```java
try {
  new CargoDeRector(julian);
} catch (IllegalArgumentException e) {
  System.out.println("Rechazado: " + e.getMessage());
}
```

`main` intenta crear el cargo con Julián, de 29 años. La excepción sale del constructor y
la atrapa el `catch`, que muestra el mensaje. Sin `try`, la excepción frenaría el programa:
también es una respuesta válida cuando no tiene sentido seguir.

## Por qué en el constructor

Como `titular` es `private` y no hay setter, el constructor es el **único** camino para
poner una persona en el cargo. Validar ahí garantiza que todo `CargoDeRector` que exista
cumple la regla: no hay forma de fabricar uno inválido.

## Para llevarte

- Un constructor puede negarse a construir lanzando una excepción.
- `IllegalArgumentException` es la excepción para "me pasaste un valor que no acepto".
- Validar en el único punto de entrada hace que la regla valga siempre.
