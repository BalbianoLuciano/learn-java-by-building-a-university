# ADR 0008 · Railway y Cloudflare Pages

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Se necesita publicar dos servicios Java y un frontend estático con poco mantenimiento y bajo costo.

## Decisión

api y runner en Railway (el runner solo en red privada); frontend en Cloudflare Pages. Todo se empaqueta con Docker para poder migrar.

## Consecuencias

- Costo inicial bajo (plan Hobby de Railway).
- Sin aislamiento por contenedor por ejecución (cubierto por ADR 0004).
- Camino de migración: VPS con Coolify o Dokploy (open source) y gVisor, sin cambiar el código.
