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
2. **api** valida: desafío existente, tamaño total ≤ 64 KB, ≤ 10 archivos, nombres válidos,
   rate limit por IP. Si falla responde `400`/`429` con un error estructurado.
3. **api** llama a **runner** `POST /internal/v1/executions` con los archivos y los límites del
   desafío.
4. **runner**:
   1. **Compila** en memoria (`--release 25`, `-proc:none`, `-Xlint:none`).
      Si falla, devuelve `status: "compile_error"` con diagnósticos (archivo, línea, columna,
      código de `javac`, mensaje).
   2. **Extrae la estructura** de las clases compiladas: clases, superclase, interfaces,
      campos (tipo, visibilidad, `final`, `static`), constructores, métodos, líneas de
      declaración.
   3. **Verifica el bytecode** contra la lista permitida ([`SECURITY.md`](SECURITY.md)).
      Si hay un uso prohibido, devuelve `status: "rejected"` con la línea.
   4. **Ejecuta** el `main` en una JVM hija bajo JDI, registrando la traza.
   5. Devuelve `status: "completed" | "runtime_error" | "timeout" | "limit_exceeded"`,
      la estructura, la traza y la salida estándar (truncada).
5. **api** evalúa las verificaciones del desafío sobre estructura + traza y arma el
   **resultado**: lista de piezas con estado, mensajes de bitácora y escena.
6. **web** muestra la vista de resultado.

Tiempo total objetivo: p95 < 4 s.

## 5. Contratos

Fuente de verdad: JSON Schemas en `packages/contracts/`. Los tipos de TypeScript se
**generan** desde ahí; los `record` de Java se validan contra los mismos schemas en tests.

### 5.1 API pública (`/api/v1`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/modules` | Módulos y desafíos (metadatos, sin soluciones) |
| `GET` | `/challenges/{id}` | Desafío completo: pedido, código base, escena real, pistas bloqueadas |
| `POST` | `/runs` | Ejecuta y devuelve el resultado (síncrono) |
| `GET` | `/challenges/{id}/hints/{level}` | Pista de nivel 1..3 |
| `GET` | `/challenges/{id}/solution` | Solución explicada |
| `GET` | `/health` | Estado del servicio |

- Errores con formato `application/problem+json` (RFC 9457).
- Pistas y solución se sirven por separado para que la interfaz las muestre recién cuando
  corresponde. No es un control de seguridad: el contenido es abierto.

### 5.2 Traza (runner → api)

```jsonc
{
  "status": "completed",
  "structure": { "classes": [ /* nombre, superclase, campos, constructores, métodos, líneas */ ] },
  "stdout": "…",
  "steps": [
    {
      "index": 0,
      "file": "Main.java",
      "line": 5,
      "event": "object_created",          // object_created | field_set | local_set | call | return | output | exception
      "object": { "id": "o1", "type": "FacultadRegional" },
      "frames": [ { "method": "Main.main", "locals": { "fr": { "ref": "o1" } } } ]
    },
    {
      "index": 1,
      "file": "FacultadRegional.java",
      "line": 9,
      "event": "field_set",
      "target": "o1",
      "field": "nombre",
      "value": { "string": "Resistencia" }
    }
  ],
  "heap": { "o1": { "type": "FacultadRegional", "fields": { "nombre": { "string": "Resistencia" }, "decano": { "null": true } } } },
  "exception": null,
  "limits": { "steps": 312, "truncated": false }
}
```

- Valores: `{ "int": 3 }`, `{ "double": 1.5 }`, `{ "boolean": true }`, `{ "char": "a" }`,
  `{ "string": "…" }`, `{ "ref": "o1" }`, `{ "null": true }`.
- `String` y los wrappers se tratan como **valores** (son inmutables); el resto de los
  objetos, como referencias.
- `heap` es el estado final de los objetos alcanzables desde clases del alumno; cada paso
  incluye solo los cambios.
- Solo se registran eventos en clases del alumno.

### 5.3 Desafío (`content/challenges/<module>/<NN-slug>/`)

```
challenge.yaml     # metadatos, pedido, verificaciones, escena, pistas
starter/*.java     # código base
solution/*.java    # solución de referencia
solution.md        # explicación de la solución
```

El formato completo de `challenge.yaml` está en [`specs/challenge-format.md`](specs/challenge-format.md).

### 5.4 Resultado (api → web)

```jsonc
{
  "runId": "…",
  "outcome": "incomplete",              // passed | incomplete | failed
  "pieces": [
    { "id": "fr-resistencia", "archetype": "regional-faculty", "state": "incomplete",
      "label": "FR Resistencia", "sourceRef": { "file": "Main.java", "line": 5 },
      "slots": { "dean": { "state": "missing" } } }
  ],
  "log": [
    { "state": "incomplete", "title": "La FR Resistencia está sin decano.",
      "why": "…", "sourceRef": { "file": "Main.java", "line": 5 }, "hint": "…", "pieceId": "fr-resistencia" }
  ],
  "timeline": [ /* pasos con referencia a piezas y líneas */ ],
  "stdout": "…"
}
```

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
bitácora para cada resultado y qué línea se resalta. Agregar un tipo nuevo requiere un ADR.

## 7. Escena

- Cada desafío define **bindings**: qué tipo del alumno se dibuja con qué **arquetipo**
  (`FacultadRegional → regional-faculty`), qué campo es el cartel y qué campos son
  **slots** (p. ej. `decano → dean-pedestal`).
- Los arquetipos son modelos **generados por código** en `apps/web/src/scene/archetypes/`
  ([`DESIGN.md`](../DESIGN.md) §B).
- Tipos sin binding se dibujan con el arquetipo genérico `generic-block`.
- La **silueta real** se arma con datos de `content/domain/`.

## 8. Frontend

| Ruta | Vista |
|---|---|
| `/` | Inicio: módulos y progreso |
| `/modulos/:moduleId` | Lista de desafíos del módulo |
| `/desafios/:challengeId` | Pedido de obra + editor |
| `/desafios/:challengeId/resultado` | Vista de resultado del último intento |

- **Progreso**: `localStorage`, clave versionada `ljbu.progress.v1`; lectura y escritura
  siempre en `try/catch` (la app funciona sin almacenamiento).
- El código en edición se guarda por desafío para no perderlo al recargar.
- Todos los textos de interfaz salen de `content/i18n/<lang>.json`; ningún texto visible
  queda escrito en componentes.

## 9. Configuración

Toda la configuración por variables de entorno; ningún secreto en el repo.

| Variable | Servicio | Uso |
|---|---|---|
| `RUNNER_URL` | api | URL privada del runner |
| `RUNNER_TOKEN` | api, runner | Autenticación api → runner |
| `ALLOWED_ORIGINS` | api | CORS |
| `RATE_LIMIT_RUNS_PER_MINUTE` | api | Por IP (default 10) |
| `VITE_API_URL` | web | URL pública de la api |

## 10. Observabilidad

- Logs estructurados (JSON) con `runId`, duración por etapa y `status`.
- **Nunca** se registra el código del alumno ni su salida.
- `GET /health` en api y runner para Railway.

## 11. Calidad

Nivel **intermedio** ([ADR 0010](adr/0010-calidad-intermedia.md)):

- Tests unitarios en los tres componentes; tests de integración api ↔ runner.
- **Tests de contenido**: en CI, por cada desafío, la `solution/` pasa todas las
  verificaciones y el `starter/` no.
- GitHub Actions en cada PR: formato, lint, typecheck, tests y build.
- Sin tests end-to-end en la v1.
