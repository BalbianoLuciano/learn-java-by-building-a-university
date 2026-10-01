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
3. Antes de abrir el PR, corré formato, lint y tests del componente que tocaste.
4. Abrí el PR contra `main` completando la plantilla.

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
