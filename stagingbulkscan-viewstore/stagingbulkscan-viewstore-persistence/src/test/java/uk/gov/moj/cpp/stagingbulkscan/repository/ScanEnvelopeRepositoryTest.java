package uk.gov.moj.cpp.stagingbulkscan.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ScanEnvelopeRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingbulkscan-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private ScanEnvelopeRepository scanEnvelopeRepository;

    private ScanEnvelope scanEnvelope;

    @BeforeEach
    public void setUp() {
        scanEnvelopeRepository = new ScanEnvelopeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(scanEnvelopeRepository);
        scanEnvelope = buildScanEnvelope();
    }

    @Test
    public void shouldSaveScanEnvelope() {
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final ScanEnvelope found = scanEnvelopeRepository.findBy(scanEnvelope.getId());
        assertThat(found.getZipFileName(), is("test.zip"));
        assertThat(found.getNotes(), is("Sample Notes"));
        assertThat(found.getAssociatedScanDocuments().size(), is(1));
        assertThat(found.getAssociatedScanDocuments().iterator().next().getDocumentFileName(), is("TestDocument.pdf"));
    }

    @Test
    public void shouldReturnNullWhenScanEnvelopeNotFound() {
        assertThat(scanEnvelopeRepository.findBy(UUID.randomUUID()), is(nullValue()));
    }

    private void flushAndClear() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        entityManager.flush();
        entityManager.clear();
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
