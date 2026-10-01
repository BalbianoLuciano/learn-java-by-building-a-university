# ADR 0005 · Spring Boot 4 y Java 25

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

El autor quiere sumar Java al stack con tecnologías del mercado laboral. Java 25 es la LTS vigente y trae la API `java.lang.classfile` estable.

## Decisión

api y runner con Spring Boot 4.x sobre Java 25, construidos con Maven (wrapper incluido).

## Consecuencias

- Mismo framework en los dos servicios: un solo modelo mental.
- El runner usa Spring solo para HTTP y configuración; la lógica de ejecución no depende de Spring y se testea sin levantar el contexto.
