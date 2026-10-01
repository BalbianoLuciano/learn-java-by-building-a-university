# ADR 0003 · Ejecución en servidor con JDI

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Hay que ejecutar Java real y registrar paso a paso qué objetos se crean y cómo cambian. Alternativas evaluadas: ejecutar en el navegador (CheerpJ, TeaVM), instrumentar el bytecode, o analizar solo el estado final.

## Decisión

El código se compila y ejecuta en un servicio Java (`services/runner`) que lanza una JVM hija y la observa con JDI (Java Debug Interface), registrando eventos solo en las clases del alumno.

## Consecuencias

- Se registra cualquier programa sin modificarlo; la traza es fiel a lo que hizo la JVM.
- Requiere servidor y aislamiento (ADR 0004).
- JDI suma latencia: se acota con límites de pasos y filtros de clases.
- Suma Java real al stack del autor, objetivo explícito del proyecto.
