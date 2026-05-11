# Architecture & Domain Rules

## Service overview

`stagingbulkscan` is an HMCTS Common Platform CQRS / event-sourced
context built on the MOJ **Justice Framework**
(`uk.gov.justice.services.*`). It ingests scanned-document envelopes,
runs auto-actioning logic, persists state via event-sourced aggregates,
and projects a read-model into the viewstore for queries.

## Maven module layout

```
stagingbulkscan-parent (pom)
├── stagingbulkscan-azure-core           # Azure storage adapters
├── stagingbulkscan-azure-functions      # Azure Function entry points
├── stagingbulkscan-command              # write side
│   ├── stagingbulkscan-command-api      # command JSON schemas
│   └── stagingbulkscan-command-handler  # @ServiceComponent(COMMAND_HANDLER)
├── stagingbulkscan-event                # event JSON schemas + listeners
├── stagingbulkscan-event-sources        # event-source bindings
├── stagingbulkscan-domain               # aggregate(s), domain events
├── stagingbulkscan-query                # @ServiceComponent(QUERY_*) views
├── stagingbulkscan-viewstore            # JPA viewstore + Liquibase
├── stagingbulkscan-service              # service-level orchestration
├── stagingbulkscan-json                 # JSON Schemas (cross-module)
├── stagingbulkscan-healthchecks         # custom health checks
├── stagingbulkscan-integration-test     # in-process integration tests
└── stagingbulkscan-testharness          # shared test fixtures
```

The module graph IS the architecture. Cross-module dependencies that
would create cycles or violate CQRS direction (e.g. `command-handler →
query-api`, `query → command-handler`) are forbidden. New cross-module
deps require explicit justification in the plan's "Complexity Tracking"
section.

## CQRS / event-sourcing layering (NON-NEGOTIABLE)

```
JSON command  → CommandController (Justice FW codegen)
              → CommandHandler (@ServiceComponent(COMMAND_HANDLER), @Handles)
              → Aggregate (StagingBulkScanAggregate)
              → Domain Event(s)
              → Event Store (Justice FW EventStream)
              → EventListener (@ServiceComponent(EVENT_LISTENER), @Handles)
              → Viewstore JPA repository
              → Read model (@ServiceComponent(QUERY_VIEW))
```

- **CommandHandler:** validates the envelope and delegates to an aggregate
  via `aggregate(AggregateClass.class, id, envelope, a -> a.method(...))`.
  No business logic in command handlers beyond pulling fields out of the
  payload and dispatching.
- **Aggregate:** the **only** place state-change decisions are made.
  Aggregates emit events; they do not call services.
- **Event:** immutable, JSON-schema-defined, generated under
  `uk.gov.justice.stagingbulkscan.event.*`.
- **EventListener:** updates the viewstore (and only the viewstore).
  Listeners must be idempotent — re-delivery is expected.
- **Query:** reads viewstore JPA entities only. Queries MUST NOT touch
  the event store or any command-side class.

NEVER put business logic in:
- Azure Function entry points (`*Function.java` in
  `stagingbulkscan-azure-functions`) — they deserialize, dispatch, return.
- REST controllers (Justice FW generates these from RAML/JSON Schema).
- JSON-Schema generated DTOs.

NEVER:
- Mutate aggregate state from outside the aggregate.
- Call command handlers from event listeners (no synchronous fan-in).
- Read from the event store inside a query.

## Justice Framework conventions

- `@ServiceComponent(COMMAND_API | COMMAND_HANDLER | COMMAND_CONTROLLER |
  EVENT_LISTENER | EVENT_PROCESSOR | QUERY_API | QUERY_VIEW | QUERY_CONTROLLER)`
  is the canonical layering marker. Each class declares exactly one role.
- `@Handles("<context>.<type>.<name>")` binds a handler method to a
  named JSON message. The string MUST match the JSON Schema id under
  `stagingbulkscan-json/`.
- Envelope types: `Envelope<T>` (typed payload) for codegen-bound payloads,
  `JsonEnvelope` (raw JSON) for hand-extraction. Prefer `Envelope<T>`
  when the schema-generated DTO exists.
- Aggregates extend the Justice FW base aggregate; event application is
  done via `Apply`-method dispatch.
- Liquibase migrations live in
  `stagingbulkscan-viewstore/src/main/resources/liquibase/`.

## JSON Schema is the contract

Command and event names, payload shapes, and field types are defined in
JSON Schemas under `stagingbulkscan-json` /
`stagingbulkscan-command-api` / `stagingbulkscan-event-api`. The Justice
Framework codegen produces the matching DTOs at build time.

- Adding or changing a command/event MUST start with the JSON Schema.
- A field rename or type change in a schema is a breaking change for
  every downstream consumer — flag it in the plan.

## Out-of-scope (do not add)

- Spring Boot / Spring Framework dependencies — this is Justice FW + CDI
  (Weld). Adding Spring is a constitutional deviation.
- Maven → Gradle migration — out of scope unless covered by a separate
  spec.
- Synchronous cross-context HTTP calls from inside a command handler.
- Business decisions taken in JSON-Schema generated DTOs.
