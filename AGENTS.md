# AGENTS.md

Guidelines for AI coding agents (and humans) implementing this project. This project uses
**Spec-Driven Development**: the specs are the source of truth and code follows them.

## Read before writing code

| If you touch… | Read first |
|---|---|
| Anything | `docs/PRODUCT.md`, `docs/ROADMAP.md` (current milestone) |
| `services/runner` | `docs/ARCHITECTURE.md` §4–5, `docs/SECURITY.md` (all of it) |
| `services/api` | `docs/ARCHITECTURE.md`, `docs/FEEDBACK.md`, `docs/specs/challenge-format.md` |
| `apps/web` | `DESIGN.md`, `docs/ARCHITECTURE.md` §8, `docs/FEEDBACK.md` |
| `content/` | `docs/DOMAIN.md`, `docs/CURRICULUM.md`, `docs/FEEDBACK.md`, `docs/specs/challenge-format.md` |
| `packages/contracts` | `docs/ARCHITECTURE.md` §5 |

Decisions and their reasons live in `docs/adr/`. Do not contradict an accepted ADR; if a
change requires it, write a new ADR in the same PR.

## Workflow

1. Work only on the current milestone in `docs/ROADMAP.md`.
2. If a spec is ambiguous or wrong, **update the spec first** (same PR), then the code.
3. Keep PRs small and focused; one milestone may take several PRs.
4. Every PR: formatting, lint, typecheck and tests pass locally before pushing.

## Commands

Run the checks of every component you touched; CI runs the same ones.

| Component | Check | Fix formatting |
|---|---|---|
| `apps/web` | `pnpm --filter @ljbu/web check` | `pnpm --filter @ljbu/web format` |
| `packages/contracts` | `pnpm --filter @ljbu/contracts check` | `pnpm --filter @ljbu/contracts format` |
| `services/api`, `services/runner` | `./mvnw verify` (from the service directory) | `./mvnw spotless:apply` |

- After changing a schema in `packages/contracts`, run
  `pnpm --filter @ljbu/contracts generate` and commit `src/generated/`.
- Without a local JDK, run Maven inside Docker:
  `docker run --rm -v "$PWD":/work -w /work eclipse-temurin:25-jdk ./mvnw verify`.
- `docker compose up --build` starts web, api and runner.

## Language policy (ADR 0009)

- Code, identifiers, API, commit messages, code comments: **English**.
- User-facing text: **Spanish** (rioplatense, voseo), always through i18n files or
  `content/` — never hard-coded in components or Java classes.
- Java written by learners in challenges (`starter/`, `solution/`): **Spanish** domain names
  (`FacultadRegional`, `Rector`).
- Project docs: Spanish.

## Code conventions

**Java (api, runner)**
- Java 25, Spring Boot 4, Maven wrapper (`./mvnw`).
- Format with Spotless (google-java-format). Packages: `io.github.balbianoluciano.ljbu.<service>`.
- Prefer `record` for DTOs and immutable data; constructor injection; no field injection.
- Runner execution logic must not depend on Spring.
- Tests: JUnit 5 + AssertJ. Name tests by behavior (`rejectsReflectionCalls`).

**TypeScript (web)**
- `strict: true`; no `any` without a justified comment.
- Function components and hooks; Zustand for shared state.
- Types for API data are generated from `packages/contracts`; never hand-written.
- Styles only through CSS variables defined from `DESIGN.md`; no raw hex or font names.
- Tests: Vitest + Testing Library.

**Commits:** Conventional Commits (`feat(runner): …`, `fix(web): …`, `docs: …`).

## Non-negotiables

- **Never** execute learner code outside `services/runner`'s child JVM.
- **Never** widen the bytecode allowlist without an ADR and new security tests.
- **Never** log learner code or its output.
- **Never** state something about Java or the UTN that is not true (see `docs/FEEDBACK.md`
  §3 and `docs/DOMAIN.md` §1). Every UTN fact in content cites a rule `R-…`.
- **Never** use real people's names in content.
- No secrets in the repository; configuration via environment variables.

## Definition of done

- Acceptance criteria of the milestone item met.
- Tests added or updated; CI green.
- Specs and docs updated if behavior changed.
- For content: the "ready" checklist in `docs/CURRICULUM.md` is complete.
