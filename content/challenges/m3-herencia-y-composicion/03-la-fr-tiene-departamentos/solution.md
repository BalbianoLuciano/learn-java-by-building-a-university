# La FR tiene departamentos

## Una lista en lugar de atributos numerados

```java
import java.util.ArrayList;
import java.util.List;

public class FacultadRegional extends UnidadAcademica {
  List<Departamento> departamentos = new ArrayList<>();
  ...
}
```

`departamento1`, `departamento2`, `departamento3` funcionan hasta que aparece el cuarto. La
cantidad de departamentos de una FR no está fijada por el Estatuto: depende de las carreras
que dicta (art. 92). Cuando un objeto **tiene varios** de algo y no se sabe cuántos, el
atributo es una colección.

`List<Departamento>` dice "una lista de departamentos" y `new ArrayList<>()` crea una
vacía. La lista nace con el objeto, así que nunca es `null`.

## Agregar y contar

```java
void agregarDepartamento(Departamento departamento) {
  departamentos.add(departamento);
}

int cantidadDeDepartamentos() {
  return departamentos.size();
}
```

Dos métodos de una línea reemplazan las cadenas de `if`. Y siguen funcionando con 4, 10 o
los que haga falta.

## Sigue siendo composición

La facultad **tiene** departamentos: la lista guarda referencias a objetos `Departamento`
que existen aparte. Un `Departamento` no es una `FacultadRegional`, así que acá no hay
herencia. En la maqueta, los pabellones se apoyan junto a la isla de la FR.

## Para llevarte

- "Tiene varios" se modela con una `List`, no con atributos numerados.
- Se inicializa en la declaración (`= new ArrayList<>()`) para que nunca sea `null`.
- `add` y `size` hacen el trabajo; el objeto decide cómo exponerlos.
