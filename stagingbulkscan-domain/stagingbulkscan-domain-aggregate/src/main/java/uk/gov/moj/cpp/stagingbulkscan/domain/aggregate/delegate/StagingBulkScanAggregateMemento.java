package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DocumentNextStepPaused;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StagingBulkScanAggregateMemento implements Serializable {

    private ScanEnvelope scanEnvelope;

    private DocumentNextStepPaused documentNextStepPaused;

    public ScanEnvelope getScanEnvelope() {
        return scanEnvelope;
    }

    public DocumentNextStepPaused getDocumentNextStepPaused() {
        return documentNextStepPaused;
    }

    public void registerScanEnvelope(final ScanEnvelope scanEnvelope) {
        this.scanEnvelope = buildScanEnvelope(scanEnvelope, scanEnvelope.getAssociatedScanDocuments());
    }

    public void markDocumentAsManuallyActioned(final UUID scanDocumentId, final UUID actionedBy, final ZonedDateTime actionedDate) {
        final List<ScanDocument> associatedScanDocuments = buildDocumentsWithActionedItem(scanDocumentId, actionedBy, actionedDate);
        this.scanEnvelope = buildScanEnvelope(getScanEnvelope(), associatedScanDocuments);
    }

    public void markDocumentAsAutoActioned(final UUID scanDocumentId, final UUID actionedBy, final ZonedDateTime statusUpdatedDate) {
        final List<ScanDocument> scanDocuments = buildDocumentsWithActionedItem(scanDocumentId, actionedBy, statusUpdatedDate);
        this.scanEnvelope = buildScanEnvelope(getScanEnvelope(), scanDocuments);
    }

    public void markDocumentAsAutoDelete(final UUID scanDocumentId, final ZonedDateTime deletedDate) {
        final List<ScanDocument> scanDocumentsWithActionedDocument = buildDocumentsWithDeleted(scanDocumentId, deletedDate);
        this.scanEnvelope = buildScanEnvelope(getScanEnvelope(), scanDocumentsWithActionedDocument);
    }

    private List<ScanDocument> buildDocumentsWithActionedItem(final UUID scanDocumentId, final UUID actionedBy, final ZonedDateTime actionedDate) {
        return this.scanEnvelope.getAssociatedScanDocuments().stream()
                .map(scanDocument -> scanDocument.getScanDocumentId().equals(scanDocumentId) ? setActionedDocument(scanDocument, actionedBy, actionedDate) : scanDocument)
                .collect(Collectors.toList());
    }

    private ScanDocument setActionedDocument(final ScanDocument scanDocument, final UUID actionedBy, final ZonedDateTime actionedDate) {
        return buildScanDocument(scanDocument, actionedBy, MANUALLY_ACTIONED, actionedDate, scanDocument.getDeleted(), scanDocument.getDeletedDate());
    }

    private List<ScanDocument> buildDocumentsWithDeleted(final UUID scanDocumentId, final ZonedDateTime deletedDate) {
        return this.scanEnvelope.getAssociatedScanDocuments().stream()
                .map(scanDocument -> scanDocument.getScanDocumentId().equals(scanDocumentId) ? buildDeletedDocument(scanDocument, deletedDate) : scanDocument)
                .collect(Collectors.toList());
    }

    private ScanDocument buildDeletedDocument(final ScanDocument scanDocument, final ZonedDateTime deletedDate) {
        return buildScanDocument(scanDocument, scanDocument.getActionedBy(), scanDocument.getStatus(), scanDocument.getStatusUpdatedDate(), true, deletedDate);
    }

    private ScanEnvelope buildScanEnvelope(final ScanEnvelope scanEnvelope, final List<ScanDocument> scanDocuments) {
        return ScanEnvelope.scanEnvelope()
                .withScanEnvelopeId(scanEnvelope.getScanEnvelopeId())
                .withEnvelopeClassification(scanEnvelope.getEnvelopeClassification())
                .withExtractedDate(scanEnvelope.getExtractedDate())
                .withJurisdiction(scanEnvelope.getJurisdiction())
                .withVendorOpeningDate(scanEnvelope.getVendorOpeningDate())
                .withVendorPOBox(scanEnvelope.getVendorPOBox())
                .withZipFileName(scanEnvelope.getZipFileName())
                .withNotes(scanEnvelope.getNotes())
                .withZipFileCreatedDate(scanEnvelope.getZipFileCreatedDate())
                .withAssociatedScanDocuments(scanDocuments.stream()
                        .map(document -> buildScanDocument(document, ZonedDateTime.now(UTC)))
                        .collect(Collectors.toList()))
                .build();
    }

    private ScanDocument buildScanDocument(final ScanDocument scanDocument, final ZonedDateTime statusUpdatedDate) {
        return buildScanDocument(
                scanDocument,
                scanDocument.getActionedBy(),
                scanDocument.getStatus(),
                statusUpdatedDate,
                scanDocument.getDeleted(),
                scanDocument.getDeletedDate());
    }

    private ScanDocument buildScanDocument(final ScanDocument scanDocument,
                                           final UUID actionedBy,
                                           final DocumentStatus status,
                                           final ZonedDateTime statusUpdatedDate,
                                           final Boolean deleted,
                                           final ZonedDateTime deletedDate) {
        return ScanDocument.scanDocument()
                .withScanDocumentId(scanDocument.getScanDocumentId())
                .withFileName(scanDocument.getFileName())
                .withCaseUrn(scanDocument.getCaseUrn())
                .withCasePTIUrn(scanDocument.getCasePTIUrn())
                .withDocumentControlNumber(scanDocument.getDocumentControlNumber())
                .withDocumentName(scanDocument.getDocumentName())
                .withManualIntervention(scanDocument.getManualIntervention())
                .withNextAction(scanDocument.getNextAction())
                .withNextActionDate(scanDocument.getNextActionDate())
                .withNotes(scanDocument.getNotes())
                .withVendorReceivedDate(scanDocument.getVendorReceivedDate())
                .withActionedBy(actionedBy)
                .withStatus(status)
                .withStatusUpdatedDate(statusUpdatedDate)
                .withDeleted(deleted)
                .withDeletedDate(deletedDate)
                .withPlea(scanDocument.getPlea())
                .withMc100s(scanDocument.getMc100s())
                .build();
    }

    //only for unit tests
    public void setScanEnvelope(ScanEnvelope scanEnvelope) {
        this.scanEnvelope = scanEnvelope;
    }

    public void documentPaused(final DocumentNextStepPaused documentNextStepPaused) {
        this.documentNextStepPaused = documentNextStepPaused;
    }
}
