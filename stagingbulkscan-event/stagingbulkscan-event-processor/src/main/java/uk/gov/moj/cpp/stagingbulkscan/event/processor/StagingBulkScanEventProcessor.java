package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.CASE_NOT_FOUND;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.DEFENDANT_DETAILS_UPDATED;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.DOCUMENT_ATTACHED_FOLLOWUP_REQUIRED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.BlobClientProvider;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PublicScanDocumentActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.stagingbulkscan.event.exception.DocumentMissingException;

@SuppressWarnings({"squid:S00107", "squid:S1166"})
@ServiceComponent(EVENT_PROCESSOR)
public class StagingBulkScanEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingBulkScanEventProcessor.class);

    private static final String PUBLIC_STAGING_BULK_SCAN_MARK_AS_ACTIONED = "public.stagingbulkscan.mark-as-actioned";
    private static final String QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID = "stagingbulkscan.get-scan-envelope-document-by-ids";
    private static final String PUBLIC_STAGINGBULKSCAN_DOCUMENT_MARKED_FOR_FOLLOW_UP = "public.stagingbulkscan.document-marked-for-follow-up";
    private static final String PUBLIC_STAGINGBULKSCAN_SCAN_ENVELOPE_REGISTERED = "public.stagingbulkscan.scan-envelope-registered";
    private static final String SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String SCAN_ENVELOPE_ID = "scanEnvelopeId";
    private static final char DOT = '.';

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private BlobClientProvider blobClientProvider;

    @Inject
    private Requester requester;

    @Handles("stagingbulkscan.events.mark-as-actioned")
    public void scanDocumentActionedPublicEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.mark-as-actioned event received {}", event.toObfuscatedDebugString());
        }

        final ScanDocumentManuallyActioned scanDocumentActioned = this.jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentManuallyActioned.class);

        final PublicScanDocumentActioned publicScanDocumentActioned = new PublicScanDocumentActioned(scanDocumentActioned.getScanEnvelopeId(), scanDocumentActioned.getScanDocumentId());

        final JsonObject publicEventPayload = this.objectToJsonObjectConverter.convert(publicScanDocumentActioned);

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_STAGING_BULK_SCAN_MARK_AS_ACTIONED).build(), publicEventPayload));
    }


    @Handles("stagingbulkscan.events.scan-envelope-registered")
    public void processScanEnvelopeRegistered(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.scan-envelope-registered {}", event.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PUBLIC_STAGINGBULKSCAN_SCAN_ENVELOPE_REGISTERED).build(), event.payload()));
    }

    @Handles("stagingbulkscan.events.actioned-document-deleted")
    public void deletePhysicalDocumentsFromAzure(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.actioned-document-deleted event received {}", event.toObfuscatedDebugString());
        }
        final ActionedDocumentDeleted deleteEvent =
                this.jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                        ActionedDocumentDeleted.class);

        final String pdfFileName = zipFileNameWithoutExtension(deleteEvent.getZipFileName())
                + File.separator
                + deleteEvent.getDocumentFileName();

        LOGGER.debug("Deleting PDF {} from azure blob store.", pdfFileName);
        //delete pdf file
        blobClientProvider.deleteIfExists(pdfFileName);

        final String pngFileName = zipFileNameWithoutExtension(deleteEvent.getZipFileName())
                + File.separator
                + documentFilenameWithoutExtension(deleteEvent.getDocumentFileName())
                + ".png";

        LOGGER.debug("Deleting IMAGE {} from azure blob store.", pngFileName);
        //delete png file
        blobClientProvider.deleteIfExists(pngFileName);
    }

    @Handles("stagingbulkscan.events.scan-document-rejected")
    public void handleDocumentRejectedEvent(final JsonEnvelope event) {
        markDocumentAsFollowUp(event, null);
    }

    @Handles("stagingbulkscan.events.scan-document-expired")
    public void handleDocumentExpiredEvent(final JsonEnvelope event) {
        markDocumentAsFollowUp(event, CASE_NOT_FOUND.toString());
    }

    @Handles("stagingbulkscan.events.document-auto-actioned-with-follow-up")
    public void handleDocumentAutoActionedEvent(final JsonEnvelope event) {
        markDocumentAsFollowUp(event, DEFENDANT_DETAILS_UPDATED.toString());
    }

    @Handles("stagingbulkscan.events.document-attached-with-follow-up")
    public void handleDocumentAttachedWithFollowUpEvent(final JsonEnvelope event) {
        markDocumentAsFollowUp(event, DOCUMENT_ATTACHED_FOLLOWUP_REQUIRED.toString());
    }

    private void markDocumentAsFollowUp(final JsonEnvelope event, final String statusCode) {
        final JsonObject payload = event.payloadAsJsonObject();
        final Optional<UUID> documentId = getUUID(payload, SCAN_DOCUMENT_ID);
        final Optional<UUID> envelopeId = getUUID(payload, SCAN_ENVELOPE_ID);

        if (documentId.isPresent() && envelopeId.isPresent()) {
            final Envelope<JsonValue> jsonEnvelope = envelopeFrom(
                    metadataFrom(event.metadata()).withName(QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID).build(),
                    createObjectBuilder()
                            .add(SCAN_DOCUMENT_ID, documentId.get().toString())
                            .add(SCAN_ENVELOPE_ID, envelopeId.get().toString())
                            .build());

            final Envelope<JsonObject> envelope = requester.requestAsAdmin(jsonEnvelope, JsonObject.class);

            if (!envelope.payload().containsKey("documentName")) {
                triggerRetryOnDocument(documentId.get().toString());
            }

            final JsonObject responsePayload = enrich(envelope.payload(), "statusCode", statusCode);
            final Optional<UUID> envelopeDocumentId = getUUID(responsePayload, "id");

            if (envelopeDocumentId.isPresent()) {
                sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                                .withName(PUBLIC_STAGINGBULKSCAN_DOCUMENT_MARKED_FOR_FOLLOW_UP).build(),
                        createObjectBuilder()
                                .add("document", responsePayload)
                                .build()));
            }
        }
    }

    private void triggerRetryOnDocument(final String documentId) {
        LOGGER.warn("Document is not found for documentId {}", documentId);
        throw new DocumentMissingException("Document not found, so retrying -->>" + documentId);
    }

    private JsonObject enrich(JsonObject source, String key, String value) {
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        source.entrySet().
                forEach(e -> builder.add(e.getKey(), e.getValue()));
        if (StringUtils.isNotBlank(value)) {
            builder.add(key, value);
        }
        return builder.build();
    }

    private String zipFileNameWithoutExtension(final String zipFileName) {
        final int index =
                zipFileName.contains(Character.toString(DOT)) ? zipFileName.indexOf(DOT) : zipFileName.length();

        return zipFileName.substring(0, index);
    }

    private String documentFilenameWithoutExtension(final String documentFileName) {
        final int index =
                documentFileName.contains(Character.toString(DOT)) ? documentFileName.indexOf(DOT) : documentFileName.length();

        return documentFileName.substring(0, index);
    }
}
