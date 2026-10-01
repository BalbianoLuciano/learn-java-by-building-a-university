# ADR 0006 · React, TypeScript y React Three Fiber

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

La interfaz combina paneles, editor de código y una escena 3D sincronizada con la bitácora.

## Decisión

React 19 + TypeScript strict + Vite; Three.js mediante React Three Fiber y drei; Monaco como editor; Zustand para estado; i18next para textos.

## Consecuencias

- Escena declarativa: las piezas son componentes que reciben el resultado de la api.
- Ecosistema grande y documentado.
- Monaco y Three.js pesan: la escena y el editor se cargan de forma diferida.
