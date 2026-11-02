package uk.gov.moj.cpp.stagingbulkscan.repository;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
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

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * On J17 the repositories are DeltaSpike repositories that use the framework's request-scoped EntityManager
 * (EntityManagerProducer). The J25 upgrade replaces them with hand-written classes using a container-managed
 * {@code @PersistenceContext}. Every assertion below encodes the J17 result.
 *
 * Harness note: under HSQLDialect on H2 a UUID-keyed entity cannot be loaded after the persistence context is
 * cleared, so these tests never clear it.
 */
@RunWith(CdiTestRunner.class)
public class ViewstoreRepositoryParityTest extends BaseTransactionalJunit4Test {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final List<DocumentStatus> ACTIONED = asList(MANUALLY_ACTIONED, AUTO_ACTIONED);

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    @Inject
    private ScanEnvelopeRepository scanEnvelopeRepository;

    @Inject
    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @Before
    public void rememberDefaultTimeZone() {
        originalDefaultTimeZone = TimeZone.getDefault();
    }

    @After
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // BSC-P01: DeltaSpike @MaxResults only limits the query when the value is greater than 0. The J25 repository
    // passes it to setMaxResults: 0 returns no rows, a negative value throws IllegalArgumentException.
    @Test
    public void bscP01_findDocumentsEligibleForDeletionShouldIgnoreAMaxResultsOfZeroOrLess() {
        final ScanEnvelope envelope = envelopeWith(actionedDocument(), actionedDocument());
        scanEnvelopeRepository.save(envelope);
        entityManager.flush();
        final ZonedDateTime cutoff = ZonedDateTime.now().plusDays(1);

        assertThat(eligibleForDeletion(envelope, cutoff, 0), hasSize(2));
        assertThat(eligibleForDeletion(envelope, cutoff, -1), hasSize(2));
        assertThat(eligibleForDeletion(envelope, cutoff, 1), hasSize(1));
    }

    // BSC-P02: the framework EntityManagerProducer sets the JVM default time zone to UTC when it creates the
    // EntityManager the repositories use. J25 repositories no longer go through that producer.
    @Test
    public void bscP02_shouldSetJvmDefaultTimeZoneToUtcWhenRepositoryIsUsed() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        scanEnvelopeRepository.findBy(randomUUID());

        assertThat(TimeZone.getDefault().getID(), is("UTC"));
    }

    // BSC-P03: DeltaSpike save() persists a new entity and returns the same, now managed, instance.
    // The J25 save() always merges, which returns a managed copy and leaves the argument detached.
    @Test
    public void bscP03_saveOfNewEnvelopeShouldReturnTheSameManagedInstance() {
        final ScanEnvelope envelope = envelopeWith(actionedDocument());

        final ScanEnvelope saved = scanEnvelopeRepository.save(envelope);

        assertThat(saved, sameInstance(envelope));
        assertThat(entityManager.contains(envelope), is(true));
    }

    // BSC-P04: two documents with the same key in one envelope fail on J17 with a JPA PersistenceException
    // (EntityExistsException). J25 merges instead and fails with IllegalStateException
    // ("Multiple representations of the same entity"), which is not a PersistenceException.
    @Test
    public void bscP04_duplicateDocumentKeyInOneEnvelopeShouldFailWithAPersistenceException() {
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
