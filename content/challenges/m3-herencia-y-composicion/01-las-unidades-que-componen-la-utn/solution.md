# Las unidades que componen la UTN

## "Es un"

```java
public class FacultadRegional extends UnidadAcademica {}

public class RegionalAcademica extends UnidadAcademica {
  FacultadRegional facultadDeQueDepende;
}
```

`extends` declara que toda `FacultadRegional` **es** una `UnidadAcademica`. La subclase
recibe `nombre`, `ciudad`, `provincia` y `describir()` sin escribirlos: están en la
superclase y Java los encuentra ahí. `FacultadRegional` queda vacía por ahora; en los
próximos desafíos le vamos a agregar lo que solo una FR tiene (decano, departamentos).

`RegionalAcademica` hereda lo mismo y conserva lo propio: `facultadDeQueDepende`, porque
una RA depende de una FR y una FR no depende de nadie (Estatuto, arts. 5 y 112).

## Dónde se ejecuta `describir()`

Cuando `main` llama `resistencia.describir()`, Java busca el método en `FacultadRegional`.
Como no está, sube a `UnidadAcademica` y ejecuta ese. En la maqueta se ve como un edificio
de dos pisos: la planta baja es la superclase; arriba, lo que agrega la subclase.

## Qué NO es herencia

La RA tiene una referencia a una FR: eso es composición ("tiene una"), no herencia. Una RA
no es una FR; depende de una. Más adelante vas a ver por qué confundir las dos cosas
rompe el modelo.

## Para llevarte

- `extends` = "es un". La subclase recibe atributos y métodos de la superclase.
- Lo que se hereda no se repite; lo propio se queda en la subclase.
- Un método heredado se ejecuta sobre el objeto de la subclase, con sus atributos.
