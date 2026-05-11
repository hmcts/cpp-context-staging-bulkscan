# Data Model: Fix Scheduler Out-of-Memory on Large Document Sets

**Branch**: `CIMD-3102-fix-scheduler-oom-pagination`
**Date**: 2026-05-11

## Existing Entity: `scan_document`

No viewstore schema changes. This page documents the fields used by the new
deletion-eligibility query.

### JPA Entity: `ScanDocument`

**Package**: `uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument`
**Table**: `scan_document`

| Field | Column | Type | Role in new query |
|-------|--------|------|-------------------|
| `id` (PK part) | `id` | `UUID` (via `ScanSnapshotKey`) | Returned in response |
| `scanEnvelopeId` (PK part) | `scan_envelope_id` | `UUID` (via `ScanSnapshotKey`) | Returned in response |
| `status` | `status` | `DocumentStatus` (STRING enum) | **Filter**: `IN (MANUALLY_ACTIONED, AUTO_ACTIONED)` |
| `deleted` | `deleted` | `boolean` | **Filter**: `= false` |
| `statusUpdatedDate` | `status_updated_date` | `ZonedDateTime` | **Filter**: `< :cutoffDate`; **Order**: ASC |

---

## New Query Message

### `stagingbulkscan.get-documents-eligible-for-deletion`

**Request payload** (sent by `DeletingActionedDocumentsScheduler`):

| Field | Type | Description |
|-------|------|-------------|
| `cutoffDate` | ISO-8601 string | Documents with `statusUpdatedDate` before this instant are eligible |
| `maxResults` | integer | Maximum records to return; defaults to `50000` if absent |

**Response payload** (same shape as `get-all-documents-by-status`):

```json
{
  "scanDocuments": [
    {
      "id": "...",
      "scanEnvelopeId": "...",
      "status": "MANUALLY_ACTIONED",
      "statusUpdatedDate": "..."
    }
  ]
}
```

---

## New Repository Method

### `ScanDocumentRepository.findDocumentsEligibleForDeletion`

```
Method:   findDocumentsEligibleForDeletion(
              ZonedDateTime cutoffDate,
              int maxResults
          ): List<ScanDocument>

JPQL:     FROM ScanDocument doc
          WHERE  doc.status IN :statuses
            AND  doc.deleted = false
            AND  doc.statusUpdatedDate < :cutoffDate
          ORDER BY doc.statusUpdatedDate ASC

Statuses: List.of(MANUALLY_ACTIONED, AUTO_ACTIONED) — passed by the calling service method
maxResults: applied via EntityManager.setMaxResults(maxResults)
```

**Ordering rationale**: `ASC` on `statusUpdatedDate` processes the oldest eligible records first,
ensuring the backlog is drained in a deterministic order across multiple scheduler runs.

---

## Deletion Eligibility — State Transition

A `ScanDocument` becomes eligible for deletion when ALL of the following are true:

1. `status` is `MANUALLY_ACTIONED` or `AUTO_ACTIONED`
2. `deleted` is `false`
3. `statusUpdatedDate` is at least `deleteAfterActionedDays` days before now

The scheduler computes `cutoffDate = now().minus(deleteAfterActionedDays, DAYS)` and passes it to
the query. Once a delete command is dispatched and processed, the document's `deleted` flag is set
to `true` by the existing event → listener → viewstore path, so it will not be selected again.

---

## Configuration: `deletionBatchSize`

| Property key | Default (applied in scheduler) | Constraint |
|-------------|-------------------------------|------------|
| `deletionBatchSize` | `50000` | Positive integer; WARN log + fallback to default if absent or invalid |
