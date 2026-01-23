package uk.gov.moj.cpp.stagingbulkscan.event.service;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StagingBulkScanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingBulkScanService.class);
    private static final String QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID = "stagingbulkscan.get-scan-document-by-id";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getScanDocumentById(final JsonEnvelope envelope, final UUID scanEnvelopeId, final UUID scanDocumentId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("retrieve scanDocument from stagingBulkScan for envelopeId {} and scanDocumentId {}", scanEnvelopeId, scanDocumentId);
        }
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .add("scanDocumentId", scanDocumentId.toString())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName(QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID), payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(jsonEnvelope, JsonObject.class);
        return response.payload();
    }

}
