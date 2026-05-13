# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build
```bash
# Full build (all modules)
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build a single module
mvn clean install -pl stagingbulkscan-event/stagingbulkscan-event-processor -am

# Run a single test class
mvn test -pl stagingbulkscan-event/stagingbulkscan-event-processor -Dtest=StagingBulkScanEventProcessorTest

# Build with SonarQube analysis
./buildWithSonar.sh
```

### Integration Tests
Integration tests require Docker and an external `cpp-developers-docker` repo:
```bash
export CPP_DOCKER_DIR=/path/to/cpp-developers-docker
./runIntegrationTests.sh
```

### Release / Versioning
Releases are managed via jgitflow. Branch conventions:
- `main` — active development (develop branch)
- `dev/release` — release branch (master equivalent)
- `dev/feature-*` — feature branches
- `dev/release-*` — release cut branches

## Architecture

This is a **CQRS + Event Sourcing** microservice built on the HMCTS `cpp` / `uk.gov.justice` framework. The service manages bulk-scanned court documents.

### Module Responsibilities

| Module | Role |
|---|---|
| `stagingbulkscan-command/stagingbulkscan-command-api` | REST command endpoints; RAML → code-generated JAX-RS + JMS dispatcher |
| `stagingbulkscan-command/stagingbulkscan-command-handler` | Handles commands via `@Handles`; loads/saves domain aggregate via event stream |
| `stagingbulkscan-domain/stagingbulkscan-domain-aggregate` | `StagingBulkScanAggregate` — event-sourced aggregate with delegate pattern |
| `stagingbulkscan-domain/stagingbulkscan-domain-event` | Internal domain event POJOs |
| `stagingbulkscan-domain/stagingbulkscan-domain-common` | Shared validators (NI number, postcode, email, phone, driving licence) and query response DTOs |
| `stagingbulkscan-event/stagingbulkscan-event-listener` | `EVENT_LISTENER` — projects events into the viewstore (JPA) |
| `stagingbulkscan-event/stagingbulkscan-event-processor` | `EVENT_PROCESSOR` — orchestration; publishes public events; scheduler for deleting aged documents |
| `stagingbulkscan-event-sources` | YAML event source definitions (JMS topic URIs, datasource JNDI names) |
| `stagingbulkscan-query/stagingbulkscan-query-api` | REST query endpoints; RAML → code-generated JAX-RS |
| `stagingbulkscan-query/stagingbulkscan-query-view` | `QUERY_VIEW` — reads from viewstore repositories; handles query routing |
| `stagingbulkscan-viewstore/stagingbulkscan-viewstore-persistence` | JPA entities (`ScanEnvelope`, `ScanDocument`) and Spring Data repositories |
| `stagingbulkscan-viewstore/stagingbulkscan-viewstore-liquibase` | Liquibase database migrations |
| `stagingbulkscan-json` | JSON schemas for all domain types (used for validation and code generation) |
| `stagingbulkscan-azure-core` | Azure Blob Storage client (`BlobClientProvider`) and application parameters |
| `stagingbulkscan-azure-functions` | Azure Functions: `BulkScanInboxProcessor` (EventGrid trigger on blob) and `PoliceEmailExtractorFunction` |
| `stagingbulkscan-service` | Deployable WAR assembly |
| `stagingbulkscan-integration-test` | End-to-end tests using REST + JMS + Wiremock |
| `stagingbulkscan-healthchecks` | Provides ignored healthcheck names for the framework |
| `stagingbulkscan-testharness` | Test utilities for other CPP contexts consuming this service |

### Request / Event Flow

**Command path** (inbound scan document registration and actions):
```
HTTP POST → Command API (JAX-RS, RAML-generated)
  → JMS topic (stagingbulkscan.handler.command)
    → Command Handler (@Handles, @ServiceComponent(COMMAND_HANDLER))
      → StagingBulkScanAggregate (loads state from event stream)
        → Aggregate produces domain events → persisted to event store
```

**Event propagation** (after command is processed):
```
Event Store
  ├─→ EVENT_LISTENER (stagingbulkscan-event-listener)
  │     Projects events into viewstore JPA tables (ScanDocument, ScanEnvelope)
  └─→ EVENT_PROCESSOR (stagingbulkscan-event-processor)
        Produces public events (public.stagingbulkscan.*)
        Calls external contexts (SJP service) via Requester/Sender
```

**Query path**:
```
HTTP GET → Query API (RAML-generated JAX-RS)
  → QUERY_VIEW (StagingBulkScanQueryView)
    → StagingBulkScanService / ScanDocumentRepository (JPA/viewstore)
```

**Azure ingestion path** (bulk scan zip from court scanners):
```
Azure Blob Storage (inbox container)
  → EventGrid trigger → BulkScanInboxProcessor (Azure Function)
    → Extracts ZIP, converts PDF pages to PNG thumbnails
    → Calls Command API (register-scan-envelope)
    → Stores PDFs + PNGs in Azure Blob Storage
```

### Framework Conventions

The `uk.gov.justice` framework drives most wiring via annotations and code generation:

- **`@ServiceComponent(COMPONENT_TYPE)`** — marks a class as a framework-managed component (COMMAND_HANDLER, EVENT_PROCESSOR, EVENT_LISTENER, QUERY_VIEW)
- **`@Handles("event.name")`** — routes a named message to the annotated method
- **RAML files** in `src/raml/` are processed by the framework Maven plugin to generate JAX-RS endpoints and JMS plumbing; the `(mapping):` block in RAML docstrings maps media types to message names
- **YAML subscription descriptors** (`subscriptions-descriptor.yaml`) declare which events each component subscribes to
- **Event sources** (`event-sources.yaml`) map logical event source names to JMS URIs and datasources
- **Aggregate pattern**: `StagingBulkScanAggregate` uses `match/when/otherwiseDoNothing` from `EventSwitcher` to apply events. State is held in `StagingBulkScanAggregateMemento`; domain logic is split across delegate classes (`StagingBulkScanDelegate`, `DefendantPleaDelegate`, `DefendantFinancialMeansDelegate`)

### Key Domain Concepts

- **ScanEnvelope**: A batch of scanned documents (zip file) arriving from a court scanner or police
- **ScanDocument**: An individual document within a `ScanEnvelope`
- **Document statuses**: `MANUALLY_ACTIONED`, `AUTO_ACTIONED` — actioned documents are physically deleted from Azure Blob after a configurable number of days (`deleteAfterActionedDays`) by `DeletingActionedDocumentsScheduler`
- **SJP (Single Justice Procedure)**: Documents may be routed to the SJP context based on `isSjp` flag
- **Public events**: Events prefixed `public.stagingbulkscan.*` are published onto the shared public JMS topic for consumption by other contexts

### CI/CD

Azure Pipelines (`azure-pipelines.yaml`):
- **PR builds**: run `context-verify` template (compile + unit tests + SonarQube PR analysis)
- **Push to `main`**: run `context-validation` template (full build + integration tests + image push)
- Agent pool: `MDV-ADO-AGENT-AKS-01` with `centos8-j17` identifier (Java 17)
- SonarQube project key: `uk.gov.moj.cpp.stagingbulkscan:stagingbulkscan-parent`
