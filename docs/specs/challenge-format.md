# Formato de un desafío

> Spec de la carpeta de un módulo y de un desafío. Los schemas que los validan son
> `packages/contracts/module.schema.json` y `packages/contracts/challenge.schema.json`; si
> este documento y un schema no coinciden, se corrigen los dos en el mismo PR.

## Carpetas

```
content/challenges/m1-clases-y-objetos/
├── module.yaml
└── 03-la-facultad-donde-estudias/
    ├── challenge.yaml
    ├── starter/
    │   ├── Main.java
    │   └── FacultadRegional.java
    ├── solution/
    │   ├── Main.java
    │   └── FacultadRegional.java
    ├── solution.md
    └── tests/                      # opcional
        └── segunda-facultad/
            ├── Main.java
            └── expected.yaml
```

- Nombre de carpeta del módulo: `m<n>-<slug>`; del desafío: `<NN>-<slug>`, con `NN` igual a
  `order`.
- `starter/` y `solution/` sin `package`; una clase pública por archivo; siempre un
  `Main.java` con `public static void main(String[] args)`. Tienen los mismos archivos, y
  los que no son editables son idénticos en las dos carpetas.

## `module.yaml`

```yaml
id: m1
order: 1
title:
  es: "Clases, objetos y referencias"
goal:
  es: "Entender que una clase es un molde, que cada `new` crea un objeto con estado propio y que una variable apunta a un objeto."
```

## `challenge.yaml`

Las claves van en inglés; los textos que ve el alumno, como mapas por idioma (`es`
obligatorio, `en` opcional). Este es el desafío 1.3 tal como está publicado:

```yaml
id: m1-03
module: m1
order: 3
title:
  es: "La facultad donde estudiás"
concept: aliasing
rules: [R-UNI-02]
brief:
  es: |
    Ya existe la FR Resistencia, pero le falta la provincia. Declará una variable
    `miFacultad` que apunte a **la misma** facultad y, usando solo `miFacultad`, cargale
    la provincia: "Chaco".
criteria:
  - checks: [alias-declared]
    es: "`miFacultad` apunta a la FR Resistencia"
  - checks: [province-through-alias]
    es: "La provincia se cargó usando `miFacultad`"
editable: [Main.java]

scene:
  bindings:
    - type: FacultadRegional
      archetype: regional-faculty
      label: nombre
  pieces:
    - id: fr-resistencia
      type: FacultadRegional
      where: { field: nombre, equals: "Resistencia" }
      label: { es: "FR Resistencia" }
  showVariables: true
  realReference:
    regionalFaculties: [resistencia]

checks:
  - id: alias-declared
    type: shared_reference
    params:
      a: { local: resistencia, in: Main.main }
      b: { local: miFacultad, in: Main.main }
    piece: fr-resistencia
    highlight: declaration
    feedback:
      pass:
        what: { es: "`miFacultad` y `resistencia` apuntan a la misma facultad." }
        why: { es: "Asignar una referencia no copia el objeto: ahora hay dos cables al mismo edificio." }
      fail:
        what: { es: "`miFacultad` no apunta a la FR Resistencia." }
        why: { es: "Para compartir el objeto, `miFacultad` tiene que recibir la referencia que guarda `resistencia`." }
        hint: { es: "¿Qué tenés que poner a la derecha del `=` para no crear otra facultad?" }
    traps:
      - when:
          type: object_count
          params: { type: FacultadRegional, atLeast: 2 }
        what: { es: "Creaste una segunda facultad en vez de apuntar a la que ya existía." }
        why: { es: "Cada `new` construye un objeto nuevo; para compartir uno que ya existe se asigna su referencia." }
        hint: { es: "¿Qué tenés que poner a la derecha del `=` para no crear otra facultad?" }

  - id: province-through-alias
    type: object_field
    params:
      where: { type: FacultadRegional, field: nombre, equals: "Resistencia" }
      expect: { field: provincia, equals: "Chaco" }
      writtenThrough: { local: miFacultad, in: Main.main }
    piece: fr-resistencia
    highlight: last_write
    feedback:
      pass:
        what: { es: "La FR Resistencia ahora tiene su provincia: Chaco." }
        why: { es: "La cargaste a través de `miFacultad` y se ve también desde `resistencia`: es el mismo objeto." }
      fail:
        what: { es: "La provincia de la FR Resistencia todavía no se cargó." }
        why: { es: "Un cambio hecho a través de cualquiera de las dos variables modifica el mismo objeto." }
        hint: { es: "Usá `miFacultad`, un punto y el atributo que querés cargar." }
    traps:
      - when:
          type: object_field
          params:
            where: { type: FacultadRegional, field: nombre, equals: "Resistencia" }
            expect: { field: provincia, equals: "Chaco" }
        what: { es: "La provincia se cargó, pero no a través de `miFacultad`." }
        why: { es: "El pedido es llegar al objeto por la segunda variable, para comprobar que las dos apuntan al mismo." }
        hint: { es: "¿Qué variable está antes de `.provincia`?" }

hints:
  - es: "Una variable de tipo objeto guarda una referencia, no el objeto."
  - es: "Declará `FacultadRegional miFacultad = …` sin usar `new`."
  - es: "Asignale a `miFacultad` la variable `resistencia`, y después cargá la provincia a través de `miFacultad`."
```

Campos:

| Campo | Qué es |
|---|---|
| `id` | `<módulo>-<orden>` con dos dígitos (`m1-03`). Único y estable: nunca se reutiliza |
| `concept` | Etiqueta del concepto principal |
| `rules` | Reglas de [`../DOMAIN.md`](../DOMAIN.md) §4 en las que se apoya |
| `brief` | El pedido de obra: Markdown corto, hasta 600 caracteres |
| `criteria` | La lista "Lo que tenés que lograr". Cada ítem se tilda cuando pasan **todas** sus `checks` |
| `editable` | Archivos que el alumno puede editar; el resto es solo lectura |
| `limits` | Opcional. Solo puede **bajar** los límites de [`../SECURITY.md`](../SECURITY.md) §3 |
| `scene` | Cómo se dibuja el resultado (ver abajo) |
| `checks` | Las verificaciones, en el orden en que se informan |
| `hints` | Exactamente 3 pistas, de la más conceptual a la más concreta |

## `scene`

| Campo | Qué es |
|---|---|
| `bindings` | Con qué arquetipo de [`DESIGN.md`](../../DESIGN.md) §B4 se dibuja cada clase. `label` es el atributo que va en el cartel; `slots` asocia atributos de composición con el nombre de un slot (`decano: dean`) |
| `pieces` | Las piezas que el pedido manda construir. Cada una tiene `id`, `type`, un `label` para cuando todavía no existe y, si hace falta distinguirla, `where: { field, equals }` |
| `showVariables` | Dibuja las variables de `main` que apuntan a un objeto |
| `realReference` | La parte de la UTN real con la que se compara, por id de `content/domain/` |

Cada pieza de `pieces` se empareja con el primer objeto de su tipo que cumple `where` (sin
`where`, con el primero que quede libre). Si el código todavía no creó ese objeto, la pieza
se informa igual, como **no construida**. Los objetos que el pedido no menciona también
son piezas, con un id generado (`facultadregional-1`).

## `checks`

Cada verificación tiene:

| Campo | Qué es |
|---|---|
| `id` | Único dentro del desafío |
| `type`, `params` | Qué comprueba (tabla de abajo) |
| `piece` | La pieza de `scene.pieces` de la que habla. No va si habla de todo el programa |
| `slot` | Opcional: el slot de esa pieza al que se refiere |
| `highlight` | Qué línea señalar: `object_creation`, `declaration` o `last_write`. Si esa línea no existe se usa otra que la verificación conozca y, si no hay ninguna, la de `main` |
| `feedback` | `pass: { what, why }` y `fail: { what, why, hint }` |
| `traps` | Errores comunes: si la verificación falla y se cumple el `when` de una trampa, su mensaje reemplaza al de `fail` |

Tipos y parámetros (los tipos son los de [`../ARCHITECTURE.md`](../ARCHITECTURE.md) §6):

| Tipo | Parámetros |
|---|---|
| `class_exists` | `class` |
| `extends` | `class`, `superclass` |
| `implements` | `class`, `interface` |
| `is_abstract` | `class`; con `method`, comprueba ese método |
| `field` | `class`, `name` o `names`; opcionales `type`, `visibility`, `final`, `static` |
| `constructor` | `class`, `parameterTypes`; opcionales `visibility` y `delegatesTo: this \| super` |
| `method` | `class`, `name`; opcionales `parameterTypes`, `returnType`, `visibility`, `static`, `abstract`, `override` |
| `object_count` | `type` y uno de `equals`, `atLeast`, `atMost`; opcional `where: { field, equals \| notEquals }` |
| `object_field` | `where: { type, field, equals \| notEquals }`, `expect` (uno o varios `{ field, equals \| notNull \| isNull }`); opcionales `all: true` y `writtenThrough: { local, in }` |
| `shared_reference` | `a` y `b`, cada uno `{ local, in }` o `{ object: { type, field, equals }, field }`; o `all: { type, field }` |
| `call_dispatch` | `method`, `receiverType`, `implementation` |
| `stdout_contains` | `text`; opcional `ignoreCase` |
| `no_exception` | — |
| `throws` | `type`; opcionales `messageContains` y `caught` |

- Los tipos se escriben como en el código, con nombres simples: `String`,
  `List<Departamento>`.
- Un `type` selecciona también los objetos de sus subclases.
- `delegatesTo` y `call_dispatch` miran lo que pasó al ejecutar: necesitan que el `main`
  del desafío use ese constructor o ese método.
- `writtenThrough` comprueba por qué variable se llegó al objeto. La traza sabe qué objeto
  se escribió, no con qué variable: eso se lee del texto de la línea que hizo la escritura
  o que llamó al método que la hizo ([ADR 0017](../adr/0017-piezas-y-verificaciones-sobre-la-traza.md)).
- Una excepción sin atrapar hace fallar la ejecución, salvo que una verificación `throws`
  del desafío la pida.

Algunos tipos llenan marcadores en los textos de `feedback` y de `traps`:

| Tipo | Marcador | Valor |
|---|---|---|
| `field`, `object_field` | `{missing}` | Los atributos que faltan o no tienen el valor esperado |
| `object_count` | `{count}` | Cuántos objetos hay |

## `tests/`

Cada subcarpeta es una variante del código del alumno: los archivos editables que cambia y
un `expected.yaml` con lo que el resultado tiene que decir. Sirve para comprobar las
trampas.

```yaml
outcome: incomplete
log:
  - check: alias-declared
    state: incomplete
    title: "Creaste una segunda facultad en vez de apuntar a la que ya existía."
```

`title` es opcional; si está, el título de la entrada tiene que ser exactamente ese.

## Reglas de validación

La api las comprueba todas al arrancar y **no arranca** si alguna falla:

- Cada archivo cumple su schema.
- `id` único en todo `content/`; `order` único dentro del módulo; `module` y `order`
  coinciden con las carpetas.
- Toda `check` nombrada en `criteria` existe; todo `piece` nombrado en una `check` existe
  en `scene.pieces`.
- Los `params` de cada verificación y de cada trampa son los de su tipo, sin campos de más.
- Un texto solo usa los marcadores que llena el tipo de su verificación.
- Toda clase de `solution/` (salvo `Main`) tiene un binding; si no tiene dibujo propio, se
  declara con `archetype: generic-block`.
- Todo texto visible tiene al menos `es`.
- `rules` solo contiene ids de `content/domain/rules.json`, y `realReference`, ids de
  `content/domain/`.
- `feedback.fail` siempre lleva `hint`; en una trampa es opcional.
- "Qué pasó" y "por qué" suman hasta 280 caracteres ([`../FEEDBACK.md`](../FEEDBACK.md)
  §3). El resto de las reglas de redacción las revisa una persona.
- `solution.md` existe y no está vacío.
- Las variantes de `tests/` solo cambian archivos editables.

## Tests de contenido (CI)

Para cada desafío, con el runner real:

1. `solution/` compila, se ejecuta y pasa **todas** las verificaciones.
2. `starter/` compila, se ejecuta y **no** pasa todas las verificaciones.
3. Cada variante de `tests/` obtiene el resultado de su `expected.yaml`.
