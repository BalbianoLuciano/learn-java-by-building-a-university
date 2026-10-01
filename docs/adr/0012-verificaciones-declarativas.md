# ADR 0012 · Verificaciones declarativas

- **Estado:** aceptada
- **Fecha:** 2026-10-01

## Contexto

Hay que decidir si un programa cumple un pedido de obra sin volver a ejecutar código fuera del sandbox.

## Decisión

Las verificaciones se declaran en `challenge.yaml` con tipos cerrados (ver `ARCHITECTURE.md` §6) y se evalúan en la api sobre la estructura y la traza.

## Consecuencias

- Quien escribe contenido no programa verificaciones: las declara.
- Un tipo nuevo de verificación requiere ADR, implementación y tests.
- Casos muy específicos pueden no poder expresarse; se resuelven sumando un tipo, no con código ad hoc.
