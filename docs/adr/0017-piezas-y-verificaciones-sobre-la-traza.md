# ADR 0017 · Piezas declaradas en el desafío y verificaciones sobre la traza

- **Estado:** aceptada
- **Fecha:** 2026-10-02

## Contexto

Al implementar la api hubo que definir tres cosas que las specs dejaban abiertas: cómo sabe una verificación de qué pieza habla (`piece: fr-resistencia`), qué se le manda a la web para dibujar y reproducir la ejecución, y cómo comprobar que un cambio se hizo "a través de" una variable, cosa que la traza no registra.

## Decisión

- **Las piezas que pide el desafío se declaran** en `scene.pieces` (id, tipo, condición opcional y un rótulo para cuando faltan). La api las empareja con los objetos de la traza. Una pieza pedida que el código no creó se informa igual, como no construida. Los objetos que nadie pidió también son piezas, con id generado; las variables de `main` que apuntan a un objeto, también.
- **La línea de tiempo es la traza con piezas**: cada paso del runner, con la pieza que toca y la pieza a la que pasa a apuntar. La web no recibe la traza ni tiene que interpretar ids de objetos.
- **`writtenThrough` lee el código fuente**: para saber por qué variable se llegó a un objeto se busca `variable.` en la línea que hizo la escritura, o en la línea de `main` que llamó al método que la hizo. Es la única verificación que mira texto en lugar de la traza.
- **Los mensajes pueden usar marcadores** (`{missing}`, `{count}`) que llena cada tipo de verificación; la api rechaza al arrancar un marcador que el tipo no llena.

## Consecuencias

- Quien escribe un desafío declara qué espera ver construido; no tiene que conocer ids de objetos.
- La maqueta puede mostrar lo que falta (pieza no construida) además de lo que hay.
- `writtenThrough` es una heurística sobre el texto: una línea como `a.x = 1; b.x = 2;` puede engañarla. Se usa solo donde el concepto es justamente el alias (desafío 1.3).
- Si la línea de tiempo necesita datos nuevos (por ejemplo, el contenido de una lista paso a paso), hay que ampliar primero la traza del runner.
