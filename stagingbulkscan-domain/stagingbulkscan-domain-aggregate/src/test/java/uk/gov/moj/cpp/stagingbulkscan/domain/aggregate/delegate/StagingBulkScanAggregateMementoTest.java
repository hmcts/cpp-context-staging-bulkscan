package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class StagingBulkScanAggregateMementoTest {
    private static final StagingBulkScanAggregateMemento memento = new StagingBulkScanAggregateMemento();
    private static final UUID ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final ZonedDateTime ACTIONED_DATE = ZonedDateTime.now(UTC);
    private static final ZonedDateTime DELETED_DATE = ZonedDateTime.now(UTC);

    @Test
    public void getScanEnvelope() {
        memento.setScanEnvelope(buildScanEnvelope());
        assertNotNull(memento.getScanEnvelope());
    }

    @Test
    public void registerScanEnvelope() {
        memento.registerScanEnvelope(buildScanEnvelope());
        assertEquals(ENVELOPE_ID, memento.getScanEnvelope().getScanEnvelopeId());
        assertEquals(DOCUMENT_ID, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getScanDocumentId());
    }

    @Test
    public void markDocumentAsManuallyActioned() {
        memento.setScanEnvelope(buildScanEnvelope());
        memento.markDocumentAsManuallyActioned(DOCUMENT_ID, ACTIONED_BY, ACTIONED_DATE);
        assertEquals(ENVELOPE_ID, memento.getScanEnvelope().getScanEnvelopeId());
        assertEquals(DOCUMENT_ID, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getScanDocumentId());
        assertEquals(ACTIONED_BY, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getActionedBy());
        assertNotNull(memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getStatusUpdatedDate());
    }

    @Test
    public void markDocumentAsAutoActioned() {
        memento.setScanEnvelope(buildScanEnvelope());
        memento.markDocumentAsAutoActioned(DOCUMENT_ID, ACTIONED_BY, ACTIONED_DATE);
        assertEquals(ENVELOPE_ID, memento.getScanEnvelope().getScanEnvelopeId());
        assertEquals(DOCUMENT_ID, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getScanDocumentId());
        assertEquals(ACTIONED_BY, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getActionedBy());
        assertNotNull(memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getStatusUpdatedDate());
    }

    @Test
    public void markDocumentAsAutoDelete() {
        memento.setScanEnvelope(buildScanEnvelope());
        memento.markDocumentAsAutoDelete(DOCUMENT_ID, DELETED_DATE);
        assertEquals(ENVELOPE_ID, memento.getScanEnvelope().getScanEnvelopeId());
        assertEquals(DOCUMENT_ID, memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getScanDocumentId());
        assertNotNull(memento.getScanEnvelope().getAssociatedScanDocuments().get(0).getDeletedDate());
    }

    private ScanEnvelope buildScanEnvelope() {
        return ScanEnvelope.scanEnvelope()
                .withScanEnvelopeId(ENVELOPE_ID)
                .withAssociatedScanDocuments(
                        Arrays.asList(ScanDocument.scanDocument().withScanDocumentId(DOCUMENT_ID).build())).build();
    }
}