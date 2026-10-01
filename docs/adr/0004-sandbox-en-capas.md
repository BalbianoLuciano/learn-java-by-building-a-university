# ADR 0004 · Sandbox en capas

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

El `SecurityManager` está deshabilitado de forma permanente desde Java 24 y Railway no permite crear un contenedor por ejecución.

## Decisión

Aislamiento en cinco capas: validación de entrada, compilación controlada, lista permitida de bytecode con `java.lang.classfile`, JVM hija con límites y runner sin dominio público ni secretos. Detalle en `docs/SECURITY.md`.

## Consecuencias

- Funciona en Railway o en cualquier contenedor sin privilegios.
- Riesgo residual ante fallas de la JVM; aceptable sin datos sensibles. Mitigación futura: VPS con gVisor.
- Toda ampliación de la lista permitida requiere ADR y tests de seguridad.
