package uk.gov.moj.cpp.stagingbulkscan.event.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.DEFENDANT_DETAILS_UPDATED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanServiceTest {
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private StagingBulkScanService stagingBulkScanService;

    @Test
    public void shouldGetScanDocumentById() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID scanEnvelopeId = UUID.randomUUID();
        final JsonObject scanDocument = JsonObjects.createObjectBuilder()
                .add("status", "PENDING")
                .add("caseUrn", "12345")
                .add("statusCode", DEFENDANT_DETAILS_UPDATED.toString())
                .build();
        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-scan-document-by-id"),
                scanDocument);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .add("scanDocumentId", scanDocumentId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);
        final JsonObject scanDocumentById = stagingBulkScanService.getScanDocumentById(jsonEnvelope, scanEnvelopeId, scanDocumentId);
        assertThat(scanDocumentById.getString("caseUrn"), CoreMatchers.is("12345"));
        assertThat(scanDocumentById.getString("status"), CoreMatchers.is("PENDING"));
        assertThat(scanDocumentById.getString("statusCode"), CoreMatchers.is(DEFENDANT_DETAILS_UPDATED.toString()));
    }
}