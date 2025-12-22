package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.Interpreter;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DocumentNextStepDecided;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAttachedAndFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanAggregateTest {

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
    private static final UUID DOCUMENT_ID_1 = UUID.fromString("ec998ef3-4775-4166-9ad8-f5eeba78ded1");
    private static final UUID DOCUMENT_ID_2 = UUID.fromString("ec998ef3-4775-4166-9ad8-f5eeba78ded2");
    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final UUID CASE_ID = UUID.fromString("fd998ef3-4775-4166-9ad8-f5eeba78ded2");
    private static final UUID DEFENDANT_ID = UUID.fromString("a64d30e4-40de-4e48-b0d1-ef80ee2e6942");
    private static final UUID OFFENCE_ID = randomUUID();

    @InjectMocks
    private StagingBulkScanAggregate stagingBulkScanAggregate;

    @AfterEach
    public void teardown() {
        try {
            // ensure aggregate is serializable
            SerializationUtils.serialize(stagingBulkScanAggregate);
        } catch (SerializationException e) {
            fail("Aggregate should be serializable");
        }
    }

    @Test
    public void testShouldAggregateEvents() {
        final ScanEnvelope envelope = buildPayload();
        Stream<Object> events = stagingBulkScanAggregate.register(envelope);
        List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        assertThat(eventList.get(0), instanceOf(ScanEnvelopeRegistered.class));

        ScanEnvelopeRegistered scanEnvelopeRegistered = (ScanEnvelopeRegistered) eventList.get(0);
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getVendorPOBox(), equalTo(VENDOR_POBOX));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getZipFileName(), equalTo(ZIP_FILE_NAME));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getAssociatedScanDocuments().get(0).getFileName(), equalTo(PDF_NAME));
    }

    @Test
    void shouldRegisterRaiseDecideEventWhenRegisterLate(){
        final ScanEnvelope envelope = buildPayload();
        stagingBulkScanAggregate.raiseDocumentNextstepDecided(envelope.getScanEnvelopeId(), envelope.getAssociatedScanDocuments().get(0).getScanDocumentId(), true);

        Stream<Object> events = stagingBulkScanAggregate.register(envelope);
        List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());

        assertThat(eventList.get(0), instanceOf(ScanEnvelopeRegistered.class));

        ScanEnvelopeRegistered scanEnvelopeRegistered = (ScanEnvelopeRegistered) eventList.get(0);
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getVendorPOBox(), equalTo(VENDOR_POBOX));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getZipFileName(), equalTo(ZIP_FILE_NAME));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getAssociatedScanDocuments().get(0).getFileName(), equalTo(PDF_NAME));

        DocumentNextStepDecided documentNextStepDecided = (DocumentNextStepDecided) eventList.get(1);
        assertThat(documentNextStepDecided.getScanEnvelopeId(), is(envelope.getScanEnvelopeId()));
        assertThat(documentNextStepDecided.getScanDocumentId(), is(envelope.getAssociatedScanDocuments().get(0).getScanDocumentId()));
        assertThat(documentNextStepDecided.getCaseUrn(), is(envelope.getAssociatedScanDocuments().get(0).getCaseUrn()));
        assertThat(documentNextStepDecided.getIsSjp(), is(true));
    }

    @Test
    void shouldRaiseDecideEventWhenRegisterBefore(){
        final ScanEnvelope envelope = buildPayload();

        Stream<Object> events = stagingBulkScanAggregate.register(envelope);
        List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        assertThat(eventList.get(0), instanceOf(ScanEnvelopeRegistered.class));

        ScanEnvelopeRegistered scanEnvelopeRegistered = (ScanEnvelopeRegistered) eventList.get(0);
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getVendorPOBox(), equalTo(VENDOR_POBOX));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getZipFileName(), equalTo(ZIP_FILE_NAME));
        assertThat(scanEnvelopeRegistered.getScanEnvelope().getAssociatedScanDocuments().get(0).getFileName(), equalTo(PDF_NAME));

        events = stagingBulkScanAggregate.raiseDocumentNextstepDecided(envelope.getScanEnvelopeId(), envelope.getAssociatedScanDocuments().get(0).getScanDocumentId(), true);
        eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        DocumentNextStepDecided documentNextStepDecided = (DocumentNextStepDecided) eventList.get(0);
        assertThat(documentNextStepDecided.getScanEnvelopeId(), is(envelope.getScanEnvelopeId()));
        assertThat(documentNextStepDecided.getScanDocumentId(), is(envelope.getAssociatedScanDocuments().get(0).getScanDocumentId()));
        assertThat(documentNextStepDecided.getCaseUrn(), is(envelope.getAssociatedScanDocuments().get(0).getCaseUrn()));
        assertThat(documentNextStepDecided.getIsSjp(), is(true));
    }

    @Test
    public void testMarkAsActioned() {
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument().withScanDocumentId(DOCUMENT_ID_1).build(),
                        ScanDocument.scanDocument().withScanDocumentId(DOCUMENT_ID_2).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        Stream<Object> events = stagingBulkScanAggregate.markAsManuallyActioned(SCAN_ENVELOPE_ID, DOCUMENT_ID_1, ACTIONED_BY);
        List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        assertThat(eventList.get(0), instanceOf(ScanDocumentManuallyActioned.class));

        final ScanDocumentManuallyActioned scanDocumentManuallyActioned = (ScanDocumentManuallyActioned) eventList.get(0);
        assertThat(scanDocumentManuallyActioned.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(scanDocumentManuallyActioned.getScanEnvelopeId(), equalTo(SCAN_ENVELOPE_ID));
        assertThat(scanDocumentManuallyActioned.getActionedBy(), equalTo(ACTIONED_BY));

        final ScanEnvelope scanEnvelopeObjAfterAction = stagingBulkScanAggregate.getScanEnvelope();
        final ScanDocument scanDocument = scanEnvelopeObjAfterAction.getAssociatedScanDocuments().stream().filter(document -> document.getScanDocumentId().equals(DOCUMENT_ID_1)).findFirst().orElse(ScanDocument.scanDocument().build());
        assertThat(scanDocument.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(scanDocument.getActionedBy(), equalTo(ACTIONED_BY));
    }

    @Test
    public void testActionedDocumentDelete() {
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope()
                .withZipFileName(ZIP_FILE_NAME)
                .withAssociatedScanDocuments(
                        Arrays.asList(ScanDocument.scanDocument()
                                        .withScanDocumentId(DOCUMENT_ID_1)
                                        .withFileName(PDF_NAME)
                                        .build(),
                                ScanDocument.scanDocument()
                                        .withScanDocumentId(DOCUMENT_ID_2)
                                        .withFileName(PDF_NAME)
                                        .build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        Stream<Object> events = stagingBulkScanAggregate.deleteActionedDocument(SCAN_ENVELOPE_ID,
                DOCUMENT_ID_1);

        List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        assertThat(eventList.get(0), instanceOf(ActionedDocumentDeleted.class));

        ActionedDocumentDeleted actionedDocumentDeleted = (ActionedDocumentDeleted) eventList.get(0);
        assertThat(actionedDocumentDeleted.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(actionedDocumentDeleted.getScanEnvelopeId(), equalTo(SCAN_ENVELOPE_ID));
        assertThat(actionedDocumentDeleted.getZipFileName(), equalTo(ZIP_FILE_NAME));
        assertThat(actionedDocumentDeleted.getDocumentFileName(), equalTo(PDF_NAME));

        final ScanEnvelope scanEnvelopeObjAfterAction = stagingBulkScanAggregate.getScanEnvelope();
        final ScanDocument scanDocument = scanEnvelopeObjAfterAction.getAssociatedScanDocuments().stream().filter(document -> document.getScanDocumentId().equals(DOCUMENT_ID_1)).findFirst().orElse(ScanDocument.scanDocument().build());
        assertThat(scanDocument.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(scanDocument.getFileName(), equalTo(PDF_NAME));
        assertTrue(scanDocument.getDeleted());
    }

    @Test
    public void testMarkAsAutoActioned() {
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument().withScanDocumentId(DOCUMENT_ID_1).build(),
                        ScanDocument.scanDocument().withScanDocumentId(DOCUMENT_ID_2).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.markAsAutoActioned(SCAN_ENVELOPE_ID, DOCUMENT_ID_1, ACTIONED_BY);
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());

        assertThat(eventList.get(0), instanceOf(ScanDocumentAutoActioned.class));

        final ScanDocumentAutoActioned scanDocumentAutoActioned = (ScanDocumentAutoActioned) eventList.get(0);
        assertThat(scanDocumentAutoActioned.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(scanDocumentAutoActioned.getScanEnvelopeId(), equalTo(SCAN_ENVELOPE_ID));
        assertThat(scanDocumentAutoActioned.getActionedBy(), equalTo(ACTIONED_BY));

        final ScanEnvelope scanEnvelopeObjAfterAction = stagingBulkScanAggregate.getScanEnvelope();
        final ScanDocument scanDocument = scanEnvelopeObjAfterAction.getAssociatedScanDocuments().stream().filter(document -> document.getScanDocumentId().equals(DOCUMENT_ID_1)).findFirst().orElse(ScanDocument.scanDocument().build());
        assertThat(scanDocument.getScanDocumentId(), equalTo(DOCUMENT_ID_1));
        assertThat(scanDocument.getActionedBy(), equalTo(ACTIONED_BY));
    }

    @Test
    public void defendantGuiltyAndDontWishToComeToCourt() {
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.GUILTY, false, true, null))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(true, true, null), randomUUID());
        assertPleaDetailsUpdatedHasAllFieldsCorrectly(events);
    }

    @Test
    public void defendantGuiltyAndWishToComeToCourt() {
        final Interpreter interpreter = new Interpreter("welsh", true);
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.GUILTY, true, true, interpreter))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(false, true, interpreter), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getLanguage(), is("welsh"));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getNeeded(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(true));
    }

    @Test
    public void defendantNotGuilty() {
        final Interpreter interpreter = new Interpreter("welsh", true);
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.NOT_GUILTY, true, true, interpreter))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(false, true, interpreter), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getLanguage(), is("welsh"));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getNeeded(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.NOT_GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(true));
    }

    @Test
    public void defendantHearingLanguageIsWelsh() {
        final Interpreter interpreter = new Interpreter("welsh", true);
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.NOT_GUILTY, true, true, interpreter))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(false, false, interpreter), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getLanguage(), is("welsh"));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getNeeded(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.NOT_GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(true));
    }

    @Test
    public void defendantHearingLanguageIsNotSelected() {
        final Interpreter interpreter = new Interpreter("welsh", true);
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.NOT_GUILTY, true, null, interpreter))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(false, false, interpreter), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getLanguage(), is("welsh"));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getNeeded(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.NOT_GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(false));
    }

    @Test
    public void defendantInterpreterIsNotSelected() {
        final Interpreter interpreter = new Interpreter("welsh", true);
        final ScanEnvelope scanEnvelope = ScanEnvelope.scanEnvelope().withAssociatedScanDocuments(
                Arrays.asList(ScanDocument.scanDocument()
                        .withPlea(buildPlea(PleaType.NOT_GUILTY, true, null, null))
                        .withScanDocumentId(DOCUMENT_ID_1).build())).build();

        registerScanEnvelopeTemplate(scanEnvelope);

        final Stream<Object> events = stagingBulkScanAggregate.updateDefendantDetails(buildDefendant(false, false, interpreter), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getLanguage(), is("welsh"));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter().getNeeded(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.NOT_GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(true));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(false));
    }

    @Test
    public void shouldRaiseDocumentFollowUp() {
        final Stream<Object> events = stagingBulkScanAggregate.raiseDocumentFollowUp(randomUUID(), randomUUID());
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(1, eventList.size());
        assertThat(eventList, hasItem(instanceOf(ScanDocumentAttachedAndFollowedUp.class)));
    }

    private Defendant buildDefendant(final boolean wishToComeToCourt, final Boolean welshHearing, final Interpreter interpreter) {
        final Defendant defendant = Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withCaseId(CASE_ID)
                .withScanDocumentId(DOCUMENT_ID_1)
                .withContactDetails(new ContactDetails.Builder().build())
                .withPlea(buildPlea(PleaType.GUILTY, wishToComeToCourt, welshHearing, interpreter))
                .build();
        return defendant;
    }

    private Plea buildPlea(final PleaType pleaType, final boolean wishToComeToCourt, final Boolean welshHearing, final Interpreter interpreter) {
        final List<Offence> offences = new ArrayList<>();
        offences.add(new Offence(false, OFFENCE_ID, pleaType, "TV Licence not found"));
        final Plea plea = new Plea(null, false, null, null, null, interpreter, offences, welshHearing, wishToComeToCourt);
        return plea;
    }

    private ScanEnvelope buildPayload() {
        return ScanEnvelope.scanEnvelope()
                .withEnvelopeClassification(ENVELOP_CLASSIFICATION)
                .withAssociatedScanDocuments(
                        Collections.singletonList(ScanDocument.scanDocument()
                                .withScanDocumentId(randomUUID())
                                .withCasePTIUrn(CASE_PTI_URN)
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


    private void assertPleaDetailsUpdatedHasAllFieldsCorrectly(Stream<Object> events) {
        final List<Object> eventList = events.collect(Collectors.toList());
        assertEquals(2, eventList.size());
        assertThat(eventList, hasItem(instanceOf(PleaDetailsUpdated.class)));
        final PleaDetailsUpdated pleaDetailsUpdated = (PleaDetailsUpdated) eventList.get(1);
        assertThat(pleaDetailsUpdated.getCaseId(), is(CASE_ID));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().getInterpreter(), is(nullValue()));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getDefendantId(), is(DEFENDANT_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getOffenceId(), is(OFFENCE_ID));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getPleaType(), is(PleaType.GUILTY));
        assertThat(pleaDetailsUpdated.getDefendantPleas().get(0).getWishToComeToCourt(), is(false));
        assertThat(pleaDetailsUpdated.getDefendantCourtOptions().isWelshHearing(), is(true));
    }

    private void registerScanEnvelopeTemplate(final ScanEnvelope scanEnvelope) {
        stagingBulkScanAggregate.register(scanEnvelope);
    }
}
