# ADR 0001 · Registrar decisiones de arquitectura

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

El proyecto se construye con Spec-Driven Development y lo pueden continuar otras personas o agentes. Las decisiones importantes tienen que poder entenderse sin el contexto de la conversación en que se tomaron.

## Decisión

Cada decisión que afecte la arquitectura, el alcance, la seguridad o el contenido se registra como ADR en `docs/adr/NNNN-titulo.md` con Contexto, Decisión y Consecuencias. Un ADR no se edita después de aceptado: se reemplaza con uno nuevo que lo marque como `reemplazada por NNNN`.

## Consecuencias

- Toda propuesta que contradiga un ADR debe incluir un ADR nuevo.
- Las specs (`docs/*.md`) describen el estado actual; los ADR explican por qué se llegó ahí.
