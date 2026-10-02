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
| **Límite** | Tiempo, memoria, pasos o salida | Lo construido hasta el corte | ❌ |
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

Catálogo mínimo de la v1:

| Código `javac` | Qué pasó (plantilla) | Por qué |
|---|---|---|
| `compiler.err.cant.resolve.location` | "Java no conoce `{símbolo}`." | Hay que declararlo antes de usarlo, o revisar mayúsculas y nombre. |
| `compiler.err.expected` | "Falta un `{token}` cerca de acá." | Java necesita esa marca para entender dónde termina algo. |
| `compiler.err.prob.found.req` | "Esperaba un `{requerido}` y recibió un `{encontrado}`." | Cada variable acepta solo valores de su tipo. |
| `compiler.err.cant.apply.symbol` | "`{método}` no recibe esos argumentos." | Los argumentos tienen que coincidir en cantidad, orden y tipo. |
| `compiler.err.report.access` | "`{miembro}` es privado de `{clase}`." | Encapsulamiento: solo la propia clase lo toca; usá sus métodos públicos. |
| `compiler.err.does.not.override.abstract` | "`{clase}` no implementa `{método}`." | Al extender una clase abstracta o implementar una interfaz, prometiste ese método. |
| `compiler.err.abstract.cant.be.instantiated` | "No se puede hacer `new {clase}`." | Una clase abstracta es un molde incompleto: se instancian sus subclases. |
| `compiler.err.cant.assign.val.to.var` | "`{campo}` es `final`: no se puede volver a asignar." | `final` fija el valor después de construir el objeto. |
| `compiler.err.missing.ret.stmt` | "A `{método}` le falta un `return`." | Si declara que devuelve algo, todos los caminos tienen que devolverlo. |
| `compiler.err.call.must.be.first.stmt.in.ctor` / `compiler.err.cant.ref.before.ctor.called` | "`super(...)` tiene que ir antes que lo demás." | La parte de la superclase se construye primero (la planta baja antes que el piso). |
| `compiler.err.non-static.cant.be.ref` | "`{miembro}` necesita un objeto." | Desde un método `static` no hay `this`: primero hacé `new`. |
| `compiler.err.var.might.not.have.been.initialized` | "`{variable}` puede no tener valor." | Las variables locales no tienen valor por defecto. |
| `compiler.err.class.public.should.be.in.file` | "La clase `{clase}` tiene que estar en `{clase}.java`." | Una clase pública por archivo, con el mismo nombre. |

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
| `OutOfMemoryError` | "Se crearon demasiados objetos." | La memoria del programa tiene un límite. | Pieza ❌ |

La línea que se muestra es la **primera línea del código del alumno** en la pila de la
excepción.

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
