# Research: Fix Scheduler Out-of-Memory on Large Document Sets

**Branch**: `CIMD-3102-fix-scheduler-oom-pagination`
**Date**: 2026-05-11

## Root Cause Confirmed

`DeletingActionedDocumentsScheduler.startTimer()` calls `getAllActionedDocuments()` which dispatches
`stagingbulkscan.get-all-documents-by-status` via `Requester.requestAsAdmin()`. That query hits
`ScanDocumentRepository.findAllDocumentsByStatusByAsc(statuses)` — a DeltaSpike Data `@Query`
with **no WHERE clause on `status_updated_date`** and **no LIMIT**. With 1.3 M rows matching the
two actioned statuses, Hibernate materialises all of them and the JVM runs out of heap.

The scheduler then performs the date check **in memory**:
```java
.filter(document -> DAYS.between(document.getStatusUpdatedDate(), ZonedDateTime.now())
    > Integer.parseInt(applicationParameters.getDeleteAfterActionedDays()))
```
This is the correct eligibility check — it just needs to move to the DB query.

---

## Decision 1: Introduce a dedicated query `stagingbulkscan.get-documents-eligible-for-deletion`

**Decision**: Add a new query message that accepts `cutoffDate` and `maxResults` as payload
parameters, routing through the existing `QUERY_API → QUERY_VIEW → Service → Repository` chain.
The scheduler calls this new query instead of the existing `get-all-documents-by-status`.

**Rationale**: Keeps strict CQRS separation (EVENT_PROCESSOR reads via QUERY_API only).
The new query is purpose-built: statuses are hardcoded in the service because this query has
a single well-defined caller (the deletion scheduler). Does not touch or pollute the existing
`get-all-documents-by-status` endpoint which is also used by the REST / UI path.

**Alternatives considered**:
- *Extend existing `get-all-documents-by-status` with optional parameters*: Risks breaking
  existing consumers; mixes scheduler-specific concerns into a general-purpose REST endpoint.
- *Inject `ScanDocumentRepository` directly into the scheduler*: Bypasses CQRS. Rejected
  because CQRS Slice Discipline (Constitution Principle I) is NON-NEGOTIABLE.

---

## Decision 2: RAML entry required alongside the new `@Handles`

**Finding**: `StagingBulkScanQueryApiTest.testActionNameAndHandleNameAreSame` reads every
`name:` line from `stagingbulkscan-query-api.raml` and asserts that the set of RAML names and
the set of `@Handles` values on `StagingBulkScanQueryApi` are identical (`containsInAnyOrder`).
Adding a new `@Handles("stagingbulkscan.get-documents-eligible-for-deletion")` without a
matching RAML entry will fail this test.

**Decision**: Add a RAML resource `/scan-documents/eligible-for-deletion` with `cutoffDate`
and `maxResults` as query parameters, following the existing RAML style.
Also add the corresponding response example JSON and response schema JSON.

---

## Decision 3: Use EntityManager directly for dynamic `maxResults`

**Decision**: The new repository method calls `entityManager().createQuery(...).setMaxResults(n)`
rather than using a DeltaSpike `@Query` abstract method.

**Rationale**: DeltaSpike Data's `@Query(max = N)` accepts only compile-time constants. A dynamic
`maxResults` parameter requires the JPA `EntityManager` API directly. `AbstractEntityRepository`
exposes `entityManager()` for exactly this use case.

---

## Decision 4: Add `deletionBatchSize` configuration key

**Decision**: Add `deletionBatchSize` as a new `@Value`-injected field in `ApplicationParameters`,
following the existing pattern. The scheduler defaults to 50 000 if the value is absent or unparseable.

**Rationale**: FR-003 requires the batch size to be externally configurable without code deployment.

---

## Confirmed: No viewstore schema changes required

- `scan_document.status_updated_date` column already exists (confirmed from JPA entity `ScanDocument`).
- No new Liquibase migrations, event schemas, or command schemas needed.

---

## Impacted Files

| File | Module | Change |
|------|--------|--------|
| `ScanDocumentRepository.java` | `stagingbulkscan-viewstore-persistence` | Add concrete `findDocumentsEligibleForDeletion` method |
| `ScanDocumentRepositoryTest.java` | `stagingbulkscan-viewstore-persistence` | Add test for new method |
| `StagingBulkScanService.java` | `stagingbulkscan-query-view` | Add `getDocumentsEligibleForDeletion` method |
| `StagingBulkScanServiceTest.java` | `stagingbulkscan-query-view` | Add test for new method |
| `StagingBulkScanQueryView.java` | `stagingbulkscan-query-view` | Add `findDocumentsEligibleForDeletion` handler method |
| `StagingBulkScanQueryViewTest.java` | `stagingbulkscan-query-view` | Add test for new handler |
| `StagingBulkScanQueryApi.java` | `stagingbulkscan-query-api` | Add `@Handles` delegation method |
| `StagingBulkScanQueryApiTest.java` | `stagingbulkscan-query-api` | Covered automatically by existing RAML sync test |
| NEW `stagingbulkscan.get-documents-eligible-for-deletion.json` | `stagingbulkscan-query-api` | Response example JSON |
| NEW `schema/stagingbulkscan.get-documents-eligible-for-deletion.json` | `stagingbulkscan-query-api` | Response schema JSON |
| `stagingbulkscan-query-api.raml` | `stagingbulkscan-query-api` | New resource `/scan-documents/eligible-for-deletion` |
| `ApplicationParameters.java` | `stagingbulkscan-azure-core` | Add `deletionBatchSize` field + getter |
| `DeletingActionedDocumentsScheduler.java` | `stagingbulkscan-event-processor` | Call new query; remove in-memory filter |
| `DeletingActionedDocumentsSchedulerTest.java` | `stagingbulkscan-event-processor` | Add `startTimer()` tests |
