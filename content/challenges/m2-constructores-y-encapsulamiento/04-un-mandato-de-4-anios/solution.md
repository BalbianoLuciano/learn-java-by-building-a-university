# Un mandato de 4 años

## De atributo a constante

```java
static final int MANDATO_EN_ANIOS = 4;
```

`mandatoEnAnios` como atributo era un error de modelo: cada cargo tendría su propia copia del
mismo número, y nada impediría que uno dijera 5. La duración del mandato es una regla del
Estatuto (art. 67), igual para todos: pertenece a la **clase**.

- `static`: hay una sola `MANDATO_EN_ANIOS`, compartida; no vive en ningún objeto.
- `final`: se asigna una vez y no cambia. Reasignarla no compila.
- El nombre en mayúsculas es la convención de Java para constantes.

Se puede leer sin ningún objeto: `CargoDeRector.MANDATO_EN_ANIOS`.

## Un dato calculado

```java
int anioDeFin() {
  return desde + MANDATO_EN_ANIOS;
}
```

El año de fin no se guarda como atributo: se deduce de `desde` (dato del objeto) y de la
constante (regla de la clase). Así no puede quedar desactualizado ni contradecir al inicio.

## Para llevarte

- `static`: de la clase, no de cada objeto.
- `final`: no cambia después de asignarse.
- Lo que se deduce de otros datos se calcula en un método, no se duplica en un atributo.
