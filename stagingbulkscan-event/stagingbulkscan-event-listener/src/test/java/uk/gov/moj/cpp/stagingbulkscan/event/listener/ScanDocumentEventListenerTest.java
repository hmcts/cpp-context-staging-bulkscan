package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.CASE_NOT_FOUND;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScanDocumentEventListenerTest {

    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final ZonedDateTime STATUS_UPDATED_DATE = ZonedDateTime.now(UTC);

    @Mock
    private ScanDocumentRepository scanDocumentRepository;

    @InjectMocks
    private ScanDocumentEventListener scanDocumentActionedEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Test
    public void shouldUpdateDocumentAsActioned() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.updateScanDocumentToMarkAsActioned(getDocumentActionedJsonEnvelope());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateDocumentAsAutoActioned() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.updateScanDocumentToMarkAsAutoActioned(getDocumentAutoActionedJsonEnvelope());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateDocumentAsFollowUp() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.handleDocumentRejected(getScanDocumentRejected());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
        final ScanDocument scanDocumentArgumentCaptorValue = scanDocumentArgumentCaptor.getValue();
        assertThat(scanDocumentArgumentCaptorValue.getStatus(), Is.is(FOLLOW_UP));
        assertThat(scanDocumentArgumentCaptorValue.getStatusUpdatedDate().format(DateTimeFormatter.ISO_INSTANT), Is.is(STATUS_UPDATED_DATE.toString()));
    }

    @Test
    public void shouldUpdateDocumentAttachedAsFollowUp() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.handleDocumentAttachedWithFollowUp(getDocumentAttachedAndFollowUpJsonEnvelope());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
        final ScanDocument scanDocumentArgumentCaptorValue = scanDocumentArgumentCaptor.getValue();
        assertThat(scanDocumentArgumentCaptorValue.getStatus(), Is.is(FOLLOW_UP));
        assertThat(scanDocumentArgumentCaptorValue.getStatusUpdatedDate().format(DateTimeFormatter.ISO_INSTANT), Is.is(STATUS_UPDATED_DATE.toString()));
    }

    @Test
    public void shouldUpdateDocumentAsExpired() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.handleDocumentExpired(getScanDocumentExpired());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
        final ScanDocument scanDocumentArgumentCaptorValue = scanDocumentArgumentCaptor.getValue();
        assertThat(scanDocumentArgumentCaptorValue.getStatus(), Is.is(FOLLOW_UP));
        assertThat(scanDocumentArgumentCaptorValue.getStatusCode(), Is.is(CASE_NOT_FOUND));
        assertThat(scanDocumentArgumentCaptorValue.getStatusUpdatedDate().format(DateTimeFormatter.ISO_INSTANT), Is.is(STATUS_UPDATED_DATE.toString()));

    }

    @Test
    public void shouldUpdateDocumentAsFollowUpAfterAutoActioned() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        scanDocumentActionedEventListener.updateScanDocumentToMarkAsAutoActionedWithFollowUp(getDocumentAutoActionedJsonEnvelope());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
        final ScanDocument scanDocumentArgumentCaptorValue = scanDocumentArgumentCaptor.getValue();
        assertThat(scanDocumentArgumentCaptorValue.getStatus(), Is.is(FOLLOW_UP));
    }

    private JsonEnvelope getScanDocumentRejected() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("rejectedDate", STATUS_UPDATED_DATE.toString())
                .build();
        return envelopeFrom((Metadata) null, payload);
    }

    private JsonEnvelope getScanDocumentExpired() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("expiredDate", STATUS_UPDATED_DATE.toString())
                .build();
        return envelopeFrom(getMetadata(), payload);
    }

    private JsonEnvelope getDocumentActionedJsonEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("status", MANUALLY_ACTIONED.toString())
                .add("actionedBy", USER_ID.toString())
                .add("statusUpdatedDate", STATUS_UPDATED_DATE.toString())
                .build();
        return envelopeFrom(getMetadata(), payload);
    }

    private JsonEnvelope getDocumentAttachedAndFollowUpJsonEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("status", MANUALLY_ACTIONED.toString())
                .add("statusUpdatedDate", STATUS_UPDATED_DATE.toString())
                .build();
        return envelopeFrom(getMetadata(), payload);
    }

    private JsonEnvelope getDocumentAutoActionedJsonEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("status", AUTO_ACTIONED.toString())
                .add("actionedBy", USER_ID.toString())
                .add("statusUpdatedDate", STATUS_UPDATED_DATE.toString())
                .build();
        return envelopeFrom(getMetadata(), payload);
    }

    private Metadata getMetadata() {
        return metadataBuilder().withId(randomUUID()).withName("dummy event name").build();
    }
}
