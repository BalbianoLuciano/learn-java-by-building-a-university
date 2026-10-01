# ADR 0002 · Monorepo

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Hay un frontend, dos servicios Java, contratos compartidos y contenido. Los contratos (traza, resultado, desafío) cambian a la vez en varios componentes.

## Decisión

Un único repositorio con `apps/`, `services/`, `packages/`, `content/` y `docs/`. Specs, contratos y código cambian juntos en el mismo PR.

## Consecuencias

- CI con un job por componente, filtrado por rutas modificadas.
- Herramientas de cada ecosistema por separado (Maven para Java, pnpm para web); sin orquestador de monorepo en la v1.
