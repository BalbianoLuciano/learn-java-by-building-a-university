# Integrador: el Decano de Resistencia

## Dos constructores, una regla

```java
CargoDeDecano(Persona titular, int desde) {
  if (titular.getAntiguedadDocente() < ANTIGUEDAD_MINIMA) {
    throw new IllegalArgumentException("Para ser Decano hacen falta 3 años de antigüedad docente");
  }
  this.titular = titular;
  this.desde = desde;
}

CargoDeDecano(Persona titular) {
  this(titular, ANIO_ACTUAL);
}
```

Una clase puede tener varios constructores con distintos parámetros (**sobrecarga**). Uno
es el *canónico*: recibe todo y contiene la validación. El otro es *de conveniencia*: su
única tarea es llamar al canónico con un valor por defecto, y lo hace con `this(...)`, que
tiene que ser la primera instrucción. La regla del art. 86 queda escrita una sola vez.

## Un setter que valida

```java
public boolean asignarDecano(Persona persona, int desde) {
  if (persona.getAntiguedadDocente() < CargoDeDecano.ANTIGUEDAD_MINIMA) {
    return false;
  }
  decano = new CargoDeDecano(persona, desde);
  return true;
}
```

Como `decano` es privado, este método es la única forma de cambiarlo. Pregunta primero y
cambia después: si Tomás no cumple, devuelve `false` **sin tocar nada**, y Lucía sigue
siendo decana. Devolver `boolean` le permite a `main` reaccionar sin usar excepciones.

Fijate que el constructor también valida. No es redundante: el setter evita crear un cargo
inválido en el caso común, y el constructor garantiza que, se lo llame desde donde se lo
llame, nunca exista un `CargoDeDecano` que rompa la regla.

## Para llevarte

- Sobrecargar constructores: uno canónico con la regla; los demás delegan con `this(...)`.
- Un setter que valida comprueba, cambia y avisa (`boolean`).
- Composición: la facultad **tiene** un cargo, y el cargo **tiene** una persona.
