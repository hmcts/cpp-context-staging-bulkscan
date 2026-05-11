# Coding Conventions — MOJ / CPP Standard for `cpp-context-staging-bulkscan`

## Dependency Injection

This service uses **CDI (Weld)** via the Justice Framework. NOT Spring.

- Use `@Inject` for all injection points — never `@Autowired`.
- Constructor injection is preferred over field injection for testability:
  - Constructor injection: `@Inject` on a constructor with `private
    final` fields.
  - Field injection (`@Inject` on the field) is permitted only where it
    matches the surrounding class style (Justice FW base classes use it),
    but new classes SHOULD prefer constructor.
- Use `@Named("qualifier")` only when multiple beans of the same type
  exist.

```java
// PREFERRED — constructor injection
private final EventStream eventStream;

@Inject
public MyHandler(final EventStream eventStream) {
    this.eventStream = eventStream;
}
```

## Justice Framework annotations (use exactly one per class)

- `@ServiceComponent(COMMAND_HANDLER)` — write-side handler
- `@ServiceComponent(EVENT_LISTENER)` — projects events into viewstore
- `@ServiceComponent(QUERY_VIEW)` — read-side query
- `@ServiceComponent(QUERY_API)` — query API
- `@ServiceComponent(COMMAND_API | COMMAND_CONTROLLER)` — command surface
- `@Handles("<name>")` — message-name binding for handlers/listeners
- `@FrameworkComponent` — non-domain framework-level beans

## DTOs and value types

- New internal value types SHOULD be Java records.
- Command and event payload types are Justice FW codegen output (POJOs
  with getters) under `uk.gov.justice.stagingbulkscan.*` — DO NOT
  hand-edit; change the JSON Schema instead.
- Hand-written value types live under
  `uk.gov.moj.cpp.stagingbulkscan.*`.

## Logging

- SLF4J via `private static final Logger LOGGER =
  LoggerFactory.getLogger(<Class>.class)` OR Lombok `@Slf4j` if Lombok
  is in scope.
- NEVER use `System.out.println`, `System.err.println`, or
  `Throwable#printStackTrace()` (Constitution Principle VI).
- NEVER log sensitive data (PII, case data, tokens, defendant identifiers
  in plaintext).
- Wrap raw payload logs in `if (LOGGER.isDebugEnabled())` and prefer
  `JsonEnvelope#toObfuscatedDebugString()` for envelope contents.
- Honour the `correlationId` MDC key — Justice FW propagates it via the
  envelope metadata; do not reset MDC inside handlers.

## Imports

- NEVER use wildcard imports (`import java.util.*`) — always explicit.
- Static imports are permitted for clarity (`import static
  java.util.UUID.fromString;`) but should be focused — avoid wildcards.

## Error handling

- Justice Framework defines its own exception hierarchy
  (`EventStreamException`, etc.) — declare them on the method signature
  rather than swallowing.
- Application exceptions extend `RuntimeException` unless the framework
  expects a checked exception.
- Never `catch (Exception e) { /* swallowed */ }`. If catch-all is
  necessary (e.g. message acknowledgement), log via SLF4J at WARN/ERROR
  with sufficient context to triage.

## Naming Conventions

| Component               | Pattern                  | Example                                    |
|-------------------------|--------------------------|--------------------------------------------|
| Command Handler         | `*CommandHandler`        | `StagingBulkScanCommandHandler`            |
| Event Listener          | `*EventListener` / `*Processor` | `ScanEnvelopeEventListener`         |
| Query View              | `*QueryView`             | `ScanEnvelopeQueryView`                    |
| Aggregate               | `*Aggregate`             | `StagingBulkScanAggregate`                 |
| Service                 | `*Service`               | `AutoActioningService`                     |
| Repository (viewstore)  | `*Repository`            | `ScanEnvelopeRepository`                   |
| JPA entity              | `*Entity` (or domain noun) | `ScanEnvelopeEntity`                     |
| Configuration           | `*Configuration`         | `AzureStorageConfiguration`                |
| Azure Function          | `*Function`              | `IngestEnvelopeFunction`                   |
| Test                    | `*Test` / `*IT`          | `StagingBulkScanCommandHandlerTest` / `*IT`|

## Testing Conventions

- **Stack:** JUnit 5 + Mockito + AssertJ + Hamcrest (already on the
  service-parent-pom test classpath).
- `@ExtendWith(MockitoExtension.class)` for unit tests.
- `@Nested` classes with `@DisplayName` for grouped scenarios.
- Method naming: `{action}_{scenario}_should_{expectation}`
  (e.g. `registerEnvelope_whenValidPayload_shouldDispatchToAggregate`).
- Integration tests live in `stagingbulkscan-integration-test`,
  suffixed `*IT`, and use the in-process Justice FW test container.
- Shared test fixtures (envelope builders, sample payloads) live in
  `stagingbulkscan-testharness` — reuse, don't duplicate.
- TDD is mandatory (Constitution Principle VIII): write the failing
  test first, see it fail for the right reason (assertion failure, not
  NoClassDefFoundError), then implement.
- Tests MUST also use SLF4J — never `System.out` even in tests.

## JSON Schemas

- Schemas live under `stagingbulkscan-json/` and per-side `*-command-api`
  / `*-event-api` modules.
- Schema id is the `@Handles` string. Renaming a schema id is a
  breaking change.
- Optional fields MUST be modelled as `[ "null", "<type>" ]` or omitted
  from `required[]` — relying on absent JSON keys for defaults is
  fragile.

## Liquibase

- All schema changes live in `stagingbulkscan-viewstore/src/main/resources/liquibase/`.
- Each changeset has a unique `id`; never edit a previously-released
  changeset — write a new one.
- Liquibase changes MUST be backwards-compatible across at least one
  release boundary (the deploy is rolling).

## Maven hygiene

- New dependencies go in `dependencyManagement` of the parent POM if
  reused across modules; otherwise in the leaf module's POM.
- Pin versions explicitly in `dependencyManagement`. Don't rely on
  transitive resolution for hard-pinned versions.
- `mvn dependency:tree` MUST be checked before adding a dep that already
  appears transitively.
