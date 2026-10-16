package uk.gov.moj.cpp.stagingbulkscan.repository;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * The same scenarios exist on the J17 copy (main, DeltaSpike repositories, CdiTestRunner). Here the hand-written J25
 * repositories run on the shared test persistence unit with the EntityManager injected directly. Every assertion
 * encodes the J17 result, so a red test is a behaviour change introduced by the upgrade.
 */
class ViewstoreRepositoryParityTest {

    private static final String PERSISTENCE_UNIT = "stagingbulkscan-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final List<DocumentStatus> ACTIONED = asList(MANUALLY_ACTIONED, AUTO_ACTIONED);

    private ScanDocumentRepository scanDocumentRepository;
    private ScanEnvelopeRepository scanEnvelopeRepository;
    private EntityManager entityManager;
    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    void setUp() {
        originalDefaultTimeZone = TimeZone.getDefault();
        scanDocumentRepository = new ScanDocumentRepository();
        scanEnvelopeRepository = new ScanEnvelopeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(scanDocumentRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(scanEnvelopeRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
    }

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // BSC-P01: on J17 DeltaSpike @MaxResults only limits the query when the value is greater than 0. The J25 repository
    // passes it to setMaxResults: 0 returns no rows, a negative value throws IllegalArgumentException.
    @Test
    void bscP01_findDocumentsEligibleForDeletionShouldIgnoreAMaxResultsOfZeroOrLess() {
        final ScanEnvelope envelope = envelopeWith(actionedDocument(), actionedDocument());
        scanEnvelopeRepository.save(envelope);
        entityManager.flush();
        final ZonedDateTime cutoff = ZonedDateTime.now().plusDays(1);

        assertThat(eligibleForDeletion(envelope, cutoff, 0), hasSize(2));
        assertThat(eligibleForDeletion(envelope, cutoff, -1), hasSize(2));
        assertThat(eligibleForDeletion(envelope, cutoff, 1), hasSize(1));
    }

    // BSC-P02: on J17 the framework EntityManagerProducer sets the JVM default time zone to UTC when it creates the
    // EntityManager the repositories use. J25 repositories no longer go through that producer.
    @Test
    void bscP02_shouldSetJvmDefaultTimeZoneToUtcWhenRepositoryIsUsed() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        scanEnvelopeRepository.findBy(randomUUID());

        assertThat(TimeZone.getDefault().getID(), is("UTC"));
    }

    // BSC-P03: on J17 DeltaSpike save() persists a new entity and returns the same, now managed, instance.
    // The J25 save() always merges, which returns a managed copy and leaves the argument detached.
    @Test
    void bscP03_saveOfNewEnvelopeShouldReturnTheSameManagedInstance() {
        final ScanEnvelope envelope = envelopeWith(actionedDocument());

        final ScanEnvelope saved = scanEnvelopeRepository.save(envelope);

        assertThat(saved, sameInstance(envelope));
        assertThat(entityManager.contains(envelope), is(true));
    }

    // BSC-P04: two documents with the same key in one envelope fail on J17 with a JPA PersistenceException
    // (EntityExistsException). J25 merges instead and fails with IllegalStateException
    // ("Multiple representations of the same entity"), which is not a PersistenceException.
    @Test
    void bscP04_duplicateDocumentKeyInOneEnvelopeShouldFailWithAPersistenceException() {
        final UUID envelopeId = randomUUID();
        final UUID documentId = randomUUID();
        final ScanEnvelope envelope = envelope(envelopeId);
        envelope.getAssociatedScanDocuments().add(document(envelopeId, documentId, "first.pdf"));
        envelope.getAssociatedScanDocuments().add(document(envelopeId, documentId, "second.pdf"));

        RuntimeException thrown = null;
        try {
            scanEnvelopeRepository.save(envelope);
            entityManager.flush();
        } catch (final RuntimeException e) {
            thrown = e;
        }

        assertThat(thrown, instanceOf(PersistenceException.class));
    }

    private List<ScanDocument> eligibleForDeletion(final ScanEnvelope envelope, final ZonedDateTime cutoff, final int maxResults) {
        final List<ScanDocument> eligible = scanDocumentRepository.findDocumentsEligibleForDeletion(ACTIONED, cutoff, maxResults);
        eligible.removeIf(document -> !document.getId().getScanEnvelopeId().equals(envelope.getId()));
        return eligible;
    }

    private ScanEnvelope envelopeWith(final ScanDocument... documents) {
        final ScanEnvelope envelope = envelope(documents[0].getId().getScanEnvelopeId());
        envelope.getAssociatedScanDocuments().addAll(asList(documents));
        return envelope;
    }

    private UUID currentEnvelopeId;

    private ScanDocument actionedDocument() {
        if (currentEnvelopeId == null) {
            currentEnvelopeId = randomUUID();
        }
        final ScanDocument document = document(currentEnvelopeId, randomUUID(), "actioned.pdf");
        document.setStatus(MANUALLY_ACTIONED);
        document.setActionedBy(randomUUID());
        document.setStatusUpdatedDate(ZonedDateTime.now().minusDays(10));
        document.setDeleted(false);
        return document;
    }

    private static ScanEnvelope envelope(final UUID envelopeId) {
        final ScanEnvelope envelope = new ScanEnvelope();
        envelope.setId(envelopeId);
        envelope.setZipFileName("parity.zip");
        envelope.setExtractedDate(ZonedDateTime.now());
        envelope.setAssociatedScanDocuments(new HashSet<>());
        return envelope;
    }

    private static ScanDocument document(final UUID envelopeId, final UUID documentId, final String fileName) {
        final ScanDocument document = new ScanDocument();
        document.setId(new ScanSnapshotKey(documentId, envelopeId));
        document.setDocumentFileName(fileName);
        document.setVendorReceivedDate(ZonedDateTime.now());
        document.setStatus(MANUALLY_ACTIONED);
        return document;
    }
}
