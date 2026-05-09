<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialised) → 1.0.0
Bump rationale: Initial ratification of the cpp-context-staging-bulkscan
                constitution. All principles and sections are new; no
                prior principles to remove or redefine, so MAJOR is the
                correct starting point (1.0.0 per the standard).

Modified principles: N/A (initial ratification).

Added sections:
  - Core Principles
      I.    CQRS Slice Discipline
      II.   Constructor Injection (CDI) & Immutable DTOs
      III.  Maven Module-Boundary Respect
      IV.   Spec-Driven Build Loop
      V.    HMCTS Standards Compliance (Justice Framework)
      VI.   No `System.out` / `System.err` — SLF4J Only
      VII.  No Wildcard Imports
      VIII. Test-Driven Development
      IX.   Azure Functions Trigger Hygiene
  - Technology Stack & Deployment
  - Development Workflow & Quality Gates
  - Governance

Removed sections: None.

Templates requiring updates:
  - .specify/templates/plan-template.md       ✅ compatible — the
      "Constitution Check" block is filled per-feature by /speckit-plan;
      plan authors MUST gate on Principles I–IX.
  - .specify/templates/spec-template.md       ✅ compatible — no
      constitution-specific content.
  - .specify/templates/tasks-template.md      ✅ compatible — task
      ordering already encodes "tests before implementation", aligning
      with Principle VIII.
  - .specify/templates/checklist-template.md  ✅ compatible — no changes.
  - .claude/rules/*.md                        ✅ aligned — encode these
      principles informally; this constitution is now the authoritative
      source.

Follow-up TODOs: None.
-->

# cpp-context-staging-bulkscan Constitution

## Core Principles

### I. CQRS Slice Discipline (NON-NEGOTIABLE)

The service is a CQRS / event-sourced context built on the MOJ Justice
Framework. The write side, the event store, and the read side are
strictly separated:

```
Command JSON  → CommandHandler (@ServiceComponent(COMMAND_HANDLER))
              → Aggregate (only place state-change decisions are made)
              → Domain Event (immutable, JSON-Schema-defined)
              → EventStream
              → EventListener (@ServiceComponent(EVENT_LISTENER))
              → Viewstore JPA repository
              → QueryView (@ServiceComponent(QUERY_VIEW))
```

- Aggregates are the **only** place state-change decisions are made.
- Event listeners may write to the viewstore only — never call command
  handlers, never read from the event store.
- Queries may read viewstore JPA entities only — never touch the event
  store and never import command-side classes.
- `@Handles` message names MUST match the JSON Schema id under
  `stagingbulkscan-json/` / `*-command-api` / `*-event-api`.

**Rationale**: CQRS only delivers its benefits — independent scaling,
auditability, replay, integration with the wider CPP estate's event bus
— if the slice direction is enforced. A single back-channel call from a
listener into a handler collapses the architecture and is impossible to
unwind later.

### II. Constructor Injection (CDI) & Immutable DTOs (NON-NEGOTIABLE)

Dependency injection is CDI (Weld) via `@Inject`. **Spring is forbidden
in this service** — Justice Framework runs on a CDI container and
mixing the two is incompatible.

- Prefer constructor injection: `@Inject` on a constructor with `private
  final` fields.
- Field injection (`@Inject` on the field) is permitted only where it
  matches existing Justice Framework base-class style; new classes
  SHOULD use constructor injection.
- New internal value types SHOULD be Java records.
- Command and event payloads come from JSON-Schema codegen
  (`uk.gov.justice.stagingbulkscan.*`) — these are POJOs and MUST NOT
  be hand-edited. Change the JSON Schema instead.

**Rationale**: constructor injection makes dependencies explicit and
test-friendly without reflection. CDI compatibility keeps the service
runnable inside the Justice Framework's container — Spring would
require a parallel runtime.

### III. Maven Module-Boundary Respect (NON-NEGOTIABLE)

The Maven module graph IS the architecture:

```
azure-core, azure-functions, command (api + handler), event,
event-sources, domain, query, viewstore, service, json,
healthchecks, integration-test, testharness
```

- Cross-module dependencies that violate CQRS direction are forbidden:
  `command-handler` MUST NOT depend on `query`; `query` MUST NOT depend
  on `command-handler`; `event` listeners depend on `viewstore` only.
- Aggregates may live only in `stagingbulkscan-domain`.
- Liquibase migrations may live only in `stagingbulkscan-viewstore`.
- Adding a new cross-module dependency requires explicit justification
  in the plan's "Complexity Tracking" section.

**Rationale**: keeps CQRS direction enforceable at compile time and
prevents the slow drift toward a ball-of-mud layout that has historically
afflicted long-lived CPP contexts.

### IV. Spec-Driven Build Loop (NON-NEGOTIABLE)

Every non-trivial change MUST flow through the cycle:

```
Spec → Write → Code Review → QA → Spec-Validate → Fix → Ship
```

implemented via the `/speckit-*` skills (`specify → clarify → plan →
tasks → implement → analyze`). The reviewer agents (`code-reviewer`,
`qa`, `spec-validator`) report findings only; they MUST NOT modify
code. The primary agent or a human applies fixes, then re-runs the
loop until all three return PASS / COMPLIANT.

The `spec-validator` checks that:
- JSON Schemas under `stagingbulkscan-json/` are well-formed.
- Schema ids match the `@Handles` strings on handlers / listeners.
- Schema changes are backwards-compatible across at least one release,
  or the spec explicitly calls out the breaking change.
- Aggregate `Apply` methods exist for every emitted event.

Changes exempt from the loop are limited to: markdown-only edits,
whitespace/import-ordering edits, README/LICENSE/CODEOWNERS updates,
and `.claude/rules/*` or `CLAUDE.md` rule updates.

**Rationale**: keeps a human (or primary agent) as the decision point;
prevents conflicting auto-fixes; preserves auditable, reproducible
review output.

### V. HMCTS Standards Compliance (NON-NEGOTIABLE)

- Build tool: **Maven** (multi-module). Gradle is forbidden in this
  service.
- Parent POM: `uk.gov.moj.cpp.common:service-parent-pom`. Do not bypass
  it (it carries Checkstyle, PMD, dependency pinning, and CI hooks).
- Microservices framework: MOJ Justice Framework
  (`uk.gov.justice.services.*`).
- Hand-written Java root package: `uk.gov.moj.cpp.stagingbulkscan.*`.
- JSON-Schema-generated Java root package:
  `uk.gov.justice.stagingbulkscan.*` — never hand-edited.
- Logging: SLF4J + Logback, with `correlationId` MDC propagation
  carried through Justice FW envelope metadata.
- PII, case data, defendant identifiers, tokens, passwords, and
  secrets MUST NOT appear in logs.
- Branch flow (jgitflow): `dev/release` master, `main` develop,
  `dev/feature-*` features, `dev/release-*` releases, `dev/hotfix-*`
  hotfixes. PRs target `main`.

**Rationale**: aligns the service with HMCTS security, observability,
and platform conventions for the CPP estate, ensuring it is operable
and auditable inside the wider system.

### VI. No `System.out` / `System.err` — SLF4J Only (NON-NEGOTIABLE)

Code MUST NOT use `System.out.println`, `System.err.println`, or
`Throwable#printStackTrace()`. All diagnostic output goes through SLF4J
(`org.slf4j.Logger` via `LoggerFactory.getLogger(...)` or `@Slf4j` if
Lombok is in scope). This applies to production code AND tests.

Wrap raw payload logs in `if (LOGGER.isDebugEnabled())` and prefer
`JsonEnvelope#toObfuscatedDebugString()` for envelope contents.

**Rationale**: structured logs depend on every log line going through
the SLF4J pipeline; direct stdout/stderr writes bypass MDC
(`correlationId`), severity routing, and log shipping — they vanish
from production observability and surface as noise in test output.

### VII. No Wildcard Imports (NON-NEGOTIABLE)

Java imports MUST be explicit: never `import java.util.*` or similar.
Static imports may be focused (e.g. `import static java.util.UUID.fromString;`)
but MUST NOT use wildcards.

**Rationale**: explicit imports make refactor diffs clearer, surface
unused dependencies, and avoid accidental shadowing when the JDK adds
new classes.

### VIII. Test-Driven Development (NON-NEGOTIABLE)

Red → Green → Refactor for every behaviour change.

1. Write the failing test first. It MUST run and fail for the *correct*
   reason — the assertion, not a `NoClassDefFoundError` / compilation
   error.
2. Write the minimum production code to make it pass.
3. Refactor with the test still green.

PRs MUST show that the test was authored at or before the production
code (commit history or paired-commit are both acceptable). The `qa`
reviewer agent gates on this — production code without an accompanying
failing-then-passing test is FAIL.

Exempt: pure mechanical refactors (rename, move, extract with no
behaviour change), formatting, and comment-only edits.

**Rationale**: the regression surface here spans aggregate state
machines, event-listener idempotency, and Azure Function trigger
bindings — classes of bug that silently break in production unless
caught fail-first.

### IX. Azure Functions Trigger Hygiene (NON-NEGOTIABLE)

- `*Function.java` classes in `stagingbulkscan-azure-functions` MUST
  remain thin: deserialize the trigger payload, dispatch to a service
  / handler, return the result. No business logic.
- Each function declares its trigger binding in code (`@FunctionName`,
  `@HttpTrigger`, `@QueueTrigger`, etc.) — bindings MUST NOT be
  defined only via `function.json` configuration.
- Azure storage queue / blob names MUST be sourced from configuration
  (`@Named` / environment), not hard-coded.

**Rationale**: thin functions are testable as plain Java; fat functions
become un-testable Azure-runtime contraptions. Code-defined bindings
keep the trigger contract reviewable in PR diffs rather than in
generated JSON.

## Technology Stack & Deployment

- **Microservices framework**: MOJ Justice Framework
  (`uk.gov.justice.services.*`).
- **DI / runtime**: CDI (Weld). NOT Spring.
- **Build tool**: Maven (parent POM
  `uk.gov.moj.cpp.common:service-parent-pom`; Checkstyle + PMD enforced).
- **Java**: per the parent POM (`mvn -version` to confirm).
- **Event sourcing**: Justice Framework `EventStream` + aggregate base
  class.
- **Message contracts**: JSON Schema, modules `stagingbulkscan-json`,
  `stagingbulkscan-command-api`, `stagingbulkscan-event-api`.
- **Cloud**: Microsoft Azure — Functions runtime + Storage queues /
  blobs (`stagingbulkscan-azure-core` + `stagingbulkscan-azure-functions`).
- **Database**: PostgreSQL viewstore (Liquibase-managed, in
  `stagingbulkscan-viewstore`).
- **Logging**: SLF4J + Logback with `correlationId` MDC propagation
  via Justice FW envelope metadata.
- **Tests**: JUnit 5, Mockito, AssertJ, Hamcrest, JsonAssert.
  Integration tests live in `stagingbulkscan-integration-test`,
  suffixed `*IT`.
- **CI/CD**: Azure DevOps pipeline (`azure-pipelines.yaml`).
- **Branching**: jgitflow Maven plugin —
  `dev/release` (master), `main` (develop), `dev/feature-*`,
  `dev/release-*`, `dev/hotfix-*`.

## Development Workflow & Quality Gates

- A JSON Schema change MUST land **before** any Java change that
  affects command/event behaviour (Principle I).
- The build loop (Principle IV) repeats until `code-reviewer`, `qa`,
  and `spec-validator` each return PASS / COMPLIANT.
- TDD (Principle VIII) MUST be visible in commit history: failing test
  commit precedes (or is paired with) the production code that
  satisfies it.
- Every feature built via spec-kit lives under `specs/NNN-slug/`
  containing at least `spec.md`, `plan.md`, and `tasks.md`. Flow:
  `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
  → /speckit-analyze`.
- Required commands run cleanly before merge:
  - `mvn clean install` — Checkstyle + PMD + unit + integration tests.
  - `mvn -pl stagingbulkscan-integration-test verify` — integration
    tests if the touched module is wired through them.
  - `mvn checkstyle:check pmd:check` — static analysis.
- Commit style: Conventional Commits (`feat:`, `fix:`, `chore:`,
  `docs:`, `refactor:`, `test:`, etc.).
- Pull requests: the description MUST state which principle(s) the
  change touches. Any deviation from a principle requires explicit
  written justification in the PR description AND in the plan's
  "Complexity Tracking" section.

## Governance

This constitution supersedes the informal conventions in
`.claude/rules/`. Where this document and those files disagree, this
document wins; the rule files are retained as quick-reference material
and MUST be kept in sync.

**Amendment procedure**:

1. Propose the change in a feature spec under `specs/`.
2. Bump `Version` per semantic versioning:
   - **MAJOR** — a breaking principle change, removal, or redefinition
     that invalidates existing practice.
   - **MINOR** — a new principle, new section, or materially expanded
     guidance.
   - **PATCH** — clarifications, wording, typo fixes, or non-semantic
     refinements.
3. Re-run `/speckit-analyze` on every in-flight feature spec to verify
   it still aligns with the amended principles; update or waive as
   required.

**Compliance expectations**:

- All PRs MUST honour these principles.
- Deviations MUST be explicitly justified in the PR description and,
  where relevant, in the plan's "Complexity Tracking" table.
- Reviewers MUST block merges that silently violate a NON-NEGOTIABLE
  principle without a written waiver.

**Version**: 1.0.0 | **Ratified**: 2026-05-09 | **Last Amended**: 2026-05-09
