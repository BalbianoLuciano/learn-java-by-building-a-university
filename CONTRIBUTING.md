# Cómo contribuir

¡Gracias por querer sumar! Este proyecto se construye con **Spec-Driven Development**: las
especificaciones de `docs/` mandan y el código las sigue.

## Formas de contribuir

| Tipo | Por dónde empezar |
|---|---|
| Un mensaje de feedback confuso o incorrecto | Issue con la plantilla "Mensaje confuso" |
| Un error en un dato de la UTN | Issue con la fuente oficial que lo corrige |
| Un desafío nuevo | Issue "Propuesta de desafío" antes de escribirlo |
| Código | Elegí un ítem del hito actual en [`docs/ROADMAP.md`](docs/ROADMAP.md) |
| Un problema de seguridad | **No** abras un issue: ver [`SECURITY.md`](SECURITY.md) |

## Antes de programar

1. Leé [`AGENTS.md`](AGENTS.md): vale para personas y agentes.
2. Si tu cambio contradice una spec o un ADR, proponé primero el cambio de spec (o un ADR
   nuevo) en el mismo PR.

## Flujo

1. Hacé un fork y creá una rama: `feat/runner-allowlist`, `fix/web-timeline`,
   `content/m1-03`.
2. Commits con [Conventional Commits](https://www.conventionalcommits.org/es/) en inglés:
   `feat(api): evaluate shared_reference checks`.
3. Antes de abrir el PR, corré formato, lint y tests del componente que tocaste (ver
   [Comandos](#comandos)).
4. Abrí el PR contra `main` completando la plantilla.

## Comandos

CI corre lo mismo en cada PR, un job por componente y solo si cambió.

| Componente | Requisitos | Antes del PR |
|---|---|---|
| `apps/web` | Node 24, pnpm | `pnpm --filter @ljbu/web check` |
| `packages/contracts` | Node 24, pnpm | `pnpm --filter @ljbu/contracts check` |
| `services/api`, `services/runner` | JDK 25 | `./mvnw verify` (desde la carpeta del servicio) |

- Instalación de la parte JS: `pnpm install` en la raíz.
- Arreglar el formato: `pnpm format` (JS) y `./mvnw spotless:apply` (Java).
- Si cambiás un schema de `packages/contracts`, regenerá los tipos con
  `pnpm --filter @ljbu/contracts generate` y commitealos.
- Servidor de desarrollo de la web: `pnpm --filter @ljbu/web dev`.
- Sin JDK instalado, los comandos de Maven corren igual dentro de Docker. Desde la raíz del
  repo (los tests del runner leen los schemas de `packages/contracts`):
  `docker run --rm -v "$PWD":/repo -w /repo/services/runner eclipse-temurin:25-jdk ./mvnw verify`.
- El runner no arranca sin `RUNNER_TOKEN`; para levantarlo solo:
  `RUNNER_TOKEN=local ./mvnw spring-boot:run`.
- Si cambia la traza que produce el runner, el ejemplo del contrato se regenera con
  `./mvnw test -Dtest=ContractExampleTest -Dljbu.updateExamples=true`.
- Todo junto: `docker compose up --build`.

## Idiomas

- Código, commits y comentarios: inglés.
- Textos para el alumno y documentación: español (voseo).
- Java de los desafíos: identificadores en español.

Detalle en [ADR 0009](docs/adr/0009-politica-de-idiomas.md).

## Contenido

Un desafío nuevo tiene que cumplir los criterios de "listo" de
[`docs/CURRICULUM.md`](docs/CURRICULUM.md), respetar el formato de
[`docs/specs/challenge-format.md`](docs/specs/challenge-format.md) y citar reglas reales
de [`docs/DOMAIN.md`](docs/DOMAIN.md). Al contribuir contenido aceptás publicarlo bajo
CC BY-SA 4.0; al contribuir código, bajo MIT.

## Código de conducta

Este proyecto sigue el [Código de conducta](CODE_OF_CONDUCT.md).
