# ADR 0013 · Progreso local sin cuentas

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Las cuentas suman base de datos, autenticación y datos personales. La v1 busca cero fricción.

## Decisión

El progreso y el código en edición se guardan en `localStorage` (clave versionada). No hay cuentas ni base de datos.

## Consecuencias

- No se guardan datos personales en el servidor.
- El progreso no se sincroniza entre dispositivos.
- La app funciona aunque el almacenamiento falle.
