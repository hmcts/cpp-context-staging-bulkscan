package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentExpired;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentRejected;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAttachedAndFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.ProblemValue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StagingBulkScanDelegateTest {

    private static final String VENDOR_POBOX = "19853";
    private static final String ZIP_FILE_NAME = "ABC.zip";
    private static final String PDF_NAME = "XYZ.pdf";
    private static final String ENVELOP_CLASSIFICATION = "ENVELOP_CLASSIFICATION";
    private static final String CASE_PTI_URN = "CASE_PTI_URN";
    private static final String CASE_URN = "CASE_URN";
    private static final String DOCUMENT_CONTROL_NUMBER = "789798798";
    private static final String DOC_NAME = "DOC_NAME";
    private static final String MANUAL_INTERVENTION = "NO";
    private static final String NEXT_ACTION = "NA";
    private static final String THIS_IS_TEST = "This is test";
    private static final String NOTES = THIS_IS_TEST;
    private static final String PROSECUTOR_AUTHORITY_ID = "SJP";
    private static final String PROSECUTOR_AUTHORITY_CODE = "TVL";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String DOC_NOTES = "TEST_NOTES";
    private static final UUID ENVELOPE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final ZonedDateTime ACTIONED_DATE = ZonedDateTime.now(UTC);
    private static final ZonedDateTime DELETED_DATE = ZonedDateTime.now(UTC);
    private static final ZonedDateTime EXPIRED_DATE = ZonedDateTime.now(UTC);

    private StagingBulkScanDelegate stagingBulkScanDelegate;

    private StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    @BeforeEach
    public void setup() {
        stagingBulkScanAggregateMemento = new StagingBulkScanAggregateMemento();
        stagingBulkScanDelegate = new StagingBulkScanDelegate(stagingBulkScanAggregateMemento);
    }

    @Test
    public void register() {
        final ScanEnvelope scanEnvelope = buildPayload();
        final Stream<Object> eventStream = stagingBulkScanDelegate.register(scanEnvelope);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(ScanEnvelopeRegistered.class),
                Matchers.<ScanEnvelopeRegistered>hasProperty("scanEnvelope", is(scanEnvelope)),
                Matchers.<ScanEnvelopeRegistered>hasProperty("scanEnvelope", hasProperty("scanEnvelopeId", is(scanEnvelope.getScanEnvelopeId()))))));
    }

    @Test
    public void handleEnvelopeAttribute() {
        final ScanEnvelope scanEnvelope = buildPayload();
        final ScanEnvelopeRegistered scanEnvelopeRegistered = new ScanEnvelopeRegistered(scanEnvelope);

        stagingBulkScanDelegate.handleEnvelopeAttribute(scanEnvelopeRegistered);

        final ScanEnvelope expected = stagingBulkScanAggregateMemento.getScanEnvelope();

        assertThat(expected, allOf(
                Matchers.instanceOf(ScanEnvelope.class),
                Matchers.hasProperty("scanEnvelopeId", is(scanEnvelope.getScanEnvelopeId()))));
    }

    @Test
    public void markAsManuallyActioned() {
        final Stream<Object> eventStream = stagingBulkScanDelegate.markAsManuallyActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY);

        final ScanDocumentManuallyActioned scanDocumentManuallyActioned = (ScanDocumentManuallyActioned) eventStream.findFirst().get();

        assertThat(scanDocumentManuallyActioned, allOf(
                Matchers.instanceOf(ScanDocumentManuallyActioned.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID))
        ));
    }

    @Test
    public void handleScanDocumentManuallyActioned() {
        final ScanDocumentManuallyActioned scanDocumentManuallyActioned = new ScanDocumentManuallyActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY, ACTIONED_DATE);
        stagingBulkScanAggregateMemento.setScanEnvelope(buildPayload());
        stagingBulkScanDelegate.handleScanDocumentManuallyActioned(scanDocumentManuallyActioned);

        final ScanEnvelope expected = stagingBulkScanAggregateMemento.getScanEnvelope();

        assertThat(expected.getScanEnvelopeId(), is(ENVELOPE_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getScanDocumentId(), is(DOCUMENT_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getActionedBy(), is(ACTIONED_BY));
        assertThat(expected.getAssociatedScanDocuments().get(0).getStatusUpdatedDate(), notNullValue());
    }

    @Test
    public void markAsAutoActioned() {
        final ScanEnvelope scanEnvelope = buildPayload();
        stagingBulkScanAggregateMemento.setScanEnvelope(scanEnvelope);
        final Stream<Object> eventStream = stagingBulkScanDelegate.markAsAutoActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY);

        final ScanDocumentAutoActioned scanDocumentAutoActioned = (ScanDocumentAutoActioned) eventStream.findFirst().get();

        assertThat(scanDocumentAutoActioned, allOf(
                Matchers.instanceOf(ScanDocumentAutoActioned.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID))
        ));
    }

    @Test
    public void markAsAutoActionedWithCasePtiUrn() {
        final ScanEnvelope scanEnvelope = buildPayloadPtiUrn();
        stagingBulkScanAggregateMemento.setScanEnvelope(scanEnvelope);
        final Stream<Object> eventStream = stagingBulkScanDelegate.markAsAutoActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY);
        final List<Object> events = eventStream.collect(toList());

        final ScanDocumentAutoActioned scanDocumentAutoActioned = (ScanDocumentAutoActioned) events.get(0);

        assertThat(scanDocumentAutoActioned, allOf(
                Matchers.instanceOf(ScanDocumentAutoActioned.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID))
        ));

        final DefendantDetailsUpdateRequested defendantDetailsUpdateRequested = (DefendantDetailsUpdateRequested) events.get(1);
        assertThat(defendantDetailsUpdateRequested.getCaseUrn(), is(CASE_PTI_URN));
    }

    @Test
    public void markAsAutoActionedWithCaseUrn() {
        final ScanEnvelope scanEnvelope = buildPayload();
        stagingBulkScanAggregateMemento.setScanEnvelope(scanEnvelope);
        final Stream<Object> eventStream = stagingBulkScanDelegate.markAsAutoActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY);
        final List<Object> events = eventStream.collect(toList());

        final ScanDocumentAutoActioned scanDocumentAutoActioned = (ScanDocumentAutoActioned) events.get(0);

        assertThat(scanDocumentAutoActioned, allOf(
                Matchers.instanceOf(ScanDocumentAutoActioned.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID))
        ));

        final DefendantDetailsUpdateRequested defendantDetailsUpdateRequested = (DefendantDetailsUpdateRequested) events.get(1);
        assertThat(defendantDetailsUpdateRequested.getCaseUrn(), is(CASE_URN));
    }

    @Test
    public void handleScanDocumentAutoActioned() {
        final ScanDocumentAutoActioned scanDocumentAutoActioned = new ScanDocumentAutoActioned(ENVELOPE_ID, DOCUMENT_ID, ACTIONED_BY, ACTIONED_DATE);
        stagingBulkScanAggregateMemento.setScanEnvelope(buildPayload());
        stagingBulkScanDelegate.handleScanDocumentAutoActioned(scanDocumentAutoActioned);

        final ScanEnvelope expected = stagingBulkScanAggregateMemento.getScanEnvelope();

        assertThat(expected.getScanEnvelopeId(), is(ENVELOPE_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getScanDocumentId(), is(DOCUMENT_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getActionedBy(), is(ACTIONED_BY));
        assertThat(expected.getAssociatedScanDocuments().get(0).getStatusUpdatedDate(), notNullValue());
    }

    @Test
    public void deleteActionedDocument() {
        stagingBulkScanAggregateMemento.setScanEnvelope(buildPayload());
        final Stream<Object> eventStream = stagingBulkScanDelegate.deleteActionedDocument(ENVELOPE_ID, DOCUMENT_ID);
        final ActionedDocumentDeleted actionedDocumentDeleted = (ActionedDocumentDeleted) eventStream.findFirst().get();

        assertThat(actionedDocumentDeleted, allOf(
                Matchers.instanceOf(ActionedDocumentDeleted.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID))
        ));
    }

    @Test
    public void handleActionedDocumentDelete() {
        final ActionedDocumentDeleted actionedDocumentDeleted = new ActionedDocumentDeleted(ENVELOPE_ID, DOCUMENT_ID, ZIP_FILE_NAME, DOC_NAME, DELETED_DATE);
        stagingBulkScanAggregateMemento.setScanEnvelope(buildPayload());
        stagingBulkScanDelegate.handleActionedDocumentDelete(actionedDocumentDeleted);

        final ScanEnvelope expected = stagingBulkScanAggregateMemento.getScanEnvelope();

        assertThat(expected.getScanEnvelopeId(), is(ENVELOPE_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getScanDocumentId(), is(DOCUMENT_ID));
        assertThat(expected.getAssociatedScanDocuments().get(0).getDeletedDate(), notNullValue());
    }

    @Test
    public void testRejectDocument() {
        final Stream<Object> eventStream = stagingBulkScanDelegate.rejectDocument(ENVELOPE_ID, DOCUMENT_ID, buildProblems());
        final ScanDocumentRejected scanDocumentRejected = (ScanDocumentRejected) eventStream.findFirst().get();

        assertThat(scanDocumentRejected, allOf(
                Matchers.instanceOf(ScanDocumentRejected.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID)),
                Matchers.hasProperty("rejectedDate", is(notNullValue()))
        ));
    }

    @Test
    public void testExpireDocument() {
        final Stream<Object> eventStream = stagingBulkScanDelegate.expireDocument(ENVELOPE_ID, DOCUMENT_ID, EXPIRED_DATE);
        final ScanDocumentExpired scanDocumentExpired = (ScanDocumentExpired) eventStream.findFirst().get();

        assertThat(scanDocumentExpired, allOf(
                Matchers.instanceOf(ScanDocumentExpired.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID)),
                Matchers.hasProperty("scanDocumentId", is(notNullValue()))
        ));
    }

    @Test
    public void shouldRaiseDocumentFollowUpEvent() {
        Stream<Object> eventStream = stagingBulkScanDelegate.raiseDocumentFollowUp(ENVELOPE_ID, DOCUMENT_ID);
        final ScanDocumentAttachedAndFollowedUp scanDocumentFollowedUp = (ScanDocumentAttachedAndFollowedUp) eventStream.findFirst().get();
        assertThat(scanDocumentFollowedUp, allOf(
                Matchers.instanceOf(ScanDocumentAttachedAndFollowedUp.class),
                Matchers.hasProperty("scanEnvelopeId", is(ENVELOPE_ID)),
                Matchers.hasProperty("scanDocumentId", is(DOCUMENT_ID)),
                Matchers.hasProperty("statusUpdatedDate", is(notNullValue()))
        ));
    }

    private ScanEnvelope buildPayload() {
        return ScanEnvelope.scanEnvelope()
                .withScanEnvelopeId(ENVELOPE_ID)
                .withEnvelopeClassification(ENVELOP_CLASSIFICATION)
                .withAssociatedScanDocuments(
                        Collections.singletonList(ScanDocument.scanDocument()
                                .withScanDocumentId(DOCUMENT_ID)
                                .withCaseUrn(CASE_URN)
                                .withDocumentControlNumber(DOCUMENT_CONTROL_NUMBER)
                                .withDocumentName(DOC_NAME)
                                .withFileName(PDF_NAME)
                                .withManualIntervention(MANUAL_INTERVENTION)
                                .withNextAction(NEXT_ACTION)
                                .withNextActionDate(ZonedDateTime.now())
                                .withNotes(NOTES)
                                .withProsecutorAuthorityId(PROSECUTOR_AUTHORITY_ID)
                                .withProsecutorAuthorityCode(PROSECUTOR_AUTHORITY_CODE)
                                .withVendorReceivedDate(ZonedDateTime.now())
                                .withScanningDate(ZonedDateTime.now())
                                .withPlea(Plea.plea().withOffences(Collections.singletonList(Offence.offence().build())).build())
                                .build()))
                .withExtractedDate(ZonedDateTime.now())
                .withJurisdiction(JURISDICTION)
                .withNotes(DOC_NOTES)
                .withVendorOpeningDate(ZonedDateTime.now())
                .withVendorPOBox(VENDOR_POBOX)
                .withZipFileCreatedDate(ZonedDateTime.now())
                .withZipFileName(ZIP_FILE_NAME)
                .build();
    }

    private ScanEnvelope buildPayloadPtiUrn() {
        return ScanEnvelope.scanEnvelope()
                .withScanEnvelopeId(ENVELOPE_ID)
                .withEnvelopeClassification(ENVELOP_CLASSIFICATION)
                .withAssociatedScanDocuments(
                        Collections.singletonList(ScanDocument.scanDocument()
                                .withScanDocumentId(DOCUMENT_ID)
                                .withCasePTIUrn(CASE_PTI_URN)
                                .withDocumentControlNumber(DOCUMENT_CONTROL_NUMBER)
                                .withDocumentName(DOC_NAME)
                                .withFileName(PDF_NAME)
                                .withManualIntervention(MANUAL_INTERVENTION)
                                .withNextAction(NEXT_ACTION)
                                .withNextActionDate(ZonedDateTime.now())
                                .withNotes(NOTES)
                                .withProsecutorAuthorityId(PROSECUTOR_AUTHORITY_ID)
                                .withProsecutorAuthorityCode(PROSECUTOR_AUTHORITY_CODE)
                                .withVendorReceivedDate(ZonedDateTime.now())
                                .withScanningDate(ZonedDateTime.now())
                                .withPlea(Plea.plea().withOffences(Collections.singletonList(Offence.offence().build())).build())
                                .build()))
                .withExtractedDate(ZonedDateTime.now())
                .withJurisdiction(JURISDICTION)
                .withNotes(DOC_NOTES)
                .withVendorOpeningDate(ZonedDateTime.now())
                .withVendorPOBox(VENDOR_POBOX)
                .withZipFileCreatedDate(ZonedDateTime.now())
                .withZipFileName(ZIP_FILE_NAME)
                .build();
    }

    private List<Problem> buildProblems() {
        final List<Problem> problems = new ArrayList<>();
        final List<ProblemValue> problemValues = new ArrayList<>();
        problemValues.add(new ProblemValue("id", "fileType", "CCNIP", Collections.emptyMap()));
        problems.add(new Problem("INVALID_FILE_TYPE", problemValues));
        return problems;
    }
}