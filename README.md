# Learn Java by Building a University

**Aprendé Java y Programación Orientada a Objetos modelando la UTN real.**
Escribís código Java de verdad, lo ejecutás y ves en una maqueta 3D isométrica qué construyó
tu programa, qué le falta y por qué algo falló, con cada explicación ligada a tus líneas de
código.

> 🚧 **Estado:** beta. Los 20 desafíos de los cuatro módulos están publicados en
> **https://ljbu.balbiano06.workers.dev** y se resuelven de punta a punta: editor, ejecución,
> bitácora con la explicación de cada resultado y maqueta 3D con reproducción paso a paso.
> Falta la beta con estudiantes ([roadmap](docs/ROADMAP.md)).

*English: an open source course to learn Java OOP by modeling the real structure of the
Universidad Tecnológica Nacional (Argentina). Write real Java, run it, and see what your
code built in an isometric 3D model, with feedback tied to your lines of code. Content is
in Spanish for now.*

## Cómo funciona

1. Leés un **pedido de obra**: "Modelá la Facultad Regional Resistencia con su decano".
2. Escribís Java en el editor.
3. Apretás **Ejecutar**: tu código se compila y se ejecuta en un entorno aislado.
4. Ves el resultado:
   - **Maqueta 3D:** cada pieza aparece como ✅ correcta, 🚧 incompleta o ❌ fallida, junto a
     la silueta de cómo es en la UTN real.
   - **Bitácora:** qué pasó, por qué, en qué línea y una pista.
   - **Línea de tiempo:** la construcción paso a paso, línea por línea.

## Qué vas a aprender

| Módulo | Temas | Ejemplos de la UTN real |
|---|---|---|
| 1 | Clases, objetos y referencias | Las 30 Facultades Regionales, un único Rectorado |
| 2 | Constructores y encapsulamiento | Requisitos para ser Rector o Decano, mandatos de 4 años |
| 3 | Herencia y composición | Unidades académicas, departamentos, por qué un Decano **no** hereda de Docente |
| 4 | Polimorfismo e interfaces | Órganos de gobierno, el Consejo Superior, quién elige a quién |

Todo el contenido se basa en el Estatuto Universitario de la UTN y fuentes oficiales
([dominio](docs/DOMAIN.md)). Es un proyecto independiente: **no es un sitio oficial de la
UTN**.

## Documentación

| Documento | Contenido |
|---|---|
| [Producto](docs/PRODUCT.md) | Visión, público, alcance |
| [Dominio](docs/DOMAIN.md) | El modelo de la UTN real y sus fuentes |
| [Currículo](docs/CURRICULUM.md) | Módulos y desafíos |
| [Feedback](docs/FEEDBACK.md) | Cómo se explican los errores y aciertos |
| [Diseño](DESIGN.md) | Contrato visual: interfaz y escena 3D |
| [Arquitectura](docs/ARCHITECTURE.md) | Componentes, contratos, flujo |
| [Seguridad](docs/SECURITY.md) | Cómo se ejecuta código ajeno de forma segura |
| [Formato de desafíos](docs/specs/challenge-format.md) | Cómo se escribe un desafío |
| [Decisiones (ADR)](docs/adr/) | Por qué se eligió cada cosa |
| [Roadmap](docs/ROADMAP.md) | Hitos |
| [Guía para agentes](AGENTS.md) | Reglas para implementar siguiendo las specs |

## Stack

Java 25 · Spring Boot 4 · JDI · React · TypeScript · Vite · Three.js (React Three Fiber) ·
Monaco · Railway · Cloudflare Pages.

## Desarrollo local

Con Docker alcanza para levantar todo:

```bash
docker compose up --build
```

| Qué | Dónde |
|---|---|
| Web | <http://localhost:5173> |
| Api | <http://localhost:8080/api/v1/modules> |
| Runner | sin puerto publicado, igual que en producción: `docker compose ps` muestra su estado |

Para trabajar en un componente hacen falta Node 24 con pnpm (web y contratos) y un JDK 25
(api y runner). Los comandos de cada uno están en [CONTRIBUTING.md](CONTRIBUTING.md).

## Contribuir

Las contribuciones son bienvenidas, sobre todo desafíos nuevos y correcciones de los
mensajes de feedback. Leé [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencia

- Código: [MIT](LICENSE).
- Contenido educativo (`content/`, `docs/`): [CC BY-SA 4.0](LICENSE-CONTENT.md).

## Agradecimientos

Inspirado en la cursada de Programación II de la UTN y en el
[curso de Java de Facundo Uferer](https://facundouferer.ar/cursos/java/).
