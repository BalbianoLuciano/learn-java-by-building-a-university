# Feedback

> Spec de cómo la plataforma le explica al alumno qué pasó con su código. Es el corazón
> pedagógico del proyecto: una pieza 3D sin explicación no enseña nada.

## 1. Resultado de una ejecución

| Resultado | Cuándo | Encabezado |
|---|---|---|
| ✅ `passed` | Compila, corre sin excepciones inesperadas y pasan **todas** las verificaciones | "¡Obra terminada!" |
| 🚧 `incomplete` | Compila y corre, pero alguna verificación no pasa | "Obra en construcción (n/N)" |
| ❌ `failed` | No compila, lanza una excepción inesperada, se rechaza o supera un límite | "La obra se frenó" |

## 2. Tipos de problema

| Tipo | Origen | Qué se construye | Estado |
|---|---|---|---|
| **No compila** | Diagnósticos de `javac` | Nada: la maqueta muestra solo la silueta real | ❌ |
| **Rechazado** | Uso fuera de la lista permitida ([`SECURITY.md`](SECURITY.md)) | Nada | ❌ |
| **Excepción** | Excepción no esperada por el desafío | Todo hasta el paso donde se lanzó; ahí la pieza queda ❌ | ❌ |
| **Límite** | Tiempo, pasos, objetos, salida o profundidad de llamadas | Lo construido hasta el corte | ❌ |
| **No cumple el pedido** | Verificación que no pasa | Todo, con las partes faltantes en andamio | 🚧 |

Si hay varios problemas, la bitácora los ordena así: compilación → rechazo → excepción →
límite → verificaciones (en el orden del `challenge.yaml`) → aciertos.

## 3. Estructura de cada mensaje

Todo mensaje de la bitácora tiene **cuatro partes**:

| Parte | Regla | Ejemplo |
|---|---|---|
| **Qué pasó** | Una oración, en términos de la UTN y de la maqueta | "La FR Resistencia está sin decano." |
| **Por qué** | El concepto de Java involucrado, en 1–2 oraciones | "Una facultad **tiene** un decano: es composición, no herencia." |
| **Dónde** | Archivo y línea (chip clicable); la pieza queda vinculada | `Main.java:5` |
| **Pista** | Una pregunta o un paso, **nunca** la línea resuelta | "¿Qué atributo de `FacultadRegional` está quedando en `null`?" |

Los aciertos (✅) llevan solo "qué pasó" y "por qué": reforzar el concepto también enseña.

### Reglas de redacción

- Voseo, segunda persona, tono amable y concreto. Nada de "Error fatal" ni "inválido".
- Una idea por mensaje. Si hay dos problemas, son dos entradas.
- Nunca culpar: "Te falta…", "Todavía no…", no "Hiciste mal…".
- Nombrar lo que el alumno escribió (sus nombres de variables y clases), no genéricos.
- **No mentir sobre Java.** Ejemplo a evitar: "no le heredaste un rector". Un rector no se
  hereda; una universidad *tiene* un rector. Cada mensaje del contenido se revisa contra
  esta regla.
- Máximo 280 caracteres entre "qué pasó" y "por qué".

## 4. Errores de compilación

`javac` informa errores en inglés y en jerga. La api los traduce con un **catálogo** por
código de diagnóstico (`content/feedback/javac.es.yaml`). Si un código no está en el
catálogo, se muestra un mensaje genérico con el texto original de `javac` debajo.

Cada entrada del catálogo tiene qué pasó, por qué y una pista. Los marcadores
(`{símbolo}`) se llenan con una expresión regular sobre el mensaje original de `javac`
(`match`); si el mensaje no coincide se usa un texto sin marcadores (`whatWithoutMatch`).
Los tests de la api compilan un ejemplo de cada error y comprueban el mensaje que sale.

El catálogo cubre, como mínimo, estos errores (los textos viven en el archivo, no acá):

| Qué le pasó al alumno | Códigos de `javac` |
|---|---|
| Usó un nombre que no existe | `cant.resolve`, `cant.resolve.location` y sus variantes `.args` |
| Le falta un símbolo o un valor | `expected`, `expected2`, `expected3`, `illegal.start.of.expr`, `not.stmt`, `premature.eof`, `unclosed.str.lit` |
| El tipo no coincide | `prob.found.req` |
| Llamó con argumentos que no van | `cant.apply.symbol`, `cant.apply.symbols` |
| Tocó algo privado | `report.access` |
| No implementó un método abstracto, o instanció una clase abstracta | `does.not.override.abstract`, `abstract.cant.be.instantiated` |
| Reasignó un `final` | `cant.assign.val.to.var` |
| Le falta un `return` | `missing.ret.stmt` |
| Usó el objeto antes de `super(...)` | `cant.ref.before.ctor.called` |
| Usó algo de instancia desde `static` | `non-static.cant.be.ref` |
| Usó una variable local sin valor | `var.might.not.have.been.initialized` |
| La clase pública no coincide con el archivo, o declaró dos veces un nombre | `class.public.should.be.in.file`, `already.defined` |
| Rompió una regla de la plataforma | `ljbu.err.package.not.allowed`, `ljbu.err.main.not.found`, `ljbu.err.compilation.timed.out` |

Todos los códigos de `javac` llevan el prefijo `compiler.err.`. Desde Java 25 puede haber
código antes de `super(...)` siempre que no use el objeto, así que el mensaje correcto ya
no es "`super(...)` va primero" sino "usaste el objeto antes de `super(...)`".

## 5. Excepciones

Catálogo en `content/feedback/exceptions.es.yaml`.

| Excepción | Qué pasó (plantilla) | Por qué | Visual |
|---|---|---|---|
| `NullPointerException` | "Usaste `{expresión}`, pero apunta a `null`." | Una variable sin objeto es un cable que no llega a ninguna pieza. | Cable suelto + pieza ❌ |
| `ArithmeticException` | "Una división por cero frenó la obra." | Los enteros no se pueden dividir por cero. | Pieza ❌ |
| `ArrayIndexOutOfBoundsException` / `IndexOutOfBoundsException` | "Pediste la posición `{índice}` y solo hay `{tamaño}`." | Las posiciones van de 0 a tamaño − 1. | Pieza ❌ |
| `ClassCastException` | "Un `{real}` no es un `{pedido}`." | Un objeto es de su clase y de sus superclases, no de sus hermanas. | Pisos resaltados |
| `IllegalArgumentException` (lanzada por el alumno) | Se muestra su propio mensaje | Si el desafío la espera, es un acierto ✅. | — |
| `StackOverflowError` | "Un método se llamó a sí mismo sin parar." | Cada llamada ocupa lugar en la pila hasta que se llena. | Pieza ❌ |
| `OutOfMemoryError` | "Se pidió más memoria de la que el programa tiene." | La memoria del programa tiene un límite, y los objetos y arreglos la ocupan. | Pieza ❌ |

La línea que se muestra es la **primera línea del código del alumno** en la pila de la
excepción. Los marcadores se llenan igual que en el catálogo de `javac`, a partir del
mensaje de la excepción; `{type}` y `{message}` están siempre disponibles.

Otros dos catálogos explican lo que no es ni un error de compilación ni una excepción:
`content/feedback/limits.es.yaml`, un mensaje por cada límite del sandbox, y
`content/feedback/rejections.es.yaml`, que explica por familias (archivos, red, hilos,
procesos, teclado, reflexión) el código que la lista permitida rechaza.

Una recursión que no termina casi nunca llega a `StackOverflowError`: el runner la corta
antes, al pasar las 1 000 llamadas anidadas (límite `call_depth`). Ese corte se explica con
el mismo mensaje que `StackOverflowError`. La memoria agotada sí llega como
`OutOfMemoryError`.

## 6. Verificaciones

Cada verificación del `challenge.yaml` trae sus propios textos para cada resultado (formato
completo en [`specs/challenge-format.md`](specs/challenge-format.md)):

```yaml
- id: dean-assigned
  type: object_field
  params:
    where: { type: FacultadRegional, field: nombre, equals: "Resistencia" }
    expect: { field: decano, notNull: true }
  piece: fr-resistencia
  slot: dean
  highlight: object_creation   # qué línea resaltar: object_creation | declaration | last_write
  feedback:
    pass:
      what: { es: "La FR Resistencia tiene su decano." }
      why: { es: "Guardaste un objeto `Decano` en un atributo: eso es composición." }
    fail:
      what: { es: "La FR Resistencia está sin decano." }
      why: { es: "Una facultad **tiene** un decano: es composición, no herencia." }
      hint: { es: "¿Qué atributo de `FacultadRegional` está quedando en `null`?" }
```

Una verificación puede declarar **trampas**: errores comunes con un mensaje más preciso que
el genérico. Si la verificación falla y se cumple la condición de una trampa, se muestra
el mensaje de la trampa.

## 7. Pistas y solución

| Nivel | Se habilita | Contenido |
|---|---|---|
| Pista 1 | Siempre | Conceptual: qué concepto de Java resuelve el pedido |
| Pista 2 | Tras 1 ejecución | Concreta: qué parte del código tocar |
| Pista 3 | Tras 2 ejecuciones | Casi resuelta: la estructura sin los valores exactos |
| Solución | Tras 3 ejecuciones o 3 pistas, con confirmación | Código completo + explicación paso a paso (`solution.md`) |

Ver la solución no impide completar el desafío, pero queda registrado localmente
("Completado con solución").

## 8. Calidad del feedback

- Cada desafío tiene tests de contenido que comprueban que la solución pasa y que el código
  base **produce los mensajes esperados**.
- Antes de la v1.0, al menos 5 estudiantes de la beta revisan los mensajes del módulo 1.
- En la vista de resultado hay un enlace "¿Este mensaje te confundió?" que abre un issue
  de GitHub con una plantilla (sin datos personales y sin enviar el código salvo que el
  alumno lo pegue).
