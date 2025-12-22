package uk.gov.moj.cpp.stagingbulkscan.command.handler;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.command.ExpireDocument;
import uk.gov.justice.stagingbulkscan.command.RejectDocument;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.AutoActionDocument;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.StagingBulkScanAggregate;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class StagingBulkScanCommandHandler extends AbstractCommandHandler {


    private static final Logger LOGGER = LoggerFactory.getLogger(StagingBulkScanCommandHandler.class.getName());
    private static final String SCAN_ENVELOPE_ID = "scanEnvelopeId";
    private static final String SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String IS_SJP = "isSjp";

    @Handles("stagingbulkscan.command.register-scan-envelope")
    public void registerEnvelope(final Envelope<ScanEnvelope> envelope) throws EventStreamException {

        final ScanEnvelope scanEnvelope = envelope.payload();
        aggregate(StagingBulkScanAggregate.class, scanEnvelope.getScanEnvelopeId(), envelope, a -> a.register(scanEnvelope));
    }

    @Handles("stagingbulkscan.command.mark-as-action")
    public void markAsAction(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.command.mark-as-action command received {}",
                    command.toObfuscatedDebugString());
        }

        final UUID scanEnvelopeId = fromString(command.payloadAsJsonObject().getString(SCAN_ENVELOPE_ID));
        final UUID scanDocumentId = fromString(command.payloadAsJsonObject().getString(SCAN_DOCUMENT_ID));
        final UUID actionedBy = getUserId(command);

        aggregate(StagingBulkScanAggregate.class, scanEnvelopeId, command, a -> a.markAsManuallyActioned(scanEnvelopeId, scanDocumentId, actionedBy));
    }

    @Handles("stagingbulkscan.command.delete-actioned-document")
    public void deleteActionedDocuments(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.command.delete-actioned-document command received {}",
                    command.toObfuscatedDebugString());
        }
        final UUID scanEnvelopeId = UUID.fromString(command.payloadAsJsonObject().getString(SCAN_ENVELOPE_ID));
        final UUID scanDocumentId = UUID.fromString(command.payloadAsJsonObject().getString(SCAN_DOCUMENT_ID));

        aggregate(StagingBulkScanAggregate.class, scanEnvelopeId, command, a -> a.deleteActionedDocument(scanEnvelopeId, scanDocumentId));
    }

    @Handles("stagingbulkscan.command.auto-action-scan-document")
    public void autoActionDocument(final Envelope<AutoActionDocument> envelope) throws EventStreamException {

        final AutoActionDocument autoActionDocument = envelope.payload();

        aggregate(StagingBulkScanAggregate.class, autoActionDocument.getScanEnvelopeId(), envelope,
                a -> a.markAsAutoActioned(autoActionDocument.getScanEnvelopeId(), autoActionDocument.getScanDocumentId(), autoActionDocument.getActionedBy()));
    }

    @Handles("stagingbulkscan.command.reject-document")
    public void rejectDocument(final Envelope<RejectDocument> envelope) throws EventStreamException {
        final RejectDocument payload = envelope.payload();
        final UUID scanEnvelopeId = payload.getScanEnvelopeId();
        final UUID scanDocumentId = payload.getScanDocumentId();

        aggregate(StagingBulkScanAggregate.class, scanEnvelopeId, envelope,
                a -> a.rejectDocument(scanEnvelopeId, scanDocumentId, payload.getErrors()));
    }

    @Handles("stagingbulkscan.command.expire-document")
    public void expireDocument(final Envelope<ExpireDocument> envelope) throws EventStreamException {
        final ExpireDocument payload = envelope.payload();

        aggregate(StagingBulkScanAggregate.class, payload.getScanEnvelopeId(), envelope,
                a -> a.expireDocument(payload.getScanEnvelopeId(), payload.getScanDocumentId(), payload.getExpireDate()));
    }

    @Handles("stagingbulkscan.command.update-defendant-financial-means")
    public void updateDefendantFinancialMeans(final Envelope<AllFinancialMeans> envelope) throws EventStreamException {
        final AllFinancialMeans allFinancialMeans = envelope.payload();

        aggregate(StagingBulkScanAggregate.class, allFinancialMeans.getScanEnvelopeId(), envelope,
                a -> a.updateDefendantFinancialMeans(allFinancialMeans, getUserId(envelope)));
    }

    @Handles("stagingbulkscan.command.update-defendant-additional-details")
    public void updateDefendantDetails(final Envelope<Defendant> envelope) throws EventStreamException {

        final Defendant defendantDetails = envelope.payload();

        aggregate(StagingBulkScanAggregate.class, defendantDetails.getScanEnvelopeId(), envelope,
                a -> a.updateDefendantDetails(defendantDetails, getUserId(envelope)));
    }

    @Handles("stagingbulkscan.command.raise-document-follow-up")
    public void raiseDocumentFollowUpEvent(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.command.raise-document-follow-up command received {}",
                    command.toObfuscatedDebugString());
        }

        final UUID scanEnvelopeId = fromString(command.payloadAsJsonObject().getString(SCAN_ENVELOPE_ID));
        final UUID scanDocumentId = fromString(command.payloadAsJsonObject().getString(SCAN_DOCUMENT_ID));

        aggregate(StagingBulkScanAggregate.class, scanEnvelopeId, command, a -> a.raiseDocumentFollowUp(scanEnvelopeId, scanDocumentId));
    }

    @Handles("stagingbulkscan.command.decide-document-next-step")
    public void raiseNextStepDecidedUpEvent(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.command.decide-document-next-step received {}",
                    command.toObfuscatedDebugString());
        }

        final UUID scanEnvelopeId = fromString(command.payloadAsJsonObject().getString(SCAN_ENVELOPE_ID));
        final UUID scanDocumentId = fromString(command.payloadAsJsonObject().getString(SCAN_DOCUMENT_ID));
        final Boolean isSjp = command.payloadAsJsonObject().getBoolean(IS_SJP);

        aggregate(StagingBulkScanAggregate.class, scanEnvelopeId, command, a -> a.raiseDocumentNextstepDecided(scanEnvelopeId, scanDocumentId, isSjp));
    }
}
