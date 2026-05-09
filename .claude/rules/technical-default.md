# Service Identity

- **Service:** cpp-context-staging-bulkscan
- **Description:** Common Platform context for ingesting bulk-scanned
  documents (envelopes containing case-related scans), running
  auto-actioning, and projecting state for downstream consumers via
  CQRS / event-sourcing.
- **Programme:** Crime Common Platform (CPP)
- **Organisation:** HMCTS / Ministry of Justice
- **Group ID:** `uk.gov.moj.cpp.stagingbulkscan`
- **Parent POM:** `uk.gov.moj.cpp.common:service-parent-pom:17.104.0`
- **Current version:** see `pom.xml` `<version>` (e.g. `17.104.47-SNAPSHOT`)

## Technology Stack

| Component       | Value                                                        |
|-----------------|--------------------------------------------------------------|
| Microservices   | MOJ Justice Framework (`uk.gov.justice.services.*`)          |
| Build tool      | Maven (multi-module). NEVER Gradle.                          |
| DI / runtime    | CDI (Weld) — `@Inject`, `@ServiceComponent`. NOT Spring.     |
| Event sourcing  | Justice Framework `EventStream` + aggregate base class       |
| Message contract| JSON Schema (in `stagingbulkscan-json` / `*-command-api` / `*-event-api`) |
| Serialization   | JSON via Justice FW `JsonEnvelope` / `Envelope<T>`           |
| Cloud           | Microsoft Azure — Functions runtime + Storage queues / blobs |
| Logging         | SLF4J + Logback (parent POM default)                         |
| Tests           | JUnit 5, Mockito, AssertJ, Hamcrest, JsonAssert              |
| Static analysis | Checkstyle + PMD (configured in parent POM)                  |
| Branching       | jgitflow — `dev/release` master, `main` develop, `dev/feature-*` features, `dev/release-*` release branches, `dev/hotfix-*` hotfixes |
| CI/CD           | Azure DevOps Pipeline                                        |

## Constraints

- NEVER add Spring (Spring Boot / Spring Framework) — incompatible with
  Justice Framework's CDI runtime.
- NEVER use Maven wildcard imports for Java (`import java.util.*`).
- NEVER use `System.out` / `System.err` / `printStackTrace()`.
- Hand-written code lives under `uk.gov.moj.cpp.stagingbulkscan.*`.
- JSON-Schema-generated code lives under `uk.gov.justice.stagingbulkscan.*`
  — do NOT hand-edit generated sources; change the schema instead.
- Aggregates may live only in `stagingbulkscan-domain`.
- Liquibase migrations may live only in `stagingbulkscan-viewstore`.
- Adding a new command or event requires a JSON Schema change FIRST.

## Build & Test Commands

```bash
mvn clean install                       # Full multi-module build (compile + Checkstyle + PMD + UT + IT)
mvn -DskipTests clean install           # Skip tests for a fast compile
mvn -pl stagingbulkscan-command-handler test          # Unit tests for a single module
mvn -pl stagingbulkscan-integration-test verify       # Integration tests
mvn -pl stagingbulkscan-domain -am test               # Module + its deps
mvn checkstyle:check                    # Checkstyle (configured in parent POM)
mvn pmd:check                           # PMD static analysis

# Single test class / method
mvn -pl stagingbulkscan-command-handler -Dtest=StagingBulkScanCommandHandlerTest test
mvn -pl stagingbulkscan-command-handler -Dtest=StagingBulkScanCommandHandlerTest#registerEnvelope test

# Local Azure Functions run
mvn -pl stagingbulkscan-azure-functions azure-functions:run
```

## Branch & Release Workflow

- **Feature branches:** `dev/feature-<jira-id>-<slug>` (jgitflow prefix).
- **Develop branch:** `main`. PRs target `main`.
- **Release branches:** `dev/release-<version>`. Cut from `main`.
- **Master / production:** `dev/release` (note: this is the
  jgitflow `masterBranchName`, despite the unusual naming).
- jgitflow Maven plugin handles version bumps —
  `mvn jgitflow:release-start` etc.

## Module Map (read-only at a glance)

| Module                                | Purpose                                       |
|---------------------------------------|-----------------------------------------------|
| `stagingbulkscan-azure-core`          | Azure storage / queue adapters                |
| `stagingbulkscan-azure-functions`     | Azure Function entry points (HTTP + Queue triggers) |
| `stagingbulkscan-command`             | Command-side parent (-api + -handler children) |
| `stagingbulkscan-domain`              | Aggregate(s), domain events                   |
| `stagingbulkscan-event`               | Event listeners + event JSON schemas          |
| `stagingbulkscan-event-sources`       | Event-source bindings (Justice FW)            |
| `stagingbulkscan-query`               | Read-side @ServiceComponent(QUERY_*) classes  |
| `stagingbulkscan-viewstore`           | JPA viewstore + Liquibase                     |
| `stagingbulkscan-service`             | Cross-cutting service orchestration           |
| `stagingbulkscan-json`                | Cross-module JSON Schemas                     |
| `stagingbulkscan-healthchecks`        | Custom health checks                          |
| `stagingbulkscan-integration-test`    | In-process integration tests                  |
| `stagingbulkscan-testharness`         | Shared test fixtures                          |
