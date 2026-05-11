# Feature Specification: Fix Scheduler Out-of-Memory on Large Document Sets

**Feature Branch**: `CIMD-3102-fix-scheduler-oom-pagination`
**Created**: 2026-05-11
**Status**: Draft
**Input**: CIMD-3102 — DeletingActionedDocumentsScheduler causes OOM by loading all 1.3M actioned records into memory before filtering

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Scheduler Completes Without Crashing (Priority: P1)

The document deletion scheduler runs on its configured interval and completes each cycle without exhausting available memory, regardless of how many total actioned records exist in the system.

**Why this priority**: The production system is currently crashing due to this scheduler loading over 1.3 million records into memory. This is an active production incident.

**Independent Test**: Confirmed by running the scheduler against a table containing 1.3 million or more actioned records and observing no out-of-memory error.

**Acceptance Scenarios**:

1. **Given** the scan_document table contains 1.3 million or more actioned records, **When** the scheduler fires, **Then** the scheduler completes without an out-of-memory error.
2. **Given** the scheduler fires, **When** records are retrieved from storage, **Then** only records that are both actioned and past the configured retention period are returned.
3. **Given** the scheduler fires, **When** records are retrieved from storage, **Then** no more than 50,000 records are loaded in a single run by default.

---

### User Story 2 — Eligible Documents Are Still Deleted (Priority: P1)

Documents that are past the configured retention period and have an actioned status continue to be submitted for deletion after this fix. No previously-deletable document is silently skipped.

**Why this priority**: Correctness of the deletion behaviour must be maintained; fixing the memory issue must not introduce a regression where eligible documents stop being deleted.

**Independent Test**: Confirmed by verifying that a document with an actioned status older than the configured retention period is submitted for deletion after a scheduler run.

**Acceptance Scenarios**:

1. **Given** an actioned document whose status was last updated more than the configured number of retention days ago, **When** the scheduler fires, **Then** that document is submitted for deletion.
2. **Given** an actioned document whose status was last updated within the configured retention window, **When** the scheduler fires, **Then** that document is NOT submitted for deletion.
3. **Given** a document with a non-actioned status, **When** the scheduler fires, **Then** that document is not retrieved or submitted for deletion.

---

### User Story 3 — Batch Size Is Configurable (Priority: P2)

The maximum number of records the scheduler retrieves per run can be adjusted via configuration without a code deployment.

**Why this priority**: The default of 50,000 is a safe starting point, but operations teams must be able to tune this value based on observed throughput and memory headroom.

**Independent Test**: Confirmed by setting the batch-size configuration to a custom value and observing the scheduler never retrieves more records than that value in a single run.

**Acceptance Scenarios**:

1. **Given** the batch-size configuration is set to a value N, **When** the scheduler fires, **Then** at most N records are loaded per run.
2. **Given** the batch-size configuration is not set, **When** the scheduler fires, **Then** the scheduler defaults to a batch size of 50,000.

---

### User Story 4 — Scheduler Logs Provide Operational Visibility (Priority: P3)

The scheduler logs the number of eligible records fetched and the number submitted for deletion per run, so operations staff can monitor throughput and detect backlogs.

**Why this priority**: After the fix, teams need observability to confirm the scheduler is clearing the backlog between runs.

**Independent Test**: Confirmed by inspecting scheduler log output for a run that processes at least one eligible document and seeing both a fetched-count entry and a submitted-count entry.

**Acceptance Scenarios**:

1. **Given** the scheduler fires, **When** eligible records are fetched, **Then** a log entry records how many eligible records were fetched this run.
2. **Given** the scheduler fires, **When** deletion commands are dispatched, **Then** a log entry records how many documents were submitted for deletion this run.

---

### Edge Cases

- What happens when there are zero eligible documents? The scheduler completes normally and logs that zero records were fetched and zero deletions were dispatched.
- What happens when there are fewer eligible records than the batch size limit? All eligible records are loaded and processed without error.
- What happens when eligible records exceed the batch size limit? Records beyond the limit are not processed in this run; they remain eligible and are picked up in a subsequent run.
- What happens when the batch-size configuration value is missing or invalid? The scheduler falls back to the default of 50,000 and logs a warning.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The scheduler MUST retrieve only records that have an actioned status (MANUALLY_ACTIONED or AUTO_ACTIONED), whose deleted flag is false, and whose status-updated date is older than the configured retention period — this filtering MUST occur in persistent storage, not in application memory.
- **FR-002**: The scheduler MUST retrieve no more than the configured batch-size maximum per run; the default MUST be 50,000 records.
- **FR-003**: The batch-size maximum MUST be externally configurable without a code deployment.
- **FR-004**: The scheduler MUST log the count of eligible records fetched per run at INFO level.
- **FR-005**: The scheduler MUST log the count of records submitted for deletion per run at INFO level.
- **FR-006**: Every document that was previously eligible for deletion MUST remain eligible after this change — no regression in deletion scope.
- **FR-007**: Documents not processed within a single run due to the batch limit MUST be picked up in a subsequent scheduled run.

### Key Entities

- **Scan Document**: A persisted record of a scanned document. Relevant attributes: status (e.g. MANUALLY_ACTIONED, AUTO_ACTIONED), status-updated date, deleted flag.
- **Deletion Eligibility**: A scan document is eligible for deletion when its status is actioned (MANUALLY_ACTIONED or AUTO_ACTIONED), its deleted flag is false, and the number of days since its status-updated date exceeds the configured retention period.
- **Batch Size**: The maximum number of deletion-eligible scan documents the scheduler may load per run. Defaults to 50,000; externally configurable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The scheduler completes every run without an out-of-memory error, even when the scan_document table contains 1.3 million or more actioned records.
- **SC-002**: No more than 50,000 records are loaded per scheduler run under the default configuration.
- **SC-003**: Every document that was eligible for deletion before this change continues to be deleted — zero regression in deletion coverage.
- **SC-004**: The batch size can be changed via a configuration value with no code change and takes effect on the next scheduler run.
- **SC-005**: Each scheduler run produces at least two observable log entries: one reporting records fetched, one reporting records submitted for deletion.

## Assumptions

- The configured retention period (`deleteAfterActionedDays`) is already present in application configuration and continues to be read from there unchanged.
- A batch size of 50,000 records per run is sufficient to clear the deletion backlog within a reasonable number of scheduling intervals given the current deletion rate.
- Records left unprocessed in one run due to the batch limit remain eligible and will be processed in subsequent runs — no additional requeue or retry mechanism is needed.
- The deletion command dispatched per document is unchanged by this fix.
- No changes are required to the event schema, command schema, or viewstore schema to implement this fix.