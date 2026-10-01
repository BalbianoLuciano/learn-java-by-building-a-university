# ADR 0014 · El dominio es la UTN real

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

El curso podría usar una universidad inventada, pero el autor pidió explicar Java a partir de la estructura actual de la UTN.

## Decisión

El dominio de todos los desafíos es la estructura real de la UTN, con fuentes (`docs/DOMAIN.md`). Instituciones y reglas reales; personas ficticias.

## Consecuencias

- Cada regla del contenido cita su fuente.
- Las relaciones reales guían el modelado (p. ej. los cargos son composición: ver `DOMAIN.md` §5.1).
- Hay que revisar las fuentes una vez por año lectivo.
