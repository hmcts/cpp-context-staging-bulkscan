package uk.gov.moj.cpp.stagingbulkscan.repository;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class ScanDocumentRepositoryTest extends BaseTransactionalJunit4Test {
    private static final UUID SCAN_DOCUMENT_ID_1 = randomUUID();
    private static final UUID SCAN_DOCUMENT_ID_2 = randomUUID();

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    @Inject
    private ScanEnvelopeRepository scanEnvelopeRepository;

    private ScanEnvelope scanEnvelope;

    @Before
    public void setUp() {
        scanEnvelope = buildScanEnvelope();
    }

    @After
    public void destroy() {
        scanEnvelope = null;
    }

    @Test
    public void shouldGetDocumentsByStatuses() {
        buildActionedDocuments();
        scanEnvelopeRepository.save(scanEnvelope);
        final List<ScanDocument> scanDocumentList =
                scanDocumentRepository.findAllDocumentsByStatusByDesc(Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED));
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getStatus(), is(AUTO_ACTIONED));
        assertThat(scanDocumentList.get(1).getStatus(), is(MANUALLY_ACTIONED));
    }

    @Test
    public void shouldGetFollowUpDocuments() {
        scanEnvelopeRepository.save(scanEnvelope);
        final List<ScanDocument> scanDocumentList = scanDocumentRepository.findAllDocumentsByStatusByAsc(FOLLOW_UP);
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getStatus(), is(FOLLOW_UP));
        assertThat(scanDocumentList.get(1).getStatus(), is(FOLLOW_UP));
    }

    private ScanEnvelope buildScanEnvelope() {
        final UUID scanEnvelopeId = randomUUID();
        final ScanEnvelope scanEnvelope = new ScanEnvelope();
        scanEnvelope.setId(scanEnvelopeId);
        scanEnvelope.setZipFileName("test.zip");
        scanEnvelope.setExtractedDate(now());
        scanEnvelope.setNotes("Sample Notes");

        final Set<ScanDocument> scanDocumentSet = new HashSet<>();
        final ScanDocument scanDocumentOne = new ScanDocument();
        scanDocumentOne.setId(new ScanSnapshotKey(SCAN_DOCUMENT_ID_1, scanEnvelopeId));
        scanDocumentOne.setDocumentFileName("TestDocument1.pdf");
        scanDocumentOne.setVendorReceivedDate(now().plusDays(1));
        scanDocumentOne.setStatus(FOLLOW_UP);

        final ScanDocument scanDocumentTwo = new ScanDocument();
        scanDocumentTwo.setId(new ScanSnapshotKey(SCAN_DOCUMENT_ID_2, scanEnvelopeId));
        scanDocumentTwo.setDocumentFileName("TestDocument2.pdf");
        scanDocumentTwo.setVendorReceivedDate(now());
        scanDocumentTwo.setStatus(FOLLOW_UP);
        scanDocumentSet.add(scanDocumentOne);
        scanDocumentSet.add(scanDocumentTwo);
        scanEnvelope.setAssociatedScanDocuments(scanDocumentSet);
        return scanEnvelope;
    }

    private void buildActionedDocuments() {
        final ScanDocument actionedScanDocumentOne = new ScanDocument();
        actionedScanDocumentOne.setId(new ScanSnapshotKey(randomUUID(), scanEnvelope.getId()));
        actionedScanDocumentOne.setDocumentFileName("TestDocument1.pdf");
        actionedScanDocumentOne.setVendorReceivedDate(now());
        actionedScanDocumentOne.setStatus(MANUALLY_ACTIONED);
        actionedScanDocumentOne.setActionedBy(randomUUID());
        actionedScanDocumentOne.setStatusUpdatedDate(now(UTC));
        actionedScanDocumentOne.setDeleted(false);

        final ScanDocument actionedScanDocumentTwo = new ScanDocument();
        actionedScanDocumentTwo.setId(new ScanSnapshotKey(randomUUID(), scanEnvelope.getId()));
        actionedScanDocumentTwo.setDocumentFileName("TestDocument2.pdf");
        actionedScanDocumentTwo.setVendorReceivedDate(now().plusDays(1));
        actionedScanDocumentTwo.setStatus(AUTO_ACTIONED);
        actionedScanDocumentTwo.setActionedBy(randomUUID());
        actionedScanDocumentTwo.setStatusUpdatedDate(now(UTC));
        actionedScanDocumentTwo.setDeleted(false);

        scanEnvelope.getAssociatedScanDocuments().add(actionedScanDocumentOne);
        scanEnvelope.getAssociatedScanDocuments().add(actionedScanDocumentTwo);
    }
}