# ADR 0010 · Nivel de calidad intermedio

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Se busca calidad desde el inicio sin frenar la primera versión.

## Decisión

Tests unitarios (JUnit 5, Vitest), tests de integración api ↔ runner, tests de contenido y suite de seguridad; formato y lint obligatorios; CI en cada PR. Sin tests end-to-end en la v1.

## Consecuencias

- La regresión visual de la escena se revisa a mano.
- Se reevalúa sumar Playwright antes de la v1.0.
