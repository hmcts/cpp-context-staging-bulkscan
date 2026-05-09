# Workflow: Mandatory Build Loop

Every non-trivial code change MUST follow this cycle:

```
Spec → Write → Code Review (agent) → QA (agent) → Spec Validate (agent) → Fix → Ship
```

The spec-driven entry point is the speckit slash-command flow:

```
/speckit-specify → /speckit-clarify → /speckit-plan → /speckit-tasks
                 → /speckit-implement → /speckit-analyze
```

The build loop repeats until `code-reviewer`, `qa`, and `spec-validator`
all return PASS / COMPLIANT.

## What requires the loop

| Must go through loop                                | Exempt                          |
|-----------------------------------------------------|---------------------------------|
| New / changed JSON Schema (command, event, query)   | Markdown / docs only            |
| New / changed Java production code                  | Whitespace / import-only edits  |
| New / changed test class                            | `.claude/rules/*` updates       |
| Liquibase migration                                 | `CLAUDE.md` updates             |
| Maven POM dependency or plugin change               | README updates                  |
| Azure Function trigger / binding change             |                                 |
| Helm chart / pipeline config (in CPP infra repos)   |                                 |

## TDD is non-negotiable (Constitution Principle VIII)

- Write the failing test first; confirm it fails for the *correct*
  reason (assertion failure, not `NoClassDefFoundError` /
  compilation error).
- Write the minimum production code to make it pass.
- Refactor with the test still green.
- Commit history MUST show the failing test was authored at or before
  the production code (paired commits also acceptable).

## Agent definitions

### code-reviewer (Read only)
- Spawned as sub-agent with read-only tools.
- Reviews diff for: logic errors, null safety, layering violations
  (controller / handler / aggregate / listener / query separation),
  module-boundary violations, secrets, `System.out` usage, CDI vs Spring
  drift, JSON Schema mismatches.
- Returns: **PASS** or **NEEDS CHANGES** with severity-rated findings.
- NEVER modifies code — reports only.

### qa (Read, Write, Bash)
- Spawned as sub-agent.
- Generates / extends unit + integration tests (JUnit 5 + Mockito +
  AssertJ; integration tests via the testharness module).
- Verifies TDD discipline (failing test authored before production
  code).
- Runs `mvn test` (or `-pl <module> test`) and surfaces failures.
- Returns: **PASS** or **FAIL** with test results.
- NEVER fixes production code — only writes tests.

### spec-validator (Read only)
- Spawned as sub-agent with read-only tools.
- Validates JSON Schemas under `stagingbulkscan-json/`,
  `*-command-api`, `*-event-api`:
  - Schema id matches the `@Handles` string in the corresponding
    handler / listener.
  - Required fields are populated by the codegen DTOs.
  - Schema changes are backwards-compatible (no required-field
    additions on existing events) or the spec explicitly calls out
    the breaking change.
- Validates that aggregate `Apply` methods exist for every emitted
  event.
- Returns: **COMPLIANT** or **DRIFT DETECTED** with findings.
- NEVER modifies code — reports only.

### software-engineer (Full access)
- For full feature implementation tasks.
- Follows all rules in `technical-rules.md` and the constitution.
- Runs `mvn -pl <module> -am verify` (or full `mvn install`) after
  changes.

### research (Read, Glob, Grep, WebSearch)
- For deep codebase investigation.
- Cross-references design documents and JSON Schemas.
- Returns structured findings with citations.

## Critical principle

**Agents are reporters, not fixers.** The parent agent (or developer)
reads agent reports and applies all fixes. This prevents conflicting
auto-edits and keeps a human / primary agent as the decision point.

## Exemptions

The loop does NOT apply to:
- Markdown / docs-only edits.
- Whitespace / import-ordering / autoformatter-only edits.
- Updates to `.claude/rules/*` or `CLAUDE.md` themselves.
- README / LICENSE / CODEOWNERS edits.

For exempt changes a single PR with a brief description suffices.
