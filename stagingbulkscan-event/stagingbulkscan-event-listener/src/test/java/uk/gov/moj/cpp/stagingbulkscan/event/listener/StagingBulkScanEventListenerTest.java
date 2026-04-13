package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanEventListenerTest {

    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();

    @Mock
    private ScanDocumentRepository scanDocumentRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private StagingBulkScanEventListener stagingBulkScanEventListener;

    @Test
    public void shouldUpdateDocumentAsDeleted() {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(DOCUMENT_ID, SCAN_ENVELOPE_ID);
        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(scanSnapshotKey);
        when(scanDocumentRepository.findBy(scanSnapshotKey)).thenReturn(scanDocument);

        stagingBulkScanEventListener.deleteActionedDocumentsAfterLimit(getDocumentToBeDeletedJsonEnvelope());
        final ArgumentCaptor<ScanDocument> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanDocument.class);
        verify(scanDocumentRepository).save(scanDocumentArgumentCaptor.capture());
    }

    private JsonEnvelope getDocumentToBeDeletedJsonEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                .add("scanDocumentId", DOCUMENT_ID.toString())
                .add("deleted", true)
                .add("deletedDate", ZonedDateTime.now(UTC).toString())
                .build();
        return envelopeFrom(getMetadata(), payload);
    }

    private Metadata getMetadata() {
        return metadataBuilder().withId(randomUUID()).withName("dummy event name").build();
    }
}
