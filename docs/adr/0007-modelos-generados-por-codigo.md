# ADR 0007 · Modelos 3D generados por código

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Se busca un estilo low-poly pastel consistente sin iterar en herramientas de modelado.

## Decisión

Todas las piezas se generan con geometría básica desde un catálogo de arquetipos en `apps/web/src/scene/archetypes/`. Sin archivos de modelos externos en la v1.

## Consecuencias

- Consistencia visual garantizada por los tokens de `DESIGN.md`.
- Las piezas son parametrizables (pisos, carteles, slots) a partir de la traza.
- Menos detalle que modelos hechos a mano, a propósito.
