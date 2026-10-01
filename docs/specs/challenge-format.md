# Formato de un desafío

> Spec del archivo `challenge.yaml` y de la carpeta de un desafío. El schema que lo valida
> es `packages/contracts/challenge.schema.json`; si este documento y el schema no
> coinciden, se corrigen los dos en el mismo PR.

## Carpeta

```
content/challenges/m1-clases-y-objetos/03-la-facultad-donde-estudias/
├── challenge.yaml
├── starter/
│   ├── Main.java
│   └── FacultadRegional.java
├── solution/
│   ├── Main.java
│   └── FacultadRegional.java
└── solution.md
```

- Nombre de carpeta del módulo: `m<n>-<slug>`; del desafío: `<NN>-<slug>`.
- `starter/` y `solution/` sin `package`; una clase pública por archivo; siempre un
  `Main.java` con `public static void main(String[] args)`.

## `challenge.yaml`

Las claves van en inglés; los textos que ve el alumno, como mapas por idioma (`es`
obligatorio, `en` opcional).

```yaml
id: m1-03                          # único, estable: nunca se reutiliza
module: m1
order: 3
title:
  es: "La facultad donde estudiás"
concept: aliasing                  # etiqueta del concepto principal
rules: [R-UNI-02]                  # reglas de docs/DOMAIN.md
brief:                             # pedido de obra (Markdown corto, ≤ 600 caracteres)
  es: |
    Ya existe la FR Resistencia. Creá una variable `miFacultad` que apunte a **la misma**
    facultad y, usando solo `miFacultad`, actualizá su ciudad a "Resistencia, Chaco".
criteria:                          # lo que ve el alumno como lista de "Lo que tenés que lograr"
  - check: alias-declared
    es: "`miFacultad` apunta a la FR Resistencia"
  - check: city-updated-through-alias
    es: "La ciudad se actualizó usando `miFacultad`"
editable: [Main.java]              # archivos que el alumno puede editar; el resto es solo lectura
limits:                            # opcional: si no está, se usan los de SECURITY.md
  timeoutMs: 5000

scene:
  bindings:
    - type: FacultadRegional
      archetype: regional-faculty
      label: nombre
      slots: {}
  showVariables: true              # dibuja carteles de variables locales de main
  context: [m1-01, m1-02]          # piezas de desafíos anteriores que aparecen como contexto
  realReference:                   # silueta de la UTN real
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
        why: { es: "Para compartir el objeto, `miFacultad` tiene que recibir la referencia de `resistencia`." }
        hint: { es: "¿Qué tenés que poner a la derecha del `=` para no crear otra facultad?" }
    traps:                         # detecciones específicas de errores comunes
      - when: { type: object_count, params: { type: FacultadRegional, atLeast: 2 } }
        what: { es: "Creaste una segunda FR en vez de apuntar a la existente." }
        why: { es: "Cada `new` construye un edificio nuevo; el aliasing reutiliza el que ya hay." }

  - id: city-updated-through-alias
    type: object_field
    params:
      where: { type: FacultadRegional, field: nombre, equals: "Resistencia" }
      expect: { field: ciudad, equals: "Resistencia, Chaco" }
      writtenThrough: { local: miFacultad }
    piece: fr-resistencia
    highlight: last_write
    feedback:
      pass:
        what: { es: "La FR Resistencia ahora dice \"Resistencia, Chaco\"." }
        why: { es: "Modificaste el objeto a través de `miFacultad`, y se ve también desde `resistencia`." }
      fail:
        what: { es: "La ciudad de la FR Resistencia no cambió." }
        why: { es: "Un cambio hecho a través de cualquiera de las dos variables modifica el mismo objeto." }
        hint: { es: "Usá `miFacultad.` seguido del atributo que querés cambiar." }

hints:
  - es: "Una variable de tipo objeto guarda una referencia, no el objeto."
  - es: "Declará `FacultadRegional miFacultad = …` sin usar `new`."
  - es: "`FacultadRegional miFacultad = resistencia;` y después asigná la ciudad a través de `miFacultad`."
```

## Reglas de validación

- `id` único en todo `content/`; `order` único dentro del módulo.
- Todo `check` referido en `criteria` existe en `checks`.
- Toda clase pública declarada en `solution/` tiene un binding o se acepta como
  `generic-block` explícitamente (`bindings: [{ type: X, archetype: generic-block }]`).
- Todo texto visible tiene al menos `es`.
- `rules` solo contiene IDs presentes en `content/domain/rules.json`.
- Exactamente 3 `hints`.
- Los textos de feedback cumplen las reglas de [`../FEEDBACK.md`](../FEEDBACK.md) §3
  (revisión humana; el validador solo controla longitudes).

## Tests de contenido (CI)

Para cada desafío:

1. `solution/` compila, se ejecuta y pasa **todas** las verificaciones.
2. `starter/` compila, se ejecuta y **no** pasa todas las verificaciones.
3. Opcional: `tests/*.java` con variantes de código y el resultado esperado
   (`expected.yaml`), para cubrir las trampas.
