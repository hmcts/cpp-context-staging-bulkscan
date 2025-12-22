package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate.DefendantFinancialMeansDelegate;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate.DefendantPleaDelegate;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate.StagingBulkScanAggregateMemento;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate.StagingBulkScanDelegate;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DocumentNextStepPaused;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class StagingBulkScanAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    private final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    private final StagingBulkScanDelegate stagingBulkScanDelegate;

    private final DefendantFinancialMeansDelegate defendantFinancialMeansDelegate;

    private final DefendantPleaDelegate defendantPleaDelegate;

    public StagingBulkScanAggregate() {
        stagingBulkScanAggregateMemento = new StagingBulkScanAggregateMemento();
        stagingBulkScanDelegate = new StagingBulkScanDelegate(stagingBulkScanAggregateMemento);
        defendantFinancialMeansDelegate = new DefendantFinancialMeansDelegate(stagingBulkScanAggregateMemento);
        defendantPleaDelegate = new DefendantPleaDelegate(stagingBulkScanAggregateMemento);
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ScanEnvelopeRegistered.class).apply(stagingBulkScanDelegate::handleEnvelopeAttribute),
                when(ScanDocumentManuallyActioned.class).apply(stagingBulkScanDelegate::handleScanDocumentManuallyActioned),
                when(ScanDocumentAutoActioned.class).apply(stagingBulkScanDelegate::handleScanDocumentAutoActioned),
                when(ActionedDocumentDeleted.class).apply(stagingBulkScanDelegate::handleActionedDocumentDelete),
                when(DocumentNextStepPaused.class).apply(stagingBulkScanDelegate::handleDocumentPaused),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> register(final ScanEnvelope scanEnvelope) {
        return apply(stagingBulkScanDelegate.register(scanEnvelope));
    }

    public Stream<Object> markAsManuallyActioned(final UUID scanEnvelopeId, final UUID scanDocumentId, final UUID actionedBy) {
        return apply(stagingBulkScanDelegate.markAsManuallyActioned(scanEnvelopeId, scanDocumentId, actionedBy));
    }

    public Stream<Object> markAsAutoActioned(final UUID scanEnvelopeId, final UUID scanDocumentId, final UUID actionedBy) {
        return apply(stagingBulkScanDelegate.markAsAutoActioned(scanEnvelopeId, scanDocumentId, actionedBy));
    }

    public Stream<Object> deleteActionedDocument(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        return apply(stagingBulkScanDelegate.deleteActionedDocument(scanEnvelopeId, scanDocumentId));
    }

    public Stream<Object> rejectDocument(final UUID scanEnvelopeId, final UUID scanDocumentId, List<Problem> errors) {
        return apply(stagingBulkScanDelegate.rejectDocument(scanEnvelopeId, scanDocumentId, errors));
    }

    public Stream<Object> expireDocument(UUID scanEnvelopeId, UUID scanDocumentId, ZonedDateTime expireDate) {
        return apply(stagingBulkScanDelegate.expireDocument(scanEnvelopeId, scanDocumentId, expireDate));
    }

    public Stream<Object> updateDefendantFinancialMeans(final AllFinancialMeans allFinancialMeans, final UUID actionedBy) {
        return apply(defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, actionedBy));
    }

    public Stream<Object> updateDefendantDetails(final Defendant defendantDetails, final UUID actionedBy) {
        return apply(defendantPleaDelegate.updateDefendantDetails(defendantDetails, actionedBy));
    }

    public ScanEnvelope getScanEnvelope() {
        return stagingBulkScanAggregateMemento.getScanEnvelope();
    }

    public Stream<Object> raiseDocumentFollowUp(UUID scanEnvelopeId, UUID scanDocumentId) {
        return apply(stagingBulkScanDelegate.raiseDocumentFollowUp(scanEnvelopeId, scanDocumentId));
    }

    public Stream<Object> raiseDocumentNextstepDecided(UUID scanEnvelopeId, UUID scanDocumentId, Boolean isSjp) {
        return apply(stagingBulkScanDelegate.raiseDocumentNextstepDecided(scanEnvelopeId, scanDocumentId, isSjp));
    }
}
