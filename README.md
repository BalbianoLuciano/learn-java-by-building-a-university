<div align="center">

<img src="docs/assets/readme/logo.svg" alt="" width="88" height="88">

# Learn Java by Building a University

**Learn Java and Object-Oriented Programming by modeling a real university.**<br>
Write real Java, run it, and see what your code built in a 3D model.

[**▶ Try it at ljbu.balbiano06.workers.dev**](https://ljbu.balbiano06.workers.dev)

[![CI](https://github.com/BalbianoLuciano/learn-java-by-building-a-university/actions/workflows/ci.yml/badge.svg)](https://github.com/BalbianoLuciano/learn-java-by-building-a-university/actions/workflows/ci.yml)
![Java 25](https://img.shields.io/badge/Java-25-E76F00?logo=openjdk&logoColor=white)
![Spring Boot 4](https://img.shields.io/badge/Spring_Boot-4-6DB33F?logo=springboot&logoColor=white)
![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Three.js](https://img.shields.io/badge/Three.js-React_Three_Fiber-000000?logo=threedotjs&logoColor=white)
[![Code: MIT](https://img.shields.io/badge/code-MIT-blue)](LICENSE)
[![Content: CC BY-SA 4.0](https://img.shields.io/badge/content-CC_BY--SA_4.0-lightgrey)](LICENSE-CONTENT.md)

</div>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/readme/hero-dark.png">
  <img src="docs/assets/readme/hero-light.png" alt="Home page: the title on the left and, on the right, the 3D model with the Rectorado and three Facultades Regionales being built, with the attribute bubble of FR Resistencia open.">
</picture>

> 🚧 **Beta.** All 20 challenges of the four modules are live and can be solved end to end.
> The course content is in **Spanish**, since it is built around the Universidad Tecnológica
> Nacional (UTN), Argentina. If something is confusing or broken,
> [open an issue](https://github.com/BalbianoLuciano/learn-java-by-building-a-university/issues/new).

---

## How it works

<table>
<tr>
<td width="33%" valign="top">

### 1 · Read the brief
Each challenge is a **work order** about the real UTN: *"Declare a variable `miFacultad` that
points to the same faculty and set its province through it"*.

</td>
<td width="33%" valign="top">

### 2 · Write Java
In the editor, with as many classes as you need. Hit **Run**: the code is compiled and
executed on a server, in a sandbox. Nothing to install.

</td>
<td width="33%" valign="top">

### 3 · See what it built
The **model** shows your classes and your objects; the **log** explains what happened, why
and on which line; the **timeline** replays it step by step.

</td>
</tr>
</table>

Everything on one screen: the code on the left, the model and the log on the right.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/readme/workbench-dark.png">
  <img src="docs/assets/readme/workbench-light.png" alt="A challenge: the editor with Main.java on the left; on the top right the model with FR Resistencia, its two tags resistencia and miFacultad, and the FacultadRegional blueprint behind it; below, the log with the passed result, the analogy and the checks.">
</picture>

### The run, step by step

The timeline walks through the program: the line is highlighted in the editor, the object the
step touches opens its bubble with its attributes, and the conduit from its blueprint lights
up while the constructor runs.

<p align="center">
  <img src="docs/assets/readme/replay.gif" alt="Animation: the run of challenge 1.3 replayed step by step; FR Resistencia is created, its attributes are set, and a second tag, miFacultad, hangs from the same building." width="900">
</p>

## What Java looks like

The idea comes from Flexbox Froggy: **every concept of the language has one shape, always the
same, and the shape looks like what it means.**

<table>
<tr>
<td width="55%" valign="top">

| In Java | In the model |
|---|---|
| Class | **Blueprint** on the drafting table, with a miniature of what it builds |
| Object | **Building** plugged into its blueprint, with its plate `Class #1`, `#2`… |
| Variable | **Tag** hanging from the object it points to; on the ground if it is `null` |
| Attributes and methods | **Bubble** when you click the building; `private` with a lock, `static` with a flag |
| Composition | **Attached island**: what an object has lives next to it |
| Inheritance | **Floors**: the ground floor is the superclass |
| Interface | **Seal** that classes sign |
| What is missing | **Silhouette** and scaffolding |

</td>
<td width="45%" valign="top">

<img src="docs/assets/readme/bubble.png" alt="Detail of the model of challenge 2.5: the bubble of FR Resistencia with its private attributes nombre and decano (pointing to object #1) and its methods; below, buildings with their tags interino, tomas, resistencia and lucia.">

</td>
</tr>
</table>

When something goes wrong, besides the technical message there is an **analogy** (the same
idea, explained without code) and links to the **official Java documentation** (Oracle's Java
Tutorials, the JLS and the API) to dig deeper.

## What you will learn

<img src="docs/assets/readme/modules.png" alt="The four modules as cards, each with a miniature of its final challenge: classes, objects and references; constructors and encapsulation; inheritance and composition; polymorphism and interfaces.">

| Module | Topics | With the real UTN |
|---|---|---|
| **1** · Classes, objects and references | classes, `new`, state, aliasing, `==`, arrays | the 30 regional faculties and a single Rectorado |
| **2** · Constructors and encapsulation | constructors, `this`, `private`, validation, `static final`, overloading | the requirements to be Rector or Dean, 4-year terms |
| **3** · Inheritance and composition | `extends`, `super()`, lists, composition over inheritance | academic units, departments, why a Dean does **not** extend Professor |
| **4** · Polymorphism and interfaces | abstract classes, `@Override`, dynamic dispatch, interfaces | the governing bodies, the Consejo Superior, who elects whom |

Each module is a path of five challenges that ends in a capstone.

<p align="center">
  <img src="docs/assets/readme/module-route.png" alt="Module 3 page: the title, the goal and what you will learn on the left; on the right the path of five numbered challenges, each with its miniature." width="900">
</p>

## The real UTN, not a made-up one

The 30 regional faculties, the Rectorado, the councils, the deans and their requirements come
from the **University Statute** (Res. AU 1/2011) and from UTN's official pages; every challenge
cites the rule it uses and every fact has a source ([domain](docs/DOMAIN.md)). The people in
the challenges, on the other hand, are fictional.

> An independent, open source project: **not an official UTN site**.

## How it is built

```mermaid
flowchart LR
  web["<b>web</b><br/>React · Monaco · Three.js<br/><i>Cloudflare</i>"]
  api["<b>api</b><br/>Spring Boot<br/>challenges · checks · feedback<br/><i>Railway</i>"]
  runner["<b>runner</b><br/>in-memory javac · bytecode allowlist<br/>child JVM traced with JDI<br/><i>Railway, private network only</i>"]
  web -- "learner code" --> api
  api -- "full program" --> runner
  runner -- "execution trace" --> api
  api -- "model · log · timeline" --> web
```

- The **runner** compiles the code in memory, checks the bytecode against an allowlist and runs
  it in a separate JVM with time, memory and step limits, recording every step with JDI. Since
  Java 24 there is no `SecurityManager`: isolation comes in layers ([security](docs/SECURITY.md)).
- The **api** never runs code: it evaluates **declarative checks** written in each
  `challenge.yaml` against the structure and the trace, and builds the log and the model.
- The **web** draws the model with code-generated shapes, no external 3D files.
- The contracts between the three parts are JSON Schemas in `packages/contracts`.

**Stack:** Java 25 · Spring Boot 4 · JDI · React 19 · TypeScript · Vite · React Three Fiber ·
Monaco · Zustand · pnpm · Docker · Railway · Cloudflare.

## Local development

Docker is enough to run everything:

```bash
docker compose up --build
```

| What | Where |
|---|---|
| Web | <http://localhost:5173> |
| Api | <http://localhost:8080/api/v1/modules> |
| Runner | no published port, same as in production |

To work on a component you need Node 24 with pnpm (web and contracts) and a JDK 25 (api and
runner). The commands are in [CONTRIBUTING.md](CONTRIBUTING.md); deployment, in
[DEPLOY.md](docs/DEPLOY.md).

<details>
<summary><b>Project documentation</b> (in Spanish)</summary>

The project follows Spec-Driven Development: the specs lead and the code follows them.

| Document | Contents |
|---|---|
| [Product](docs/PRODUCT.md) | Vision, audience, scope |
| [Domain](docs/DOMAIN.md) | The model of the real UTN and its sources |
| [Curriculum](docs/CURRICULUM.md) | Modules and challenges |
| [Feedback](docs/FEEDBACK.md) | How errors and successes are explained |
| [Design](DESIGN.md) | Visual contract: interface and 3D scene |
| [Architecture](docs/ARCHITECTURE.md) | Components, contracts, flow |
| [Security](docs/SECURITY.md) | How untrusted code is run safely |
| [Challenge format](docs/specs/challenge-format.md) | How a challenge is written |
| [Deploy](docs/DEPLOY.md) | Railway, Cloudflare and how to operate them |
| [Decisions (ADR)](docs/adr/) | Why each thing was chosen |
| [Roadmap](docs/ROADMAP.md) | Milestones |
| [Agent guide](AGENTS.md) | Rules to implement following the specs |

</details>

## Contributing

Contributions are welcome, especially **new challenges** and **fixes to the feedback
messages**: if a message confused you, that is already a bug. Read
[CONTRIBUTING.md](CONTRIBUTING.md).

<p align="center">
  <img src="docs/assets/readme/not-found.png" alt="404 page: a broken faculty with a fallen brick and an empty signpost, next to the text NullPointerException: the address points to null." width="720">
  <br><sub>And if you get lost, there is a 404 to match.</sub>
</p>

## License

- Code: [MIT](LICENSE).
- Educational content (`content/`, `docs/`): [CC BY-SA 4.0](LICENSE-CONTENT.md).

## Acknowledgements

Inspired by the Programación II course at UTN, by
[Flexbox Froggy](https://flexboxfroggy.com/) and by
[Facundo Uferer's Java course](https://facundouferer.ar/cursos/java/).
