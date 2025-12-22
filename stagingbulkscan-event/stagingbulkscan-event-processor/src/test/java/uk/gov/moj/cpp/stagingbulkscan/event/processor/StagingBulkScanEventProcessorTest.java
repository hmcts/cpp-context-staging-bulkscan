package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.BlobClientProvider;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PublicScanDocumentActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.stagingbulkscan.event.exception.DocumentMissingException;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanEventProcessorTest {
    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final ZonedDateTime STATUS_UPDATED_DATE = now(UTC);
    private static final ZonedDateTime DELETED_DATE = now(UTC);
    private static final String MARK_AS_ACTIONED_EVENT = "stagingbulkscan.events.mark-as-actioned";
    private static final String DELETE_ACTIONED_DOCUMENTS_EVENT = "stagingbulkscan.events.actioned-document-deleted";
    private static final String PUBLIC_STAGING_BULK_SCAN_MARK_AS_ACTIONED = "public.stagingbulkscan.mark-as-actioned";
    private static final String DOCUMENT_AUTO_ACTIONED_WITH_FOLLOW_UP ="stagingbulkscan.events.document-auto-actioned-with-follow-up";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    private StagingBulkScanEventProcessor stagingBulkScanEventProcessor;

    @Mock
    private BlobClientProvider blobClientProvider;

    @Mock
    private Requester requester;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void processPublicScanDocumentActioned() {

        final ScanDocumentManuallyActioned scanDocumentManuallyActioned = new ScanDocumentManuallyActioned(SCAN_ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY, STATUS_UPDATED_DATE);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(MARK_AS_ACTIONED_EVENT),
                this.objectToJsonObjectConverter.convert(scanDocumentManuallyActioned));

        stagingBulkScanEventProcessor.scanDocumentActionedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final Envelope<?> envelopeOut = this.envelopeArgumentCaptor.getValue();
        final JsonObject jsonObject = (JsonObject) envelopeOut.payload();
        assertThat(envelopeOut.metadata().name(), is(PUBLIC_STAGING_BULK_SCAN_MARK_AS_ACTIONED));
        final PublicScanDocumentActioned publicScanDocumentActioned = jsonObjectToObjectConverter.convert(jsonObject, PublicScanDocumentActioned.class);
        assertThat(publicScanDocumentActioned.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(publicScanDocumentActioned.getScanDocumentId(), is(DOCUMENT_ID));
    }

    @Test
    public void shouldDeletePhysicalDocumentsFromAzure() {
        final String zipFileName = "valid_scan_documents";
        final String documentFileName = STRING.next();

        final ActionedDocumentDeleted actionedDocumentDeleted =
                new ActionedDocumentDeleted(SCAN_ENVELOPE_ID, DOCUMENT_ID, zipFileName, documentFileName, DELETED_DATE);
        final JsonEnvelope event =
                envelopeFrom(metadataWithRandomUUID(DELETE_ACTIONED_DOCUMENTS_EVENT),
                        this.objectToJsonObjectConverter.convert(actionedDocumentDeleted));

        stagingBulkScanEventProcessor.deletePhysicalDocumentsFromAzure(event);

        verify(blobClientProvider).deleteIfExists(zipFileName + File.separator + documentFileName);
    }

    @Test
    public void processDocumentAutoActionedEvent() {

        final ScanDocumentFollowedUp scanDocumentFollowedUp = new ScanDocumentFollowedUp(SCAN_ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY, STATUS_UPDATED_DATE);

        final JsonObject scanDocument = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("documentName", "filename.pdf")
                .build();
        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-scan-envelope-document-by-ids"),
                scanDocument);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DOCUMENT_AUTO_ACTIONED_WITH_FOLLOW_UP),
                this.objectToJsonObjectConverter.convert(scanDocumentFollowedUp));

        stagingBulkScanEventProcessor.handleDocumentAutoActionedEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final Envelope<?> envelopeOut = this.envelopeArgumentCaptor.getValue();
        assertThat(envelopeOut.metadata().name(), is("public.stagingbulkscan.document-marked-for-follow-up"));
    }

    @Test
    public void processDocumentAutoActionedEventWhenDocumentNameEmptyItWillThrowException() {

        final ScanDocumentFollowedUp scanDocumentFollowedUp = new ScanDocumentFollowedUp(SCAN_ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY, STATUS_UPDATED_DATE);

        final JsonObject scanDocument = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .build();
        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-scan-envelope-document-by-ids"),
                scanDocument);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DOCUMENT_AUTO_ACTIONED_WITH_FOLLOW_UP),
                this.objectToJsonObjectConverter.convert(scanDocumentFollowedUp));

        assertThrows(DocumentMissingException.class,
                () -> stagingBulkScanEventProcessor.handleDocumentAutoActionedEvent(event), "Document not found, so retrying -->>" + DOCUMENT_ID);
    }

}
