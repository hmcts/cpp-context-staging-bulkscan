# Tasks: Fix Scheduler Out-of-Memory on Large Document Sets

**Input**: Design documents from `specs/001-fix-scheduler-oom-pagination/`
**Branch**: `CIMD-3102-fix-scheduler-oom-pagination`
**TDD**: All implementation tasks are preceded by failing-test tasks (Constitution Principle VIII)

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[US#]**: User story this task belongs to

---

## Phase 1: Setup

**Purpose**: Verify prerequisites before any code change

- [X] T001 Confirm branch is `CIMD-3102-fix-scheduler-oom-pagination` via `git branch --show-current`
- [X] T002 Run `mvn -DskipTests clean install` from repo root to confirm the project compiles cleanly before any changes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Artifacts that must exist before the query routing chain can be wired. No production Java yet.

**⚠️ CRITICAL**: Phases 3–6 cannot begin until this phase is complete.

- [X] T003 [P] Create response example file `stagingbulkscan-query/stagingbulkscan-query-api/src/raml/json/stagingbulkscan.get-documents-eligible-for-deletion.json` containing `{"scanDocuments": []}`
- [X] T004 [P] Create response schema file `stagingbulkscan-query/stagingbulkscan-query-api/src/raml/json/schema/stagingbulkscan.get-documents-eligible-for-deletion.json` with `$schema`, `id`, `type: object`, `properties.scanDocuments` array referencing `scan-document.json` (no `minItems` — empty result is valid)
- [X] T005 Add RAML resource `/scan-documents/eligible-for-deletion` with `cutoffDate` (required string) and `maxResults` (optional integer) query parameters, response mapping `name: stagingbulkscan.get-documents-eligible-for-deletion`, and `!include` references to T003/T004 files in `stagingbulkscan-query/stagingbulkscan-query-api/src/raml/stagingbulkscan-query-api.raml`
- [X] T006 [P] Add `@Inject @Value(key = "deletionBatchSize") private String deletionBatchSize` field and `getDeletionBatchSize()` getter to `stagingbulkscan-azure-core/src/main/java/uk/gov/moj/cpp/stagingbulkscan/azure/core/service/ApplicationParameters.java`

**Checkpoint**: T003–T006 complete — foundational artifacts in place. Proceed to Phase 3.

---

## Phase 3: User Stories 1 & 2 — Core OOM Fix & Deletion Correctness (Priority: P1)

**Goal**: Replace unbounded in-memory fetch with a DB-filtered, batch-limited query through the CQRS stack. Delivers US1 (no OOM) and US2 (correct deletions) simultaneously.

**Independent Test**: Run scheduler against a table with 1.3 M actioned rows → completes without OOM. Run with a mix of eligible/ineligible documents → only eligible ones are submitted for deletion.

### Repository Layer — TDD cycle

> **Write tests first. Confirm they FAIL (compile error is not a pass) before T011.**

- [X] T007 [P] [US1] Write failing CDI integration test `findDocumentsEligibleForDeletion_shouldReturnOnlyEligibleDocuments` in `stagingbulkscan-viewstore/stagingbulkscan-viewstore-persistence/src/test/java/uk/gov/moj/cpp/stagingbulkscan/repository/ScanDocumentRepositoryTest.java` — seed mix of eligible and ineligible rows, assert only eligible returned
- [X] T008 [P] [US1] Write failing test `findDocumentsEligibleForDeletion_shouldRespectMaxResultsLimit` in `ScanDocumentRepositoryTest.java` — seed more eligible rows than the limit, assert result size equals limit
- [X] T009 [P] [US1] Write failing test `findDocumentsEligibleForDeletion_shouldOrderByStatusUpdatedDateAscending` in `ScanDocumentRepositoryTest.java` — assert oldest eligible document is first in the returned list
- [X] T010 [P] [US2] Write failing test `findDocumentsEligibleForDeletion_shouldExcludeDeletedDocuments` in `ScanDocumentRepositoryTest.java` — document with `deleted = true` but otherwise eligible must NOT be returned
- [X] T011 [US1] Implement concrete method `findDocumentsEligibleForDeletion(List<DocumentStatus> statuses, ZonedDateTime cutoffDate, int maxResults)` in `stagingbulkscan-viewstore/stagingbulkscan-viewstore-persistence/src/main/java/uk/gov/moj/cpp/stagingbulkscan/repository/ScanDocumentRepository.java` using `entityManager().createQuery(...).setParameter(...).setMaxResults(maxResults).getResultList()` with JPQL `WHERE doc.status IN :statuses AND doc.deleted = false AND doc.statusUpdatedDate < :cutoffDate ORDER BY doc.statusUpdatedDate ASC`
- [X] T012 [US1] Run `mvn -pl stagingbulkscan-viewstore/stagingbulkscan-viewstore-persistence test` — all four new repository tests must pass (green)

### Service Layer — TDD cycle

- [X] T013 [P] [US1] Write failing unit test `getDocumentsEligibleForDeletion_shouldDelegateToRepositoryWithCorrectStatuses` in `stagingbulkscan-query/stagingbulkscan-query-view/src/test/java/uk/gov/moj/cpp/stagingbulkscan/query/view/service/StagingBulkScanServiceTest.java` — verify repository called with `[MANUALLY_ACTIONED, AUTO_ACTIONED]`, the cutoffDate, and maxResults
- [X] T014 [P] [US2] Write failing unit test `getDocumentsEligibleForDeletion_shouldMapRepositoryResultsToResponse` in `StagingBulkScanServiceTest.java` — repo returns 2 entities → response contains 2 `ScanDocument` DTOs with correct field mapping
- [X] T015 [US1] Implement `getDocumentsEligibleForDeletion(ZonedDateTime cutoffDate, int maxResults)` in `stagingbulkscan-query/stagingbulkscan-query-view/src/main/java/uk/gov/moj/cpp/stagingbulkscan/query/view/service/StagingBulkScanService.java` — hardcode statuses `List.of(MANUALLY_ACTIONED, AUTO_ACTIONED)`, delegate to repository, map to `ScanDocumentsResponse` using existing `populateDocument()` helper
- [X] T016 [US1] Run `mvn -pl stagingbulkscan-query/stagingbulkscan-query-view test` — both new service tests must pass

### Query View Layer — TDD cycle

- [X] T017 [P] [US1] Write failing unit test `findDocumentsEligibleForDeletion_shouldCallServiceWithParsedCutoffDateAndMaxResults` in `stagingbulkscan-query/stagingbulkscan-query-view/src/test/java/uk/gov/moj/cpp/stagingbulkscan/query/view/StagingBulkScanQueryViewTest.java` — envelope with `cutoffDate` ISO string and `maxResults` int → service called with parsed `ZonedDateTime` and correct int
- [X] T018 [P] [US1] Write failing unit test `findDocumentsEligibleForDeletion_whenMaxResultsAbsent_shouldUseDefault50000` in `StagingBulkScanQueryViewTest.java` — envelope payload without `maxResults` field → service called with `50000`
- [X] T019 [P] [US1] Write failing unit test `findDocumentsEligibleForDeletion_shouldReturnEnvelopeWithCorrectName` in `StagingBulkScanQueryViewTest.java` — response envelope name == `stagingbulkscan.get-documents-eligible-for-deletion`
- [X] T020 [US1] Add handler method `findDocumentsEligibleForDeletion(JsonEnvelope envelope)` to `stagingbulkscan-query/stagingbulkscan-query-view/src/main/java/uk/gov/moj/cpp/stagingbulkscan/query/view/StagingBulkScanQueryView.java` — extract `cutoffDate` string and `maxResults` int from payload, parse `ZonedDateTime`, delegate to service, wrap with `enveloper.withMetadataFrom(envelope, "stagingbulkscan.get-documents-eligible-for-deletion")`
- [X] T021 [US1] Run `mvn -pl stagingbulkscan-query/stagingbulkscan-query-view test` — all three new query view tests must pass

### Query API Layer

- [X] T022 [US1] Add `@Handles("stagingbulkscan.get-documents-eligible-for-deletion")` method `findDocumentsEligibleForDeletion(JsonEnvelope query)` delegating to `stagingBulkScanQueryView.findDocumentsEligibleForDeletion(query)` in `stagingbulkscan-query/stagingbulkscan-query-api/src/main/java/uk/gov/moj/cpp/stagingbulkscan/query/api/StagingBulkScanQueryApi.java`
- [X] T023 [US1] Run `mvn -pl stagingbulkscan-query/stagingbulkscan-query-api test` — `StagingBulkScanQueryApiTest.testActionNameAndHandleNameAreSame` must pass (validates T005 RAML entry and T022 handler are in sync)

### Scheduler — TDD cycle

- [X] T024 [P] [US1] Write failing unit test `startTimer_whenEligibleDocumentsExist_shouldSendDeleteCommandForEach` in `stagingbulkscan-event/stagingbulkscan-event-processor/src/test/java/uk/gov/moj/cpp/stagingbulkscan/event/processor/DeletingActionedDocumentsSchedulerTest.java` — mock `requester` returns envelope with 2 scan documents → `verify(sender, times(2)).send(any())`
- [X] T025 [P] [US1] Write failing unit test `startTimer_whenNoEligibleDocuments_shouldNotSendAnyCommand` in `DeletingActionedDocumentsSchedulerTest.java` — mock `requester` returns empty `scanDocuments` array → `verify(sender, never()).send(any())`
- [X] T026 [P] [US2] Write failing unit test `startTimer_shouldPassCutoffDateBasedOnRetentionDays` in `DeletingActionedDocumentsSchedulerTest.java` — capture the request envelope payload sent to `requester`; assert `cutoffDate` field ≈ `now().minus(deleteAfterActionedDays, DAYS)` (within a few seconds tolerance)
- [X] T027 [US1] Implement changes to `startTimer()` in `stagingbulkscan-event/stagingbulkscan-event-processor/src/main/java/uk/gov/moj/cpp/stagingbulkscan/event/processor/DeletingActionedDocumentsScheduler.java`: replace `getAllActionedDocuments()` call and in-memory filter with a `Requester.requestAsAdmin()` call to `stagingbulkscan.get-documents-eligible-for-deletion` passing `cutoffDate` (hardcode `maxResults` = 50000 for now — US3 will wire config); remove only `getAllActionedDocuments()` private method — retain `convertToList()` as it is reused in the new `startTimer()` body to deserialise the response JSON array
- [X] T028 [US1] Run `mvn -pl stagingbulkscan-event/stagingbulkscan-event-processor test` — T024, T025, T026 tests must pass

**Checkpoint**: US1 + US2 complete. Full query chain delivers the core OOM fix and correctness guarantee.

---

## Phase 4: User Story 3 — Configurable Batch Size (Priority: P2)

**Goal**: Wire `deletionBatchSize` config into the scheduler with a safe fallback to 50 000.

**Independent Test**: Set `deletionBatchSize=100` in config → scheduler passes `maxResults=100` in query payload. Remove config key → scheduler passes `maxResults=50000`.

### TDD cycle

- [X] T029 [US3] Write failing unit test `startTimer_shouldPassConfiguredBatchSizeToQuery` in `DeletingActionedDocumentsSchedulerTest.java` — `applicationParameters.getDeletionBatchSize()` returns `"1000"` → captured `requester` call payload has `maxResults=1000`
- [X] T030 [US3] Write failing unit test `startTimer_whenBatchSizeConfigMissing_shouldUseDefault50000` in `DeletingActionedDocumentsSchedulerTest.java` — `applicationParameters.getDeletionBatchSize()` returns `null` → captured payload has `maxResults=50000`
- [X] T031 [US3] Add `parseBatchSize()` private helper to `DeletingActionedDocumentsScheduler.java` reading `applicationParameters.getDeletionBatchSize()` and falling back to `DEFAULT_DELETION_BATCH_SIZE = 50_000` on `NumberFormatException` or `null`; update `startTimer()` to call `parseBatchSize()` instead of the hardcoded `50000` from T027
- [X] T032 [US3] Run `mvn -pl stagingbulkscan-event/stagingbulkscan-event-processor test` — T029 and T030 must pass

**Checkpoint**: US3 complete. Batch size is now configurable via the `deletionBatchSize` property.

---

## Phase 5: User Story 4 — Operational Logging (Priority: P3)

**Goal**: Two INFO log entries per scheduler run — records fetched count and records submitted count.

**Independent Test**: Inspect scheduler logs for a run processing at least one eligible document; confirm both log lines are present.

### TDD cycle

- [X] T033 [P] [US4] Write failing unit test `startTimer_shouldSendDeleteCommandForAllThreeFetchedDocuments` in `DeletingActionedDocumentsSchedulerTest.java` — mock requester returns 3 docs, assert `verify(sender, times(3)).send(any())` (log content not verifiable without logback 1.4+; count invariant preserved via behavioral assertion)
- [X] T034 [P] [US4] Write failing unit test `startTimer_shouldSubmitExactlyAsManyCommandsAsDocumentsFetched` in `DeletingActionedDocumentsSchedulerTest.java` — mock requester returns 5 docs, assert `verify(sender, times(5)).send(any())` (validates fetch count == submit count invariant)
- [X] T035 [US4] Add two `LOGGER.info(...)` calls to `startTimer()` in `DeletingActionedDocumentsScheduler.java`: one after fetching (`"Number of documents eligible for deletion: {}"`) and one after dispatching (`"Number of documents submitted for deletion: {}"`)
- [X] T036 [US4] Run `mvn -pl stagingbulkscan-event/stagingbulkscan-event-processor test` — T033 and T034 must pass

**Checkpoint**: US4 complete. All four user stories delivered.

---

## Phase 6: Polish & Validation

**Purpose**: Full build verification, static analysis, cross-module integration

- [X] T037 [P] Run `mvn checkstyle:check pmd:check` from repo root — zero violations in any changed file
- [X] T038 Run `mvn clean install` from repo root — Checkstyle + PMD + all unit tests + integration tests must pass
- [X] T039 [P] Verify `ScanDocumentRepositoryTest` tests for `findDocumentsEligibleForDeletion` pass via `mvn -pl stagingbulkscan-viewstore/stagingbulkscan-viewstore-persistence -am test`
- [X] T040 [P] Confirm no wildcard imports introduced in any changed file (`grep -r "import.*\*" stagingbulkscan-viewstore stagingbulkscan-query stagingbulkscan-azure-core stagingbulkscan-event --include="*.java"` must return nothing new)
- [X] T041 [P] Confirm no `System.out` / `System.err` / `printStackTrace` introduced (`grep -r "System\.\(out\|err\)\|printStackTrace" stagingbulkscan-viewstore stagingbulkscan-query stagingbulkscan-azure-core stagingbulkscan-event --include="*.java"` must return nothing new)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS** Phases 3–6
- **Phase 3 (US1+US2)**: Depends on Phase 2 completion — no other story dependencies
- **Phase 4 (US3)**: Depends on Phase 3 (scheduler `startTimer()` must exist to add `parseBatchSize()`)
- **Phase 5 (US4)**: Depends on Phase 3 (`startTimer()` must exist to add log calls)
- **Phase 6 (Polish)**: Depends on Phases 3–5 all complete

### User Story Dependencies

- **US1 + US2 (P1)**: Start after Phase 2 — no dependencies on US3/US4
- **US3 (P2)**: Depends on US1 (`startTimer()` implemented in T027)
- **US4 (P3)**: Depends on US1 (`startTimer()` implemented in T027); independent of US3

### Within Each User Story

- Tests MUST be written and confirmed failing before the corresponding implementation task
- Repository layer → Service layer → Query View layer → Query API layer → Scheduler layer
- Each layer's tests pass before moving to the next layer

### Parallel Opportunities Within Phase 3

```
# These can run in parallel (different files):
T007, T008, T009, T010   ← All four repository failing-tests
T013, T014               ← Both service failing-tests
T017, T018, T019         ← All three query view failing-tests
T024, T025, T026         ← All three scheduler failing-tests
```

---

## Parallel Example: Phase 3 (US1+US2)

```bash
# Step 1 — Write all repository failing tests in parallel (different test methods in same file)
Task T007: findDocumentsEligibleForDeletion_shouldReturnOnlyEligibleDocuments
Task T008: findDocumentsEligibleForDeletion_shouldRespectMaxResultsLimit
Task T009: findDocumentsEligibleForDeletion_shouldOrderByStatusUpdatedDateAscending
Task T010: findDocumentsEligibleForDeletion_shouldExcludeDeletedDocuments

# Step 2 — Implement repository (T011), then run tests (T012)

# Step 3 — Write service failing tests in parallel
Task T013: getDocumentsEligibleForDeletion_shouldDelegateToRepositoryWithCorrectStatuses
Task T014: getDocumentsEligibleForDeletion_shouldMapRepositoryResultsToResponse

# Step 4 — Implement service (T015), run tests (T016)

# ... continue layer by layer
```

---

## Implementation Strategy

### MVP (User Stories 1 + 2 only — fixes the production incident)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (JSON schema, RAML, ApplicationParameters)
3. Complete Phase 3: US1 + US2 — core OOM fix
4. **STOP and VALIDATE**: Run full build + confirm scheduler test suite passes
5. **Deploy** — this alone resolves the production OOM

### Incremental Delivery

1. Setup + Foundational → infra ready
2. Phase 3 → OOM fixed, correctness preserved (deploy)
3. Phase 4 → batch size configurable (deploy)
4. Phase 5 → operational visibility (deploy)
5. Phase 6 → full clean build + static analysis

---

## Notes

- Constitution Principle VIII (TDD) is NON-NEGOTIABLE: tests at T007–T010, T013–T014, T017–T019, T024–T026, T029–T030, T033–T034 MUST fail for the correct reason (assertion failure, not compilation error) before the corresponding implementation tasks are started
- `[P]` tasks touch different files and have no shared state — safe to generate simultaneously when using `/speckit-implement`
- Each build command in Phase 3–5 validates the current story before moving to the next
- `StagingBulkScanQueryApiTest` (T023) is already written — it validates T005 + T022 are in sync automatically; no new test case needed for the delegation method itself
