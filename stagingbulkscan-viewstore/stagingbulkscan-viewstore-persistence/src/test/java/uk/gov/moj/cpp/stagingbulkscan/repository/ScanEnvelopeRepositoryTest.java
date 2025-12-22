package uk.gov.moj.cpp.stagingbulkscan.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.time.ZonedDateTime;
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
public class ScanEnvelopeRepositoryTest extends BaseTransactionalJunit4Test {

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
    public void shouldSaveScanEnvelope() {
        scanEnvelopeRepository.save(scanEnvelope);
        final List<ScanEnvelope> scanEnvelopeList = scanEnvelopeRepository.findAll();
        assertThat(scanEnvelopeList.size(), is(1));
    }

    private ScanEnvelope buildScanEnvelope() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final ScanEnvelope scanEnvelope = new ScanEnvelope();
        scanEnvelope.setId(scanEnvelopeId);
        scanEnvelope.setZipFileName("test.zip");
        scanEnvelope.setExtractedDate(ZonedDateTime.now());
        scanEnvelope.setNotes("Sample Notes");

        Set<ScanDocument> scanDocumentSet = new HashSet<>();
        ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(new ScanSnapshotKey(UUID.randomUUID(), scanEnvelopeId));
        scanDocument.setDocumentFileName("TestDocument.pdf");
        scanDocument.setVendorReceivedDate(ZonedDateTime.now());
        scanDocumentSet.add(scanDocument);
        scanEnvelope.setAssociatedScanDocuments(scanDocumentSet);
        return scanEnvelope;
    }

}