# Política de seguridad

Este proyecto ejecuta código enviado por usuarios, así que los reportes de seguridad son
muy valiosos.

## Cómo reportar una vulnerabilidad

**No abras un issue público.** Usá el reporte privado de GitHub:
[Security → Report a vulnerability](https://github.com/BalbianoLuciano/learn-java-by-building-a-university/security/advisories/new).

Incluí, si podés: qué componente afecta, pasos para reproducirlo y el impacto que ves.
No hace falta un exploit completo.

## Qué esperar

- Acuse de recibo en un máximo de 7 días.
- Te avisamos cuando esté corregido y, si querés, te damos crédito en el aviso.

## Alcance

Especialmente: escapes del sandbox, formas de saltear la lista permitida de bytecode,
agotamiento de recursos que el runner no corte, y acceso del runner a la api u otros
servicios. El diseño del sandbox está en [`docs/SECURITY.md`](docs/SECURITY.md).

Por favor, no pruebes contra el entorno de producción con ataques de denegación de
servicio: levantá el proyecto en local.
