# Roadmap

> Hitos en orden. Cada hito termina con un PR mergeado a `main`, CI en verde y sus criterios
> de aceptación cumplidos. Un hito no empieza hasta que el anterior está cerrado, salvo que
> se indique lo contrario.

## M0 · Specs y repositorio ✅

- [x] Repositorio público creado.
- [x] Specs: producto, dominio, currículo, feedback, arquitectura, seguridad, diseño.
- [x] ADRs iniciales, licencias, guías de contribución, `AGENTS.md`.

## M1 · Esqueleto del monorepo y CI ✅

**Objetivo:** las tres apps existen, compilan y CI corre en cada PR.

- [x] `services/api` y `services/runner`: Spring Boot 4, Java 25, Maven wrapper,
  `GET /health`, Spotless, un test.
- [x] `apps/web`: Vite + React + TS strict, ESLint, Prettier, Vitest, una página con tokens
  de `DESIGN.md` y modo claro/oscuro.
- [x] `packages/contracts`: schemas de traza, resultado y desafío; generación de tipos TS.
- [x] GitHub Actions: job por componente con formato, lint, tests y build.
- [x] `docker-compose.yml` para levantar todo en local.

**Aceptación:** `docker compose up` levanta las tres apps; un PR con un error de formato
falla en CI.

## M2 · Runner: compilar, verificar, ejecutar, trazar ✅

**Objetivo:** dado código Java, el runner devuelve estructura y traza según
[`ARCHITECTURE.md`](ARCHITECTURE.md) §5.2.

- [x] Compilación en memoria con diagnósticos.
- [x] Extracción de estructura con `java.lang.classfile`.
- [x] Verificador de lista permitida ([`SECURITY.md`](SECURITY.md) §3).
- [x] JVM hija bajo JDI con límites; traza de creación de objetos, escrituras de campos,
  locales, llamadas, salida y excepciones.
- [x] Suite de seguridad completa (§5 de SECURITY.md).

**Aceptación:** un programa de inventario de ejemplo (escrito para los tests del runner) y
la solución del desafío 1.3 producen trazas que validan contra el schema; toda la suite
maliciosa es rechazada o cortada; p95 < 2 s en local.

## M3 · Api: desafíos, verificaciones y feedback ✅

**Objetivo:** `POST /api/v1/runs` devuelve el resultado completo.

- [x] Carga y validación de `content/` al arrancar (falla si un desafío es inválido).
- [x] Motor de verificaciones (tipos de ARCHITECTURE.md §6) y detección de trampas.
- [x] Catálogos de `javac` y de excepciones ([`FEEDBACK.md`](FEEDBACK.md) §4–5).
- [x] Armado de piezas, bitácora y línea de tiempo.
- [x] Rate limit, CORS, `problem+json`.
- [x] Tests de contenido en CI.

**Aceptación:** los desafíos 1.1–1.3 escritos en formato real pasan sus tests de contenido.

## M4 · Web: editor y bitácora (sin 3D) ✅

**Objetivo:** el flujo completo funciona con una vista de resultado **textual**.

- [x] Inicio, módulo, desafío con pedido de obra y Monaco.
- [x] Vista de resultado con bitácora, chips de línea, mini código y línea de tiempo.
- [x] Pistas y solución.
- [x] Progreso y código en `localStorage`; i18n.

**Aceptación:** se completa el desafío 1.3 de punta a punta solo con teclado; la bitácora
cumple WCAG 2.2 AA.

> Hacer la bitácora antes que el 3D garantiza que el feedback enseña por sí solo y que la
> maqueta es el complemento, no la única fuente de información.

## M5 · Maqueta 3D ✅

**Objetivo:** la vista de resultado muestra la escena según [`DESIGN.md`](../DESIGN.md)
Parte B.

- Cámara isométrica, islas flotantes, iluminación y sombras de contacto.
- Arquetipos: `regional-faculty`, `rectorate`, `variable-sign`, `reference-link`,
  `generic-block`, `slot`, `inheritance-floors`.
- Estados ✅ 🚧 ❌, silueta real y selección vinculada con la bitácora.
- Reproducción paso a paso sincronizada con la línea de tiempo.
- `prefers-reduced-motion`.

**Aceptación:** 60 fps con la escena del desafío 1.5 en una notebook con gráficos
integrados; clic en pieza ↔ línea funciona en ambos sentidos.

Medido en `/dev/escena` (Chrome, Apple M4): 150 piezas a 58–60 fps; con la CPU limitada a
¼ para simular una notebook floja, 45 piezas a 53–60 fps y 150 piezas a 21–44 fps.
Queda para M6, con el contenido que los usa: las formas propias de `department`, `career`
y `person` (hoy se dibujan como `generic-block`), `interface-badge`, la silueta real desde
`content/domain/` y la animación del cable al dibujarse.

## M6 · Contenido: módulos 1 y 2 ✅

- 10 desafíos completos según [`CURRICULUM.md`](CURRICULUM.md).
- Datos de `content/domain/` (30 FR, órganos, reglas).

**Aceptación:** criterios de "listo" del currículo en los 10 desafíos.

Hecho junto con M8 (los 20 desafíos en un PR), antes del deploy de M7. Queda abierto el
último punto de la lista de "listo": la revisión por una persona además de quien escribió.

## Crítica del núcleo (entre M6/M8 y M7) ✅

Pasada de revisión con Luciano sobre el producto construido, antes del deploy:

- Pantalla única del desafío: código, maqueta y bitácora a la vez; pedido de obra plegable;
  la línea elegida se resalta en el editor; chips `Archivo:línea`.
- Lenguaje visual v2 ([`DESIGN.md`](../DESIGN.md) §B4): planos sobre una mesa, uno por clase,
  con miniatura; grilla de columnas por clase; conductos por el suelo del plano a cada
  instancia, encendidos al construirse; etiquetas colgadas para las variables; chapa de
  serie; globo con atributos y métodos al tocar o al reproducir; composición en islas
  adosadas; leyenda.
- Analogías por resultado y referencias a documentación oficial de Java en cada desafío;
  catálogo de `javac` con los errores de pegar código.
- Landing con la maqueta de fondo, módulo como ruta con miniaturas, 404, logo.

## M7 · Deploy y beta cerrada

- api y runner en Railway (runner solo en red privada); web en Cloudflare Pages.
- Dominio y HTTPS.
- Beta con compañeros de cursada; plantilla de issue "¿Este mensaje te confundió?".

**Aceptación:** ≥ 5 estudiantes completan el módulo 1; p95 de ejecución < 4 s en producción.

## M8 · Contenido: módulos 3 y 4 y lanzamiento v1.0

- 10 desafíos restantes, arquetipos `department`, `career`, `person`, `interface-badge`. ✅
- Correcciones de la beta.

**Aceptación:** los 20 desafíos listos; checklist de accesibilidad completa; release v1.0.

El contenido y los arquetipos están; las correcciones de la beta, la checklist de
accesibilidad y el release dependen de M7.

## Después de la v1

Candidatos, sin compromiso: contenido en inglés, módulo de colecciones, modo libre,
migración del runner a VPS con gVisor, panel para docentes.
