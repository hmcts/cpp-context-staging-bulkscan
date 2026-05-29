package uk.gov.moj.cpp.stagingbulkscan.it;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.PENDING;
import static uk.gov.moj.cpp.stagingbulkscan.utils.CommandUtil.actionScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.CommandUtil.registerScanEnvelope;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchActionedScanDocuments;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchDocumentsForDeletion;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchDocumentsForDeletionContaining;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchPendingScanDocumentsContains;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.buildPayloadWithAllValues;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.buildScanDocumentPayload;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class DocumentDeletionQueryIT extends BaseIntegrationTest {

    private static final String SINGLE_PLEA = "Single Justice Procedure Notice - Plea (Single)";

    @Test
    public void shouldReturnActionedDocumentEligibleForDeletion() {
        final String documentFileName = STRING.next();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileName, STRING.next(), PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse pending = fetchPendingScanDocumentsContains(documentFileName);
        final ScanDocument scanDocument = findDocument(pending, documentFileName);
        final UUID envelopeId = scanDocument.getScanEnvelopeId();
        final UUID documentId = scanDocument.getId();

        actionScanDocument(envelopeId, buildScanDocumentPayload(documentId));

        // confirm it's actioned before querying the deletion endpoint
        fetchActionedScanDocuments(documentFileName);

        // cutoffDate in the future → document statusUpdatedDate is before cutoff → eligible
        final ScanDocumentsResponse eligible = fetchDocumentsForDeletionContaining(
                documentId, ZonedDateTime.now(UTC).plusDays(1), 1000);

        assertThat(eligible.getScanDocuments().stream()
                .anyMatch(doc -> doc.getId().equals(documentId)), is(true));
    }

    @Test
    public void shouldNotReturnDocumentActionedAfterCutoffDate() {
        final String documentFileName = STRING.next();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileName, STRING.next(), PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse pending = fetchPendingScanDocumentsContains(documentFileName);
        final ScanDocument scanDocument = findDocument(pending, documentFileName);
        final UUID envelopeId = scanDocument.getScanEnvelopeId();
        final UUID documentId = scanDocument.getId();

        actionScanDocument(envelopeId, buildScanDocumentPayload(documentId));

        // confirm it's actioned so statusUpdatedDate is set to approximately now
        fetchActionedScanDocuments(documentFileName);

        // cutoffDate in the past → document statusUpdatedDate is after cutoff → not eligible
        final ScanDocumentsResponse result = fetchDocumentsForDeletion(
                ZonedDateTime.now(UTC).minusDays(1), 1000);

        assertThat(result.getScanDocuments().stream()
                .anyMatch(doc -> doc.getId().equals(documentId)), is(false));
    }

    @Test
    public void shouldRespectBatchSizeLimitInDeletionQuery() {
        // Action two documents so there is at least something to limit
        final String docFileNameOne = STRING.next();
        final String docFileNameTwo = STRING.next();

        registerScanEnvelope(buildPayloadWithAllValues(docFileNameOne, docFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse pending = fetchPendingScanDocumentsContains(docFileNameOne);

        final ScanDocument docOne = findDocument(pending, docFileNameOne);
        final ScanDocument docTwo = findDocument(pending, docFileNameTwo);

        actionScanDocument(docOne.getScanEnvelopeId(), buildScanDocumentPayload(docOne.getId()));
        actionScanDocument(docTwo.getScanEnvelopeId(), buildScanDocumentPayload(docTwo.getId()));

        // wait until both are actioned
        fetchActionedScanDocuments(docFileNameOne);
        fetchActionedScanDocuments(docFileNameTwo);

        // batchSize=1 → at most 1 document regardless of how many are eligible
        final ScanDocumentsResponse limitedResult = fetchDocumentsForDeletion(
                ZonedDateTime.now(UTC).plusDays(1), 1);

        assertThat(limitedResult.getScanDocuments().size(), lessThanOrEqualTo(1));

        // batchSize=2 → at most 2 documents, but at least the two we just actioned should now be eligible
        final ScanDocumentsResponse twoResult = fetchDocumentsForDeletion(
                ZonedDateTime.now(UTC).plusDays(1), 2);

        assertThat(twoResult.getScanDocuments().size(), greaterThanOrEqualTo(1));
        assertThat(twoResult.getScanDocuments().size(), lessThanOrEqualTo(2));
    }

    private ScanDocument findDocument(final ScanDocumentsResponse response, final String documentFileName) {
        final Optional<ScanDocument> found = response.getScanDocuments().stream()
                .filter(doc -> doc.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();
        return found.orElseThrow(() ->
                new AssertionError("Document not found in response: " + documentFileName));
    }
}
