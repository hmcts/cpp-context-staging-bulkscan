package uk.gov.moj.cpp.stagingbulkscan.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class StagingBulkScanCommandApi {

    private final Sender sender;

    @Inject
    public StagingBulkScanCommandApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("stagingbulkscan.register-scan-envelope")
    public void registerScanEnvelope(final JsonEnvelope envelope) {
        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("stagingbulkscan.command.register-scan-envelope").build(),
                envelope.payloadAsJsonObject()));
    }

    @Handles("stagingbulkscan.mark-as-action")
    public void markAsAction(final JsonEnvelope envelope) {
        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("stagingbulkscan.command.mark-as-action").build(),
                envelope.payloadAsJsonObject()));
    }
}
