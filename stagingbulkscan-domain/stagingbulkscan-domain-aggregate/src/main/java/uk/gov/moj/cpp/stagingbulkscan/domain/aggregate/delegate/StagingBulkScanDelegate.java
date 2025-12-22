package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentExpired;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentRejected;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DocumentNextStepDecided;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DocumentNextStepPaused;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAttachedAndFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class StagingBulkScanDelegate implements Serializable {

    private static final String MC100 = "SJPMC100";

    private final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    public StagingBulkScanDelegate(final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento) {
        this.stagingBulkScanAggregateMemento = stagingBulkScanAggregateMemento;
    }

    public Stream<Object> register(final ScanEnvelope scanEnvelope) {
        final Stream.Builder<Object> streams = Stream.builder();
        streams.add(new ScanEnvelopeRegistered(scanEnvelope));
        if(nonNull(this.stagingBulkScanAggregateMemento.getDocumentNextStepPaused())){
            final DocumentNextStepPaused documentNextStepPaused = this.stagingBulkScanAggregateMemento.getDocumentNextStepPaused();
            raiseDocumentEvent(scanEnvelope,documentNextStepPaused.getScanEnvelopeId(), documentNextStepPaused.getScanDocumentId(), documentNextStepPaused.getIsSjp(), streams);
        }
        return streams.build();
    }

    public void handleEnvelopeAttribute(final ScanEnvelopeRegistered scanEnvelopeRegistered) {
        this.stagingBulkScanAggregateMemento.registerScanEnvelope(scanEnvelopeRegistered.getScanEnvelope());
    }

    public void handleDocumentPaused(DocumentNextStepPaused documentNextStepPaused) {
        this.stagingBulkScanAggregateMemento.documentPaused(documentNextStepPaused);
    }

    public Stream<Object> markAsManuallyActioned(final UUID scanEnvelopeId, final UUID scanDocumentId, final UUID actionedBy) {
        return Stream.of(new ScanDocumentManuallyActioned(scanEnvelopeId, scanDocumentId, actionedBy, now(UTC)));
    }

    public void handleScanDocumentManuallyActioned(final ScanDocumentManuallyActioned scanDocumentManuallyActioned) {
        this.stagingBulkScanAggregateMemento.markDocumentAsManuallyActioned(scanDocumentManuallyActioned.getScanDocumentId(), scanDocumentManuallyActioned.getActionedBy(), scanDocumentManuallyActioned.getActionedDate());
    }

    public Stream<Object> markAsAutoActioned(final UUID scanEnvelopeId, final UUID scanDocumentId, final UUID actionedBy) {
        final Stream.Builder streams = Stream.builder();
        streams.add(new ScanDocumentAutoActioned(scanEnvelopeId, scanDocumentId, actionedBy, now(UTC)));
        final Optional<ScanDocument> scanDocument = stagingBulkScanAggregateMemento.getScanEnvelope().getAssociatedScanDocuments().stream().filter(document -> document.getScanDocumentId().equals(scanDocumentId)).findFirst();
        if (scanDocument.isPresent() && isValidPleaDocument(scanDocument.get())) {
            if (isNotBlank(scanDocument.get().getCaseUrn())) {
                streams.add(new DefendantDetailsUpdateRequested(scanEnvelopeId, scanDocumentId, scanDocument.get().getCaseUrn()));
            } else {
                streams.add(new DefendantDetailsUpdateRequested(scanEnvelopeId, scanDocumentId, scanDocument.get().getCasePTIUrn()));
            }
        } else if (scanDocument.isPresent() && isValidFinancialMeansDocument(scanDocument.get())) {
            if (isNotBlank(scanDocument.get().getCaseUrn())) {
                streams.add(new DefendantFinancialMeansUpdateRequested(scanEnvelopeId, scanDocumentId, scanDocument.get().getCaseUrn()));
            } else {
                streams.add(new DefendantFinancialMeansUpdateRequested(scanEnvelopeId, scanDocumentId, scanDocument.get().getCasePTIUrn()));
            }
        }
        return streams.build();
    }

    private boolean isValidFinancialMeansDocument(ScanDocument scanDocument) {
        return scanDocument.getMc100s() != null && MC100.equalsIgnoreCase(scanDocument.getDocumentName());
    }

    private boolean isValidPleaDocument(ScanDocument scanDocument) {
        return scanDocument.getPlea() != null && !scanDocument.getPlea().getOffences().isEmpty();
    }

    public void handleScanDocumentAutoActioned(final ScanDocumentAutoActioned scanDocumentAutoActioned) {
        this.stagingBulkScanAggregateMemento.markDocumentAsAutoActioned(scanDocumentAutoActioned.getScanDocumentId(), scanDocumentAutoActioned.getActionedBy(), scanDocumentAutoActioned.getStatusUpdatedDate());
    }

    public Stream<Object> deleteActionedDocument(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        final String zipFileName = this.stagingBulkScanAggregateMemento.getScanEnvelope().getZipFileName();
        final String documentFileName = this.stagingBulkScanAggregateMemento.getScanEnvelope().getAssociatedScanDocuments().stream()
                .filter(document -> document.getScanDocumentId().equals(scanDocumentId))
                .map(ScanDocument::getFileName)
                .findFirst()
                .orElse(null);

        if (isNull(documentFileName)) {
            return Stream.empty();
        }

        return Stream.of(new ActionedDocumentDeleted(scanEnvelopeId, scanDocumentId, zipFileName, documentFileName, now(UTC)));
    }

    public void handleActionedDocumentDelete(final ActionedDocumentDeleted actionedDocumentDeleted) {
        this.stagingBulkScanAggregateMemento.markDocumentAsAutoDelete(actionedDocumentDeleted.getScanDocumentId(), actionedDocumentDeleted.getDeletedDate());
    }

    public Stream<Object> rejectDocument(final UUID scanEnvelopeId, final UUID scanDocumentId, final List<Problem> errors) {
        return Stream.of(new ScanDocumentRejected(errors, now(UTC), scanDocumentId, scanEnvelopeId));
    }

    public Stream<Object> expireDocument(final UUID scanEnvelopeId, final UUID scanDocumentId, final ZonedDateTime expireDate) {
        return Stream.of(new ScanDocumentExpired(expireDate, scanDocumentId, scanEnvelopeId));
    }

    public Stream<Object> raiseDocumentFollowUp(UUID scanEnvelopeId, UUID scanDocumentId) {
        return Stream.of(new ScanDocumentAttachedAndFollowedUp(scanEnvelopeId, scanDocumentId, now(UTC)));
    }

    public Stream<Object> raiseDocumentNextstepDecided(final UUID scanEnvelopeId, final UUID scanDocumentId, final Boolean isSjp) {
        if(isNull( this.stagingBulkScanAggregateMemento.getScanEnvelope())){
            return Stream.of(new DocumentNextStepPaused(scanEnvelopeId, scanDocumentId, isSjp));
        }
        final Stream.Builder<Object> streams = Stream.builder();
        raiseDocumentEvent(this.stagingBulkScanAggregateMemento.getScanEnvelope(),scanEnvelopeId,scanDocumentId,isSjp,streams );
        return streams.build();
    }

    private void raiseDocumentEvent(final ScanEnvelope scanEnvelope,final UUID scanEnvelopeId, final UUID scanDocumentId, final Boolean isSjp, final Stream.Builder<Object> streams) {
        final Optional<String>  caseUrn = scanEnvelope.getAssociatedScanDocuments().stream()
                .filter(doc -> doc.getScanDocumentId().equals(scanDocumentId))
                .map(doc -> isBlank(doc.getCaseUrn()) ? doc.getCasePTIUrn() : doc.getCaseUrn())
                .findFirst();
        caseUrn.ifPresent(urn -> streams.add(new DocumentNextStepDecided(scanEnvelopeId, scanDocumentId, urn,isSjp)));
    }
}