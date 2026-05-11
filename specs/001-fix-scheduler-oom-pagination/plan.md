# Implementation Plan: Fix Scheduler Out-of-Memory on Large Document Sets

**Branch**: `CIMD-3102-fix-scheduler-oom-pagination` | **Date**: 2026-05-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-fix-scheduler-oom-pagination/spec.md`

## Summary

`DeletingActionedDocumentsScheduler` currently loads every `MANUALLY_ACTIONED` / `AUTO_ACTIONED`
record (1.3 M rows) into JVM heap and then filters by date in-memory, causing OOM.

The fix introduces a dedicated query message `stagingbulkscan.get-documents-eligible-for-deletion`
that pushes `statusUpdatedDate < cutoffDate` filtering and a configurable `maxResults` cap to the
database. The scheduler continues to use `Requester.requestAsAdmin()` through the full
`QUERY_API → QUERY_VIEW → Service → Repository` chain (Constitution Principle I satisfied).

Impacted files: **14** across 5 modules. No new Maven modules. No viewstore schema changes.

---

## Technical Context

**Language/Version**: Java (per `service-parent-pom`; ≥ 11)
**Primary Dependencies**: MOJ Justice Framework, DeltaSpike Data, CDI (Weld), JPA
**Storage**: PostgreSQL — `scan_document` table via `ScanDocumentRepository`
**Testing**: JUnit 5, Mockito, AssertJ
**Target Platform**: Azure (WAR on Justice FW container)
**Project Type**: Multi-module Maven CQRS / event-sourced service
**Performance Goal**: Scheduler run completes without OOM; ≤ 50 K rows per run
**Constraints**: No Spring; SLF4J-only logging; explicit CDI injection; no wildcard imports
**Scale/Scope**: 1.3 M `scan_document` rows; fix reduces per-run load to ≤ 50 K

---

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. CQRS Slice Discipline | ✅ PASS | Scheduler continues to read via `QUERY_API → QUERY_VIEW`. New query follows identical routing chain as existing queries. |
| II. Constructor Injection (CDI) | ✅ PASS | New beans use `@Inject` matching surrounding class style. |
| III. Maven Module-Boundary Respect | ✅ PASS | No new cross-module dependencies introduced. |
| IV. Spec-Driven Build Loop | ✅ PASS | Full `/speckit-*` flow followed. |
| V. HMCTS Standards Compliance | ✅ PASS | Maven, SLF4J, parent POM, `uk.gov.moj.cpp.stagingbulkscan.*` packages. |
| VI. No `System.out` / `System.err` | ✅ PASS | All logging via existing `LOGGER` (SLF4J). |
| VII. No Wildcard Imports | ✅ PASS | All imports explicit. |
| VIII. Test-Driven Development | ✅ PASS | Failing tests authored before production code for every changed class. |
| IX. Azure Functions Trigger Hygiene | ✅ N/A | No Azure Function classes modified. |

---

## Project Structure

### Documentation (this feature)

```text
specs/001-fix-scheduler-oom-pagination/
├── spec.md
├── plan.md              ← this file
├── research.md
├── data-model.md
└── tasks.md             ← created by /speckit-tasks
```

### Source Code Changes

```text
stagingbulkscan-viewstore/
└── stagingbulkscan-viewstore-persistence/
    └── src/
        ├── main/java/…/repository/
        │   └── ScanDocumentRepository.java              ← ADD findDocumentsEligibleForDeletion()
        └── test/java/…/repository/
            └── ScanDocumentRepositoryTest.java          ← ADD test for new method

stagingbulkscan-query/
├── stagingbulkscan-query-view/
│   └── src/
│       ├── main/java/…/query/view/
│       │   ├── StagingBulkScanQueryView.java            ← ADD findDocumentsEligibleForDeletion()
│       │   └── service/
│       │       └── StagingBulkScanService.java          ← ADD getDocumentsEligibleForDeletion()
│       └── test/java/…/query/view/
│           ├── StagingBulkScanQueryViewTest.java        ← ADD test for new handler method
│           └── service/
│               └── StagingBulkScanServiceTest.java      ← ADD test for new service method
└── stagingbulkscan-query-api/
    └── src/
        ├── main/java/…/query/api/
        │   └── StagingBulkScanQueryApi.java             ← ADD @Handles delegation method
        ├── raml/
        │   ├── stagingbulkscan-query-api.raml           ← ADD /scan-documents/eligible-for-deletion
        │   ├── json/
        │   │   └── stagingbulkscan.get-documents-eligible-for-deletion.json  ← NEW response example
        │   └── json/schema/
        │       └── stagingbulkscan.get-documents-eligible-for-deletion.json  ← NEW response schema

stagingbulkscan-azure-core/
└── src/main/java/…/service/
    └── ApplicationParameters.java                       ← ADD deletionBatchSize field + getter

stagingbulkscan-event/
└── stagingbulkscan-event-processor/
    └── src/
        ├── main/java/…/event/processor/
        │   └── DeletingActionedDocumentsScheduler.java  ← MODIFY: call new query, remove in-memory filter
        └── test/java/…/event/processor/
            └── DeletingActionedDocumentsSchedulerTest.java ← ADD startTimer() tests
```

---

## Implementation Guide

### Step 1 — `ScanDocumentRepository`: new concrete method

Add a non-abstract method using `entityManager()` directly (DeltaSpike `@Query` does not support
a dynamic `maxResults` parameter; only compile-time constants are accepted via `@Query(max=N)`).

```java
public List<ScanDocument> findDocumentsEligibleForDeletion(
        final List<DocumentStatus> statuses,
        final ZonedDateTime cutoffDate,
        final int maxResults) {
    return entityManager()
            .createQuery(
                    "FROM ScanDocument doc " +
                    "WHERE doc.status IN :statuses " +
                    "AND doc.deleted = false " +
                    "AND doc.statusUpdatedDate < :cutoffDate " +
                    "ORDER BY doc.statusUpdatedDate ASC",
                    ScanDocument.class)
            .setParameter("statuses", statuses)
            .setParameter("cutoffDate", cutoffDate)
            .setMaxResults(maxResults)
            .getResultList();
}
```

Ordering `ASC` on `statusUpdatedDate` drains the oldest backlog first across multiple runs.

---

### Step 2 — `StagingBulkScanService`: new service method

```java
private static final List<DocumentStatus> DELETION_ELIGIBLE_STATUSES =
        List.of(DocumentStatus.MANUALLY_ACTIONED, DocumentStatus.AUTO_ACTIONED);

public ScanDocumentsResponse getDocumentsEligibleForDeletion(
        final ZonedDateTime cutoffDate,
        final int maxResults) {
    final List<ScanDocument> documents = scanDocumentRepository
            .findDocumentsEligibleForDeletion(DELETION_ELIGIBLE_STATUSES, cutoffDate, maxResults);
    final ScanDocumentsResponse response = new ScanDocumentsResponse();
    response.setScanDocuments(documents.stream().map(populateDocument()).collect(Collectors.toList()));
    return response;
}
```

The eligible statuses are encapsulated here; callers do not need to know them.

---

### Step 3 — `StagingBulkScanQueryView`: new handler method

```java
private static final String QUERY_GET_DOCUMENTS_ELIGIBLE_FOR_DELETION =
        "stagingbulkscan.get-documents-eligible-for-deletion";
private static final String FIELD_CUTOFF_DATE = "cutoffDate";
private static final String FIELD_MAX_RESULTS = "maxResults";
private static final int DEFAULT_MAX_RESULTS = 50_000;

public JsonEnvelope findDocumentsEligibleForDeletion(final JsonEnvelope envelope) {
    final String cutoffDateStr = envelope.payloadAsJsonObject().getString(FIELD_CUTOFF_DATE);
    final int maxResults = envelope.payloadAsJsonObject()
            .getInt(FIELD_MAX_RESULTS, DEFAULT_MAX_RESULTS);
    final ZonedDateTime cutoffDate = ZonedDateTime.parse(cutoffDateStr);

    return enveloper.withMetadataFrom(envelope, QUERY_GET_DOCUMENTS_ELIGIBLE_FOR_DELETION)
            .apply(objectToJsonObjectConverter.convert(
                    stagingBulkScanService.getDocumentsEligibleForDeletion(cutoffDate, maxResults)));
}
```

---

### Step 4 — `StagingBulkScanQueryApi`: new `@Handles` delegation

```java
@Handles("stagingbulkscan.get-documents-eligible-for-deletion")
public JsonEnvelope findDocumentsEligibleForDeletion(final JsonEnvelope query) {
    return this.stagingBulkScanQueryView.findDocumentsEligibleForDeletion(query);
}
```

---

### Step 5 — RAML: new resource

Add to `stagingbulkscan-query-api.raml` (below existing resources):

```raml
/scan-documents/eligible-for-deletion:
  get:
    description: |
      Returns actioned scan documents eligible for deletion (past the retention window),
      up to a configurable limit. Used internally by the deletion scheduler.
      ...
      (mapping):
          responseType: application/vnd.stagingbulkscan.get-documents-eligible-for-deletion+json
          name: stagingbulkscan.get-documents-eligible-for-deletion
      ...
    queryParameters:
      cutoffDate:
        description: ISO-8601 date-time; documents with statusUpdatedDate before this are eligible
        type: string
        required: true
      maxResults:
        description: Maximum records to return (default 50000)
        type: integer
        required: false
    responses:
      200:
        description: OK
        body:
          application/vnd.stagingbulkscan.get-documents-eligible-for-deletion+json:
            example: !include json/stagingbulkscan.get-documents-eligible-for-deletion.json
            schema:  !include json/schema/stagingbulkscan.get-documents-eligible-for-deletion.json
```

This is required by `StagingBulkScanQueryApiTest.testActionNameAndHandleNameAreSame` which asserts
that every `@Handles` value has a matching `name:` entry in the RAML and vice versa.

---

### Step 6 — Response JSON files

**`src/raml/json/stagingbulkscan.get-documents-eligible-for-deletion.json`** (response example):
```json
{
  "scanDocuments": []
}
```

**`src/raml/json/schema/stagingbulkscan.get-documents-eligible-for-deletion.json`** (response schema):
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://justice.gov.uk/staging/bulkscan/json/schemas/get-documents-eligible-for-deletion.json",
  "type": "object",
  "properties": {
    "scanDocuments": {
      "type": "array",
      "items": {
        "$ref": "http://justice.gov.uk/staging/bulkscan/json/schemas/scan-document.json"
      }
    }
  }
}
```

Note: `minItems` is intentionally absent — an empty result (no eligible documents) is valid.

---

### Step 7 — `ApplicationParameters`: add `deletionBatchSize`

```java
@Inject
@Value(key = "deletionBatchSize")
private String deletionBatchSize;

public String getDeletionBatchSize() {
    return deletionBatchSize;
}
```

---

### Step 8 — `DeletingActionedDocumentsScheduler`: replace in-memory filter

**Remove**:
- `getAllActionedDocuments()` private method only
- In-memory `.filter()` + `.collect()` from `startTimer()`

> **Note — incremental delivery**: The `startTimer()` body shown below is the **final** state after all phases are complete. During Phase 3 (T027), implement the query call and document loop but omit the two `LOGGER.info` calls — those are added in Phase 5 (T035) following the US4 TDD cycle. `convertToList()` is retained throughout; it is reused in the new body to deserialise the response JSON array.

**Add**:
- Constant `DEFAULT_DELETION_BATCH_SIZE = 50_000`
- Constant for the new query name `STAGING_BULK_SCAN_QUERY_GET_DOCUMENTS_ELIGIBLE_FOR_DELETION`
- `parseBatchSize()` private helper

**Replace `startTimer()` body**:

```java
@Timeout
public void startTimer() {
    LOGGER.info("DeletingActionedDocumentsScheduler triggers.");

    final int batchSize = parseBatchSize();
    final ZonedDateTime cutoffDate = ZonedDateTime.now()
            .minus(Long.parseLong(applicationParameters.getDeleteAfterActionedDays()), DAYS);

    final Envelope<JsonValue> requestEnvelope = envelopeFrom(
            metadataBuilder()
                    .withId(randomUUID())
                    .withName(STAGING_BULK_SCAN_QUERY_GET_DOCUMENTS_ELIGIBLE_FOR_DELETION)
                    .build(),
            createObjectBuilder()
                    .add("cutoffDate", cutoffDate.toString())
                    .add("maxResults", batchSize)
                    .build());

    final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
    final List<ScanDocument> eligibleDocuments =
            convertToList(response.payload().getJsonArray("scanDocuments"), ScanDocument.class);

    LOGGER.info("Number of documents eligible for deletion: {}", eligibleDocuments.size());

    eligibleDocuments.forEach(document ->
            sender.send(envelopeFrom(
                    metadataBuilder().withId(randomUUID()).withName(DELETE_ACTIONED_DOCUMENTS).build(),
                    buildPayload(document))));

    LOGGER.info("Number of documents submitted for deletion: {}", eligibleDocuments.size());
}

private int parseBatchSize() {
    try {
        return Integer.parseInt(applicationParameters.getDeletionBatchSize());
    } catch (final NumberFormatException e) {
        LOGGER.warn("Invalid or missing deletionBatchSize config, defaulting to {}",
                DEFAULT_DELETION_BATCH_SIZE);
        return DEFAULT_DELETION_BATCH_SIZE;
    }
}
```

`buildPayload(ScanDocument)` and `convertToList()` are unchanged — `ScanDocument` here is
still the response DTO `uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument`.

---

### Step 9 — Tests

#### `DeletingActionedDocumentsSchedulerTest`

New mocks needed: `ApplicationParameters` (already mocked), `Requester` (already mocked).
Remove mocks that were only used by `getAllActionedDocuments()` if any were added.

| Test method | Scenario | Key assertion |
|-------------|----------|---------------|
| `startTimer_whenEligibleDocumentsExist_shouldSendDeleteCommandForEach` | 2 documents returned by requester | `verify(sender, times(2)).send(any())` |
| `startTimer_whenNoEligibleDocuments_shouldNotSendAnyCommand` | empty array from requester | `verify(sender, never()).send(any())` |
| `startTimer_shouldPassCutoffDateBasedOnRetentionDays` | capture request envelope payload | `cutoffDate ≈ now() - deleteAfterActionedDays` |
| `startTimer_shouldPassConfiguredBatchSizeToQuery` | capture request envelope payload | `maxResults == configuredValue` |
| `startTimer_whenBatchSizeConfigMissing_shouldUseDefault50000` | `getDeletionBatchSize()` returns `null` | `maxResults == 50000` in captured payload |
| `startTimer_shouldLogFetchedAndSubmittedCounts` | verify log calls (via SLF4J test appender or LOGGER spy) | two INFO log entries |

#### `StagingBulkScanQueryViewTest`

| Test method | Scenario | Key assertion |
|-------------|----------|---------------|
| `findDocumentsEligibleForDeletion_shouldCallServiceWithParsedCutoffDateAndMaxResults` | valid envelope | service called with correct `ZonedDateTime` and `int` |
| `findDocumentsEligibleForDeletion_whenMaxResultsAbsent_shouldUseDefault` | payload has no `maxResults` | service called with `50000` |
| `findDocumentsEligibleForDeletion_shouldReturnEnvelopeWithCorrectName` | verify response name | envelope name == `stagingbulkscan.get-documents-eligible-for-deletion` |

#### `StagingBulkScanServiceTest`

| Test method | Scenario | Key assertion |
|-------------|----------|---------------|
| `getDocumentsEligibleForDeletion_shouldDelegateToRepositoryWithCorrectStatuses` | capture args to repo | statuses = `[MANUALLY_ACTIONED, AUTO_ACTIONED]` |
| `getDocumentsEligibleForDeletion_shouldMapRepositoryResultsToResponse` | repo returns 2 entities | response contains 2 `ScanDocument` DTOs |

#### `ScanDocumentRepositoryTest`

Follow the existing `@RunWith(CdiTestRunner.class)` CDI integration test pattern.

| Test method | Scenario | Key assertion |
|-------------|----------|---------------|
| `findDocumentsEligibleForDeletion_shouldReturnOnlyEligibleDocuments` | mix of eligible + non-eligible rows in DB | only eligible returned |
| `findDocumentsEligibleForDeletion_shouldRespectMaxResultsLimit` | more eligible rows than limit | result size == limit |
| `findDocumentsEligibleForDeletion_shouldOrderByStatusUpdatedDateAscending` | multiple eligible rows | oldest first |
| `findDocumentsEligibleForDeletion_shouldExcludeDeletedDocuments` | eligible-by-date but `deleted = true` | not included |

---

## Complexity Tracking

> No constitution violations. No new cross-module dependencies. CQRS slice direction fully respected.
