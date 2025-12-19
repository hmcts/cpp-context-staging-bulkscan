package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanEnvelopeRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RegisteredScanEnvelopeEventListenerTest {

    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();

    @Mock
    private ScanEnvelopeRepository scanEnvelopeRepository;

    @InjectMocks
    private RegisteredScanEnvelopeEventListener registeredScanEnvelopeEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Test
    public void shouldSaveScanEnvelope() {

        registeredScanEnvelopeEventListener.saveScanEnvelope(getScanEnvelopeJsonEnvelope());

        final ArgumentCaptor<ScanEnvelope> scanDocumentArgumentCaptor = ArgumentCaptor.forClass(ScanEnvelope.class);

        verify(scanEnvelopeRepository, times(1)).save(scanDocumentArgumentCaptor.capture());
    }

    private JsonEnvelope getScanEnvelopeJsonEnvelope() {
        final ScanDocument scanDocument = ScanDocument.scanDocument()
                .withScanDocumentId(DOCUMENT_ID)
                .withStatus(FOLLOW_UP)
                .withStatusUpdatedDate(ZonedDateTime.now(UTC))
                .build();
        final uk.gov.justice.stagingbulkscan.domain.ScanEnvelope scanEnvelope =
                uk.gov.justice.stagingbulkscan.domain.ScanEnvelope.scanEnvelope()
                        .withScanEnvelopeId(SCAN_ENVELOPE_ID)
                        .withAssociatedScanDocuments(Arrays.asList(scanDocument))
                        .build();
        final ScanEnvelopeRegistered scanEnvelopeRegistered = new ScanEnvelopeRegistered(scanEnvelope);
        return envelopeFrom(getMetadata(), objectToJsonObjectConverter.convert(scanEnvelopeRegistered));
    }

    private Metadata getMetadata() {
        return metadataBuilder().withId(randomUUID()).withName("dummy event name").build();
    }
}
