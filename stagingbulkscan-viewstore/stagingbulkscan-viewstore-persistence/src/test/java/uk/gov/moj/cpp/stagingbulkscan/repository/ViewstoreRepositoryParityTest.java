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
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * The same scenarios exist on the J17 copy (main, DeltaSpike repositories, CdiTestRunner). Here the hand-written J25
 * repositories run on the shared test persistence unit with the EntityManager injected directly. Every assertion
 * encodes the J17 result, so a red test is a behaviour change introduced by the upgrade. Where J25 differs by design
 * (the JVM default time zone, the identity returned by save(), the exception for a duplicate key), the test pins the
 * accepted J25 behaviour and says why.
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
    // keeps that: a deletionBatchSize of 0 or less must not stop deletions (setMaxResults(0) returns no rows).
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

    // BSC-P02: on J17 the framework EntityManagerProducer set the JVM default time zone to UTC when it created the
    // EntityManager the repositories used. J25 repositories no longer go through that producer, so using them leaves
    // the JVM default time zone unchanged and the server must run in UTC itself.
    @Test
    void bscP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        scanEnvelopeRepository.findBy(randomUUID());

        assertThat(TimeZone.getDefault().getID(), is("Europe/London"));
    }

    // BSC-P03: save() of a new envelope returns a managed entity with the same field values. DeltaSpike (J17) returned
    // the same instance; the J25 merge() returns a managed copy. No caller relies on the identity.
    @Test
    void bscP03_saveOfNewEnvelopeShouldReturnAManagedEntityWithTheSameFields() {
        final ScanEnvelope envelope = envelopeWith(actionedDocument());

        final ScanEnvelope saved = scanEnvelopeRepository.save(envelope);

        assertThat(saved, samePropertyValuesAs(envelope, "associatedScanDocuments"));
        assertThat(saved.getAssociatedScanDocuments(), hasSize(1));
        assertThat(entityManager.contains(saved), is(true));
    }

    // BSC-P04: two documents with the same key in one envelope still fail. J17 threw a JPA PersistenceException
    // (EntityExistsException); the J25 merge() fails with IllegalStateException ("Multiple representations of the same
    // entity"), which is not a PersistenceException. Both fail loudly, so the J25 exception is accepted.
    @Test
    void bscP04_duplicateDocumentKeyInOneEnvelopeShouldFailWithAnIllegalStateException() {
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

        assertThat(thrown, instanceOf(IllegalStateException.class));
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
