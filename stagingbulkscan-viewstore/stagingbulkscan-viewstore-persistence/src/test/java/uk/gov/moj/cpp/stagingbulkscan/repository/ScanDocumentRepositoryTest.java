package uk.gov.moj.cpp.stagingbulkscan.repository;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ScanDocumentRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingbulkscan-test-persistence-unit";

    private static final UUID SCAN_DOCUMENT_ID_1 = randomUUID();
    private static final UUID SCAN_DOCUMENT_ID_2 = randomUUID();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private ScanDocumentRepository scanDocumentRepository;

    private ScanEnvelopeRepository scanEnvelopeRepository;

    private ScanEnvelope scanEnvelope;

    @BeforeEach
    public void setUp() {
        scanDocumentRepository = new ScanDocumentRepository();
        scanEnvelopeRepository = new ScanEnvelopeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(scanDocumentRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(scanEnvelopeRepository);
        scanEnvelope = buildScanEnvelope();
    }

    @Test
    public void shouldGetDocumentsByStatuses() {
        buildActionedDocuments();
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final List<ScanDocument> scanDocumentList =
                scanDocumentRepository.findAllDocumentsByStatusByDesc(Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED));
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getStatus(), is(AUTO_ACTIONED));
        assertThat(scanDocumentList.get(1).getStatus(), is(MANUALLY_ACTIONED));
    }

    @Test
    public void shouldGetDocumentsByStatusesInAscendingOrder() {
        buildActionedDocuments();
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final List<ScanDocument> scanDocumentList =
                scanDocumentRepository.findAllDocumentsByStatusByAsc(Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED));
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getStatus(), is(MANUALLY_ACTIONED));
        assertThat(scanDocumentList.get(1).getStatus(), is(AUTO_ACTIONED));
    }

    @Test
    public void shouldGetFollowUpDocuments() {
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final List<ScanDocument> scanDocumentList = scanDocumentRepository.findAllDocumentsByStatusByAsc(FOLLOW_UP);
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getId().getId(), is(SCAN_DOCUMENT_ID_2));
        assertThat(scanDocumentList.get(1).getId().getId(), is(SCAN_DOCUMENT_ID_1));
    }

    @Test
    public void shouldGetFollowUpDocumentsInDescendingOrder() {
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final List<ScanDocument> scanDocumentList = scanDocumentRepository.findAllDocumentsByStatusByDesc(FOLLOW_UP);
        assertThat(scanDocumentList.size(), is(2));
        assertThat(scanDocumentList.get(0).getId().getId(), is(SCAN_DOCUMENT_ID_1));
        assertThat(scanDocumentList.get(1).getId().getId(), is(SCAN_DOCUMENT_ID_2));
    }

    @Test
    public void shouldExcludeDeletedDocumentsWhenFindingByStatus() {
        scanEnvelope.getAssociatedScanDocuments().forEach(document -> document.setDeleted(true));
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        assertThat(scanDocumentRepository.findAllDocumentsByStatusByAsc(FOLLOW_UP), is(empty()));
        assertThat(scanDocumentRepository.findAllDocumentsByStatusByDesc(List.of(FOLLOW_UP)), is(empty()));
    }

    @Test
    public void shouldFindAndUpdateScanDocument() {
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final ScanSnapshotKey key = new ScanSnapshotKey(SCAN_DOCUMENT_ID_1, scanEnvelope.getId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(key);
        assertThat(scanDocument.getDocumentFileName(), is("TestDocument1.pdf"));

        scanDocument.setStatus(MANUALLY_ACTIONED);
        scanDocumentRepository.save(scanDocument);
        flushAndClear();

        assertThat(scanDocumentRepository.findBy(key).getStatus(), is(MANUALLY_ACTIONED));
    }

    @Test
    public void shouldReturnNullWhenScanDocumentNotFound() {
        assertThat(scanDocumentRepository.findBy(new ScanSnapshotKey(randomUUID(), randomUUID())), is(nullValue()));
    }

    @Test
    public void shouldFindScanDocumentsByCaseUrnOrCasePtiUrn() {
        final ScanDocument[] documents = scanEnvelope.getAssociatedScanDocuments().toArray(new ScanDocument[0]);
        documents[0].setCaseUrn("CASEURN1");
        documents[1].setCasePTIUrn("PTIURN2");
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        assertThat(scanDocumentRepository.findScanDocumentStatus("CASEURN1", null).size(), is(1));
        assertThat(scanDocumentRepository.findScanDocumentStatus(null, "PTIURN2").size(), is(1));
        assertThat(scanDocumentRepository.findScanDocumentStatus("CASEURN1", "PTIURN2").size(), is(2));
        assertThat(scanDocumentRepository.findScanDocumentStatus("UNKNOWN", "UNKNOWN"), is(empty()));
    }

    @Test
    public void shouldFindDocumentsEligibleForDeletion() {
        final ZonedDateTime cutoffDate = now(UTC);
        final UUID oldestId = addActionedDocument(MANUALLY_ACTIONED, cutoffDate.minusDays(3), false);
        final UUID olderId = addActionedDocument(AUTO_ACTIONED, cutoffDate.minusDays(2), false);
        addActionedDocument(AUTO_ACTIONED, cutoffDate.minusDays(1), true);
        addActionedDocument(MANUALLY_ACTIONED, cutoffDate.plusDays(1), false);
        scanEnvelopeRepository.save(scanEnvelope);
        flushAndClear();

        final List<ScanDocument> eligible = scanDocumentRepository.findDocumentsEligibleForDeletion(
                Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED), cutoffDate, 10);
        assertThat(eligible.size(), is(2));
        assertThat(eligible.get(0).getId().getId(), is(oldestId));
        assertThat(eligible.get(1).getId().getId(), is(olderId));

        final List<ScanDocument> limited = scanDocumentRepository.findDocumentsEligibleForDeletion(
                Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED), cutoffDate, 1);
        assertThat(limited.size(), is(1));
        assertThat(limited.get(0).getId().getId(), is(oldestId));

        final List<ScanDocument> autoActionedOnly = scanDocumentRepository.findDocumentsEligibleForDeletion(
                List.of(AUTO_ACTIONED), cutoffDate, 10);
        assertThat(autoActionedOnly.stream().map(document -> document.getId().getId()).toList(), containsInAnyOrder(olderId));
    }

    private void flushAndClear() {
        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        entityManager.flush();
        entityManager.clear();
    }

    private UUID addActionedDocument(final DocumentStatus status,
                                     final ZonedDateTime statusUpdatedDate,
                                     final boolean deleted) {
        final UUID documentId = randomUUID();
        final ScanDocument document = new ScanDocument();
        document.setId(new ScanSnapshotKey(documentId, scanEnvelope.getId()));
        document.setDocumentFileName("Actioned.pdf");
        document.setVendorReceivedDate(now());
        document.setStatus(status);
        document.setStatusUpdatedDate(statusUpdatedDate);
        document.setDeleted(deleted);
        scanEnvelope.getAssociatedScanDocuments().add(document);
        return documentId;
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
