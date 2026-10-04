# Arquitectura

> Spec técnica. Define componentes, contratos y flujo de una ejecución. Las decisiones que
> la sostienen están en [`adr/`](adr/). La seguridad del sandbox se detalla en
> [`SECURITY.md`](SECURITY.md).

## 1. Vista general

```
┌──────────────────────────┐   HTTPS    ┌──────────────────────────┐  red privada  ┌──────────────────────────┐
│ apps/web                 │──────────► │ services/api             │─────────────► │ services/runner          │
│ React + TS + Vite        │  /api/*    │ Spring Boot 4 · Java 25  │  /internal/*  │ Spring Boot 4 · Java 25  │
│ R3F (Three.js) · Monaco  │ ◄──────────│ contenido · verificación │ ◄─────────────│ compila · verifica ·     │
│ Cloudflare Pages         │   JSON     │ feedback · rate limit    │   traza JSON  │ ejecuta con JDI          │
└──────────────────────────┘            │ Railway (público)        │               │ Railway (solo privado)   │
                                        └──────────────────────────┘               └────────────┬─────────────┘
                                                                                                │ proceso hijo
                                                                                   ┌────────────▼─────────────┐
                                                                                   │ JVM del alumno           │
                                                                                   │ límites de memoria/tiempo│
                                                                                   └──────────────────────────┘
```

| Componente | Responsabilidad | No hace |
|---|---|---|
| `apps/web` | Editor, vista de resultado, maqueta 3D, progreso local, i18n | Ejecutar o validar Java |
| `services/api` | Servir desafíos, validar pedidos, invocar al runner, evaluar verificaciones, armar el feedback | Compilar ni ejecutar código del alumno |
| `services/runner` | Compilar, verificar bytecode, ejecutar bajo JDI, devolver estructura + traza | Conocer desafíos, verificaciones o textos |
| `content/` | Desafíos, textos, datos de la UTN real (con fuentes) | Lógica |
| `packages/contracts` | JSON Schemas de todos los contratos | — |

**Regla clave:** el código del alumno solo se ejecuta en el runner. La API trabaja sobre
**datos** (estructura y traza), nunca vuelve a ejecutar código.

## 2. Estructura del monorepo

```
.
├── apps/
│   └── web/                    # frontend
├── services/
│   ├── api/                    # Spring Boot: API pública
│   └── runner/                 # Spring Boot: ejecución aislada
├── packages/
│   └── contracts/              # JSON Schemas (fuente de verdad de los contratos)
├── content/
│   ├── challenges/<module>/<NN-slug>/   # un desafío por carpeta
│   ├── domain/                 # datos de la UTN real + fuentes
│   ├── feedback/               # catálogos de mensajes: javac, excepciones, límites, rechazos
│   └── i18n/                   # textos de interfaz por idioma
├── docs/                       # specs, ADRs
├── DESIGN.md
└── AGENTS.md
```

## 3. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje backend | Java 25 (LTS) |
| Framework backend | Spring Boot 4.x, Maven (wrapper `./mvnw`) |
| Análisis de bytecode | API `java.lang.classfile` del JDK (sin dependencias externas) |
| Compilación | `javax.tools.JavaCompiler` en memoria |
| Ejecución y traza | JDI (Java Debug Interface, módulo `jdk.jdi`) |
| Rate limit | Bucket4j |
| Frontend | React 19, TypeScript (strict), Vite |
| 3D | Three.js vía React Three Fiber + drei |
| Editor | Monaco (`@monaco-editor/react`) |
| Estado | Zustand |
| Rutas | React Router |
| i18n | i18next + react-i18next |
| Gestor de paquetes JS | pnpm |
| Tests | JUnit 5 + AssertJ (Java), Vitest + Testing Library (web) |
| Formato y lint | Spotless (google-java-format), ESLint, Prettier |
| CI | GitHub Actions |
| Hosting | Railway (api + runner), Cloudflare Pages (web) |

Las versiones exactas se fijan en los archivos de build; este documento no las repite.

## 4. Flujo de una ejecución

1. **web** envía `POST /api/v1/runs` con `{ challengeId, files: [{ path, content }] }`.
2. **api** valida: desafío existente, tamaño total ≤ 64 KB, rate limit por IP y que los
   archivos enviados sean editables en ese desafío. Arma el programa con los archivos del
   desafío, reemplazando los editables por los del alumno; los de solo lectura salen
   siempre del desafío. Si algo falla responde `400`/`404`/`429` con un error estructurado.
3. **api** llama a **runner** `POST /internal/v1/executions` con los archivos y los límites del
   desafío, autenticándose con `RUNNER_TOKEN`.
4. **runner**:
   1. **Compila** en memoria (`--release 25`, `-proc:none`, `-Xlint:none`, `-g`).
      Si falla, devuelve `status: "compile_error"` con diagnósticos (archivo, línea, columna,
      código de `javac`, mensaje). Una declaración `package` o un `Main` sin
      `public static void main(String[] args)` se informan igual, con códigos `ljbu.err.*`.
   2. **Extrae la estructura** de las clases compiladas: clases, superclase, interfaces,
      campos (tipo, visibilidad, `final`, `static`), constructores, métodos, líneas de
      declaración y `@Override`. Las líneas y `@Override` no están en el bytecode: salen
      del árbol de sintaxis de `javac` ([ADR 0016](adr/0016-datos-del-fuente-desde-el-arbol-de-javac.md)).
   3. **Verifica el bytecode** contra la lista permitida ([`SECURITY.md`](SECURITY.md)).
      Si hay un uso prohibido, devuelve `status: "rejected"` con cada símbolo y su línea.
   4. **Ejecuta** el `main` en una JVM hija bajo JDI, registrando la traza.
   5. Devuelve `status: "completed" | "runtime_error" | "timeout" | "limit_exceeded"`,
      la estructura, la traza y la salida estándar (truncada).
5. **api** valida la respuesta del runner contra el schema de la traza, evalúa las
   verificaciones del desafío sobre estructura + traza y arma el **resultado**: piezas con
   estado, mensajes de bitácora y línea de tiempo.
6. **web** muestra la vista de resultado.

Tiempo total objetivo: p95 < 4 s.

## 5. Contratos

Fuente de verdad: JSON Schemas en `packages/contracts/` (`trace.schema.json`,
`result.schema.json`, `challenge.schema.json`). Los tipos de TypeScript se **generan** desde
ahí (`pnpm --filter @ljbu/contracts generate`); los `record` de Java se validan contra los
mismos schemas en tests. El detalle campo por campo está en los schemas; los ejemplos de
esta sección viven en `packages/contracts/examples/` y se validan en CI.

### 5.1 API pública (`/api/v1`)

| Método | Ruta | Descripción | Schema de la respuesta |
|---|---|---|---|
| `GET` | `/modules` | Módulos y desafíos (metadatos, sin soluciones) | `module-list` |
| `GET` | `/challenges/{id}` | Lo que hace falta para trabajar: pedido, criterios, código base, reglas y referencia real | `challenge-view` |
| `POST` | `/runs` | Ejecuta y devuelve el resultado (síncrono). Cuerpo: `run-request` | `result` |
| `GET` | `/challenges/{id}/hints/{level}` | Pista de nivel 1..3 | `hint` |
| `GET` | `/challenges/{id}/solution` | Solución y su explicación | `solution` |
| `GET` | `/health` | Estado del servicio | — |

- Los textos se devuelven ya en un idioma (`?lang=es`, que es el valor por defecto).
- Errores con formato `application/problem+json` (RFC 9457) y un campo `code`:
  `challenge_not_found`, `hint_not_found`, `not_editable`, `duplicate_file`,
  `files_too_large` (400/404); `rate_limited` y `run_in_progress` (429, con `Retry-After`);
  `runner_busy` (503) y `runner_unavailable` (502).
- `GET /challenges/{id}` no incluye verificaciones, pistas ni solución. Pistas y solución
  se sirven por separado para que la interfaz las muestre recién cuando corresponde. No es
  un control de seguridad: el contenido es abierto.

### 5.2 Ejecución y traza (api ↔ runner)

`POST /internal/v1/executions`, con `Authorization: Bearer <RUNNER_TOKEN>`
(`execution-request.schema.json`):

```jsonc
{
  "files": [{ "path": "Main.java", "content": "…" }],
  "limits": { "timeoutMs": 5000 }        // opcional: solo puede bajar el límite
}
```

Responde `200` con la traza, cualquiera sea el resultado del programa. Responde `400` si el
pedido rompe una regla de entrada ([`SECURITY.md`](SECURITY.md) §3, capa 1), `401` sin el
token y `503` si la cola de ejecuciones está llena; el `400` y el `503` van como
`application/problem+json` con un campo `code`.

Traza de la solución del desafío 1.3 (`trace.schema.json`; completa en
`packages/contracts/examples/trace.completed.json`, que un test del runner compara con lo
que el runner produce de verdad):

```jsonc
{
  "status": "completed",
  "structure": { "classes": [ /* nombre, superclase, campos, constructores, métodos, líneas */ ] },
  "stdout": "",
  "steps": [
    { "index": 0, "file": "Main.java", "line": 3, "event": "call", "method": "Main.main", "args": [] },
    { "index": 1, "file": "Main.java", "line": 3, "event": "object_created",
      "object": { "id": "o1", "type": "FacultadRegional" } },
    { "index": 2, "file": "Main.java", "line": 3, "event": "call",
      "method": "FacultadRegional.<init>", "target": "o1", "args": [] },
    { "index": 3, "file": "FacultadRegional.java", "line": 1, "event": "return",
      "method": "FacultadRegional.<init>" },
    { "index": 4, "file": "Main.java", "line": 3, "event": "local_set",
      "method": "Main.main", "name": "resistencia", "value": { "ref": "o1" } },
    { "index": 5, "file": "Main.java", "line": 4, "event": "field_set",
      "target": "o1", "field": "nombre", "value": { "string": "Resistencia" } },
    { "index": 6, "file": "Main.java", "line": 5, "event": "field_set",
      "target": "o1", "field": "ciudad", "value": { "string": "Resistencia" } },
    { "index": 7, "file": "Main.java", "line": 7, "event": "local_set",
      "method": "Main.main", "name": "miFacultad", "value": { "ref": "o1" } },
    { "index": 8, "file": "Main.java", "line": 8, "event": "field_set",
      "target": "o1", "field": "provincia", "value": { "string": "Chaco" } },
    { "index": 9, "file": "Main.java", "line": 9, "event": "return", "method": "Main.main" }
  ],
  "heap": {
    "o1": { "type": "FacultadRegional",
            "fields": { "nombre": { "string": "Resistencia" }, "ciudad": { "string": "Resistencia" },
                        "provincia": { "string": "Chaco" } } }
  },
  "statics": {},
  "exception": null,
  "limits": { "steps": 10, "truncated": false, "exceeded": null }
}
```

La forma depende de `status`: con `compile_error` solo viaja `diagnostics` (archivo, línea,
columna, código y mensaje); con `rejected`, `structure` y `violations` (archivo, línea y
símbolo prohibido); en el resto, lo de arriba.

Cada paso tiene `index`, `file`, `line` y `event`, más los datos del evento:

| Evento | Cuándo | Datos |
|---|---|---|
| `call` | Empieza un método o constructor del alumno | `method` (la implementación que corre: `Clase.metodo`, o `Clase.<init>`), `target` (el receptor; no va en métodos `static`), `args`. La línea es la de la llamada |
| `return` | Termina sin excepción | `method`, `value` (no va si devuelve `void`) |
| `object_created` | Se crea un objeto de una clase del alumno | `object: { id, type }`. La línea es la del `new` |
| `field_set` | Se escribe un campo de una clase del alumno | `target` (el objeto) o `ownerClass` (si el campo es `static`), `field`, `value` |
| `local_set` | Una variable local recibe un valor | `method`, `name`, `value`. Los parámetros no generan este evento: llegan en `args` |
| `output` | La línea escribió en la salida estándar | `text` |
| `exception` | Se lanza una excepción | `exception` (tipo, mensaje, archivo y línea del código del alumno), `caught` (si la atrapa el alumno; si no, termina el programa) |

- Valores: `{ "int": 3 }` (también `byte`, `short` y `long`), `{ "double": 1.5 }` (también
  `float`; `NaN` e infinitos viajan como texto), `{ "boolean": true }`, `{ "char": "a" }`,
  `{ "string": "…" }` (hasta 1 000 caracteres), `{ "ref": "o1" }`, `{ "null": true }`.
- `String` y los wrappers se tratan como **valores** (son inmutables); el resto de los
  objetos, como referencias.
- `heap` es el **estado final** de todo objeto que la traza menciona. Los objetos del
  alumno llevan sus campos; los arreglos, listas y sets llevan `size` y `elements`; los
  maps, `size` y `entries` (hasta 200 en ambos casos); cualquier otro objeto del JDK va
  sin campos. `statics` trae los campos `static` de las clases del alumno.
- Solo se registran eventos en clases del alumno. Los constructores se registran en el
  orden real: el de la superclase termina antes que el de la subclase.
- `limits.exceeded` dice qué límite cortó el programa (`steps`, `objects`, `output` o
  `call_depth`) cuando `status` es `limit_exceeded`.

Limitaciones conocidas de la v1:

- El contenido de arreglos y colecciones se lee al final: un `add` no genera un paso.
- Las variables locales se comparan al pasar de una línea a otra: un bucle escrito entero
  en una sola línea no genera pasos intermedios.
- La JVM no ejecuta un `main` de cuerpo vacío: ese programa termina `completed` sin pasos.
- El depurador no informa un evento en la misma posición de código que el anterior. Una
  recursión cuyo método empieza llamándose a sí mismo no deja pasos de las llamadas
  internas; igual se corta por profundidad.

### 5.3 Contenido (`content/`)

```
challenges/<module>/module.yaml               # título y objetivo del módulo
challenges/<module>/<NN-slug>/challenge.yaml  # metadatos, pedido, verificaciones, escena, pistas
challenges/<module>/<NN-slug>/starter/*.java  # código base
challenges/<module>/<NN-slug>/solution/*.java # solución de referencia
challenges/<module>/<NN-slug>/solution.md     # explicación de la solución
challenges/<module>/<NN-slug>/tests/          # variantes con el resultado esperado
domain/*.json                                 # reglas y unidades de la UTN real, con fuente
feedback/*.es.yaml                            # catálogos de mensajes
```

El formato completo está en [`specs/challenge-format.md`](specs/challenge-format.md). La
api carga todo al arrancar y no arranca si algo es inválido.

### 5.4 Resultado (api → web)

`result.schema.json`; ejemplo completo en `packages/contracts/examples/result.incomplete.json`.

```jsonc
{
  "runId": "…",
  "outcome": "incomplete",              // passed | incomplete | failed
  "progress": { "passed": 1, "total": 2 },
  "pieces": [
    { "id": "fr-resistencia", "archetype": "regional-faculty", "state": "incomplete",
      "built": true, "label": "Resistencia", "sourceRef": { "file": "Main.java", "line": 5 },
      "slots": { "dean": { "state": "missing", "pieceIds": [] } } },
    { "id": "var-resistencia", "archetype": "variable-sign", "state": "passed",
      "built": true, "label": "resistencia", "target": "fr-resistencia" }
  ],
  "log": [
    { "state": "incomplete", "title": "La FR Resistencia está sin decano.",
      "why": "…", "sourceRef": { "file": "Main.java", "line": 5 }, "hint": "…",
      "pieceId": "fr-resistencia", "checkId": "dean-assigned" }
  ],
  "timeline": [
    { "index": 0, "sourceRef": { "file": "Main.java", "line": 5 }, "event": "object_created",
      "pieceId": "fr-resistencia", "name": "FacultadRegional" },
    { "index": 1, "sourceRef": { "file": "Main.java", "line": 5 }, "event": "local_set",
      "pieceId": "var-resistencia", "targetPieceId": "fr-resistencia",
      "name": "resistencia", "value": { "ref": "o1" } }
  ],
  "stdout": "…"
}
```

- `outcome`: `failed` si no compiló, se rechazó, lanzó una excepción que el desafío no
  pide o lo cortó un límite; `passed` si pasan todas las verificaciones; si no,
  `incomplete`.
- `log` sigue el orden de [`FEEDBACK.md`](FEEDBACK.md) §2. `checkId` permite tildar los
  criterios del pedido; `detail` trae el texto original de `javac` o de la JVM cuando no
  hay un mensaje propio.
- `pieces`: las que pide el desafío (con `built: false` si el código no las creó), los
  demás objetos de clases del alumno y las variables de `main` que apuntan a un objeto
  (`target`) o a nada. La pieza cuyo método se estaba ejecutando al saltar una excepción
  queda `failed`.
- `timeline` es la traza del runner paso por paso, con la pieza que toca cada paso
  (`pieceId`) y la pieza a la que pasa a apuntar un campo o una variable (`targetPieceId`).
  `name` es la clase creada, el campo o la variable escritos, el método llamado o el tipo
  de la excepción, según el evento.
- Si el programa no llegó a ejecutarse, `pieces` y `timeline` van vacías.

## 6. Verificaciones

Las verificaciones son **declarativas** (definidas en `challenge.yaml`) y se evalúan en la
api sobre estructura + traza. Tipos de la v1:

| Tipo | Comprueba |
|---|---|
| `class_exists` | Existe una clase con ese nombre |
| `extends` / `implements` | Herencia o interfaz |
| `is_abstract` | Clase o método abstracto |
| `field` | Campo con tipo, visibilidad, `final`, `static` |
| `constructor` | Constructor con esos parámetros; opcional: llama a `super(...)` / `this(...)` |
| `method` | Método con firma, visibilidad y opcional `@Override` |
| `object_count` | Cantidad de objetos creados de un tipo |
| `object_field` | Un objeto (elegido por un campo, p. ej. `nombre == "Resistencia"`) tiene un valor |
| `shared_reference` | Dos variables o campos apuntan al mismo objeto (aliasing) |
| `call_dispatch` | Una llamada polimórfica ejecutó la implementación de cierto tipo |
| `stdout_contains` | La salida contiene un texto |
| `no_exception` / `throws` | Terminó sin excepción, o lanzó la esperada |

Cada verificación declara: `id`, tipo y parámetros, la **pieza** que afecta, el mensaje de
bitácora para cada resultado y qué línea se resalta. Los parámetros de cada tipo están en
[`specs/challenge-format.md`](specs/challenge-format.md). Agregar un tipo nuevo requiere un
ADR.

Se evalúan siempre que el programa haya llegado a ejecutarse, aunque después lo haya
frenado una excepción o un límite: lo construido hasta ahí también recibe su mensaje.

## 7. Escena

- Cada desafío define **bindings**: qué tipo del alumno se dibuja con qué **arquetipo**
  (`FacultadRegional → regional-faculty`), qué campo es el cartel y qué campos son
  **slots** (p. ej. `decano → dean`).
- Cada desafío declara las **piezas** que pide construir; la api las empareja con los
  objetos que creó el código ([ADR 0017](adr/0017-piezas-y-verificaciones-sobre-la-traza.md)).
- Los arquetipos son modelos **generados por código** en `apps/web/src/scene/archetypes/`
  ([`DESIGN.md`](../DESIGN.md) §B).
- Tipos sin binding se dibujan con el arquetipo genérico `generic-block`.
- Para `inheritance-floors` la api agrega `floors`: las clases del objeto desde la cima de
  la jerarquía hasta la propia, un piso cada una. Para toda pieza agrega `interfaces`: las
  interfaces del alumno que implementa su clase (o una superclase), una insignia cada una.
- Para que la maqueta muestre **Java** y no solo la UTN ([`DESIGN.md`](../DESIGN.md) §B4),
  el resultado trae además `classes` (las clases del alumno tal como las extrajo el runner:
  atributos con visibilidad, `static` y `final`; constructores; métodos) y cada pieza
  construida trae `type` (su clase) y `fields` (el valor final de cada atributo, con el
  `pieceId` cuando es una referencia a otra pieza). Los valores intermedios salen de los
  `field_set` de la línea de tiempo; qué método corre en cada paso, de los `call`.
- La **silueta real** se arma con datos de `content/domain/`: `realReference` del desafío
  nombra FR (`regional-faculties.json`) y órganos (`governing-bodies.json`); la api los
  resuelve y la vista del desafío los muestra con ciudad, provincia, composición y mandato.
- En la web (`apps/web/src/scene/`): `layout.ts` ubica las piezas (Rectorado al centro,
  anillos, ocupantes de slots junto a su dueño, variables al frente); `replay.ts` deriva
  de la línea de tiempo qué existe en cada paso (objetos creados, a qué apunta cada
  variable, qué slots se llenaron, cuántos pisos construyeron los constructores);
  `overlay.tsx` dibuja carteles e íconos como HTML anclado a la escena; `SceneView.tsx`
  arma el canvas y se carga de forma diferida.

## 8. Frontend

| Ruta | Vista |
|---|---|
| `/` | Inicio: módulos y progreso |
| `/modulos/:moduleId` | Lista de desafíos del módulo |
| `/desafios/:challengeId` | Pedido de obra (panel desplegable), editor, maqueta y bitácora en una sola pantalla |
| `/desafios/:challengeId/resultado` | Redirige al desafío (la vista de resultado dejó de ser una página) |
| `/dev/escena` | Solo en desarrollo: escena sintética de hasta 150 piezas con contador de fps (`?piezas=N`) |

- **Progreso**: `localStorage`, clave versionada `ljbu.progress.v1` (estado, ejecuciones,
  pistas vistas y si se vio la solución, por desafío); lectura y escritura siempre en
  `try/catch` (la app funciona sin almacenamiento).
- El código en edición se guarda por desafío en `ljbu.code.v1` para no perderlo al
  recargar; el tema elegido, en `ljbu.theme.v1`.
- El último resultado vive solo en memoria: al recargar, la maqueta y la bitácora
  vuelven a estar vacías hasta la próxima ejecución.
- Al ejecutar, la web manda solo los archivos editables; la api completa el programa con
  los de solo lectura.
- Monaco se empaqueta con la app (solo el editor y el lenguaje Java): no se carga desde una
  CDN.
- La maqueta es un `role="img"` con una descripción; su equivalente textual es la bitácora y
  la lista de piezas que queda bajo ella (`<details>`), usable con teclado.
- Todos los textos de interfaz salen de `content/i18n/<lang>.json`; ningún texto visible
  queda escrito en componentes. Los mensajes de la bitácora pueden citar código del
  alumno: se muestran como texto, nunca como HTML.

## 9. Configuración

Toda la configuración por variables de entorno; ningún secreto en el repo.

| Variable | Servicio | Uso |
|---|---|---|
| `RUNNER_URL` | api | URL privada del runner |
| `RUNNER_TOKEN` | api, runner | Autenticación api → runner |
| `RUNNER_MAX_CONCURRENT` | runner | Ejecuciones simultáneas (default 2) |
| `RUNNER_QUEUE_CAPACITY` | runner | Ejecuciones en espera antes de responder `503` (default 8) |
| `ALLOWED_ORIGINS` | api | CORS: orígenes de la web, separados por coma |
| `CONTENT_DIR` | api | Carpeta de `content/` (en la imagen, `/app/content`) |
| `RATE_LIMIT_RUNS_PER_MINUTE` | api | Por IP (default 10) |
| `VITE_API_URL` | web | URL pública de la api |

## 10. Observabilidad

- Logs estructurados (JSON) con `runId`, duración por etapa y `status`.
- **Nunca** se registra el código del alumno ni su salida.
- Estado del servicio para Railway: `GET /api/v1/health` en la api y `GET /health` en el
  runner. Responden `{ "status": "ok" }` sin autenticación.

## 11. Calidad

Nivel **intermedio** ([ADR 0010](adr/0010-calidad-intermedia.md)):

- Tests unitarios en los tres componentes; tests de integración api ↔ runner: los tests
  de la api levantan el runner real como proceso y pasan por HTTP, así que el motor de
  verificaciones y los catálogos se prueban contra lo que `javac` y la JVM dicen de verdad.
- **Tests de contenido**: en CI, por cada desafío, la `solution/` pasa todas las
  verificaciones y el `starter/` no.
- GitHub Actions en cada PR: formato, lint, typecheck, tests y build.
- Sin tests end-to-end en la v1.
