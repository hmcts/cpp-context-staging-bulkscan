package uk.gov.moj.cpp.stagingbulkscan.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.PENDING;
import static uk.gov.moj.cpp.stagingbulkscan.stub.SjpServiceStub.verifySetDefendantCommandInvoked;
import static uk.gov.moj.cpp.stagingbulkscan.stub.SjpServiceStub.verifySetPleaCommandInvoked;
import static uk.gov.moj.cpp.stagingbulkscan.utils.CommandUtil.actionScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.CommandUtil.deleteScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.CommandUtil.registerScanEnvelope;
import static uk.gov.moj.cpp.stagingbulkscan.utils.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchActionedScanDocuments;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchExpiredScanDocumentsContains;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchLiveScanDocumentsContains;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueryUtil.fetchPendingScanDocumentsContains;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.DOCUMENT_AUTO_ACTIONED_EVENT;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.MARK_DOCUMENT_AS_ACTIONED_PUBLIC_EVENT;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.PUBLIC_BULK_SCAN_MATERIAL_FOLLOWUP;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.PUBLIC_CASE_REJECTED;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.PUBLIC_CC_DOCUMENT_ADDED_EVENT;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.PUBLIC_SJP_DOCUMENT_ADDED_EVENT;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.VENDOR_RECEIVED_DATE;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.addCcCaseMaterialEvent;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.addSjpCaseMaterialEvent;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.buildPayloadWithAllValues;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.buildScanDocumentPayload;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.expireDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateActionedScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateExpiredScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateFinancialMeansDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateFinancialMeansDocumentForFollowup;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateFollowUpScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.queryAndValidateFollowUpScanDocumentWithCasePtiUrn;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.rejectDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.ScanEnvelopeHelper.validateEnvelopeForDeletedScanDocument;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WireMockStubUtils.stubIdMapperReturningExistingAssociation;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;
import uk.gov.moj.cpp.stagingbulkscan.stub.SjpServiceStub;
import uk.gov.moj.cpp.stagingbulkscan.utils.JsonHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class StagingBulkScanRegisterIT extends BaseIntegrationTest {
    private static final String SINGLE_PLEA = "Single Justice Procedure Notice - Plea (Single)";
    private static final String DISQUALIFICATION_REPLY_SLIP = "DISQUALIFICATION_REPLY_SLIP";
    public static final String STAGING_BULK_SCAN = "stagingbulkscan";
    private final UUID caseId = fromString("5c019f8a-b3a3-40e5-9709-c54bc4f1c55c");

    private final UUID offenceId = fromString("4bf99b17-b35a-4125-9331-a6e52dc10726");
    private final UUID offenceId_1 = fromString("82cbe27a-761b-4738-be1c-ca90e42fa78f");
    private final UUID defendantId = fromString("14831593-0952-49bf-a060-d39f2cf752a1");

    private static final String DOCUMENT_MARKED_FOR_FOLLOW_UP_PUBLIC_EVENT = "public.stagingbulkscan.document-marked-for-follow-up";
    private static final String STAGING_BULK_SCAN_EVENT = "jms.topic.stagingbulkscan.event";
    private static final String PROSECUTOR_NAME = "TFL";

    @Test
    public void shouldMarkDocumentAsManuallyActioned() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, FOLLOW_UP, SINGLE_PLEA));

        final ScanDocumentsResponse scanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocument = scanDocuments
                .getScanDocuments()
                .stream()
                .filter(document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final UUID scanEnvelopeId = scanDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = scanDocument.get().getId();

        final JmsMessageConsumerClient docStatusUpdatedPublicConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(MARK_DOCUMENT_AS_ACTIONED_PUBLIC_EVENT)
                        .getMessageConsumerClient();

        actionScanDocument(scanEnvelopeId, buildScanDocumentPayload(scanDocumentId));

        final JsonEnvelope jsEnvelope = docStatusUpdatedPublicConsumer.retrieveMessageAsJsonEnvelope().get();

        assertThat(jsEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.scanEnvelopeId", is(scanEnvelopeId.toString())),
                withJsonPath("$.scanDocumentId", is(scanDocumentId.toString()))
        )));

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "MANUALLY_ACTIONED", PROSECUTOR_NAME, SINGLE_PLEA);
    }

    @Test
    public void shouldDeleteScanDocumentsFromAzure() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();

        //Save the envelope with 2 documents
        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocument = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final UUID scanEnvelopeId = scanDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = scanDocument.get().getId();

        //mark 1st document as actioned
        actionScanDocument(scanEnvelopeId, buildScanDocumentPayload(scanDocumentId));

        //Fetch All envelopes
        final ScanDocumentsResponse actionedDocuments = fetchActionedScanDocuments(documentFileNameOne);

        final Optional<ScanDocument> actionedDocumentOptional = actionedDocuments.getScanDocuments().stream().findFirst();
        final ScanDocument actionedDocument = actionedDocumentOptional.get();

        final JmsMessageConsumerClient deleteDocumentActionedDocumentConsumer = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.actioned-document-deleted")
                .getMessageConsumerClient();

        //delete actioned document.
        deleteScanDocument(actionedDocument.getScanEnvelopeId(), actionedDocument.getId());

        final JsonEnvelope jsEnvelope = deleteDocumentActionedDocumentConsumer.retrieveMessageAsJsonEnvelope().get();
        assertThat(jsEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.scanEnvelopeId", equalTo(actionedDocument.getScanEnvelopeId().toString())),
                withJsonPath("$.scanDocumentId", equalTo(actionedDocument.getId().toString()))
        )));

        //Validate if document is deleted
        validateEnvelopeForDeletedScanDocument(actionedDocument.getScanEnvelopeId(), actionedDocument.getId(), documentFileNameOne);
    }

    @Test
    public void shouldActionSjpDocumentAsAutoActioned() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();
        final UUID submissionId = randomUUID();

        final JmsMessageConsumerClient deleteDocumentActionedDocumentConsumer = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames(DOCUMENT_AUTO_ACTIONED_EVENT)
                .getMessageConsumerClient();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocument = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final UUID scanEnvelopeId = scanDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = scanDocument.get().getId();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanEnvelopeId, scanDocumentId));
        
        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertThat(deleteDocumentActionedDocumentConsumer.retrieveMessage().isPresent(), is(true));

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "AUTO_ACTIONED", PROSECUTOR_NAME, SINGLE_PLEA);
    }

    @Test
    void shouldActionSjpDocumentAsAutoActionedWhenOrderIsWrong() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();
        final UUID submissionId = randomUUID();

        final JsonObject scanDocumentJson = buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA);
        final JmsMessageConsumerClient deleteDocumentActionedDocumentConsumer = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames(DOCUMENT_AUTO_ACTIONED_EVENT)
                .getMessageConsumerClient();

        final UUID scanEnvelopeId = fromString(scanDocumentJson.getString("scanEnvelopeId"));
        final UUID scanDocumentId = fromString(scanDocumentJson.getJsonArray("associatedScanDocuments").getJsonObject(0).getString("scanDocumentId"));
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanEnvelopeId, scanDocumentId));


        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        registerScanEnvelope(scanDocumentJson);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertThat(deleteDocumentActionedDocumentConsumer.retrieveMessage().isPresent(), is(true));

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "AUTO_ACTIONED", PROSECUTOR_NAME, SINGLE_PLEA);
    }

    @Test
    public void shouldActionCcDocumentAsAutoActioned() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();
        final UUID submissionId = randomUUID();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocument = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final UUID scanEnvelopeId = scanDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = scanDocument.get().getId();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanEnvelopeId, scanDocumentId));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_CC_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addCcCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "AUTO_ACTIONED", PROSECUTOR_NAME, SINGLE_PLEA);
    }

    @Test
    public void handleDocumentRejectedAndFollowUp() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();
        final UUID submissionId = randomUUID();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(DOCUMENT_MARKED_FOR_FOLLOW_UP_PUBLIC_EVENT)
                .getMessageConsumerClient();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse pendingScanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> pendingDocument = pendingScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertTrue(pendingDocument.isPresent());

        final UUID scanEnvelopeId = pendingDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = pendingDocument.get().getId();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanEnvelopeId, scanDocumentId));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient2 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_CASE_REJECTED)
                .getMessageConsumerClient();

        rejectDocument(submissionId);

        assertThat(publicJmsMessageConsumerClient2.retrieveMessageAsJsonEnvelope().isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertTrue(followupDocumentOptional.isPresent());

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateFollowUpScanDocument(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP", PROSECUTOR_NAME, SINGLE_PLEA);
        verifyDocumentMarkedForFollowUpPublicEventReceived(followupDocument.getId(), publicJmsMessageConsumerClient);
    }

    @Test
    public void handleDocumentExpiryAndFollowUp() {
        final String documentFileNameOne = STRING.next();
        final String documentFileNameTwo = STRING.next();
        final UUID submissionId = randomUUID();
        final JmsMessageConsumerClient publicJmsMessageConsumerClient1 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(DOCUMENT_MARKED_FOR_FOLLOW_UP_PUBLIC_EVENT)
                .getMessageConsumerClient();

        registerScanEnvelope(buildPayloadWithAllValues(documentFileNameOne, documentFileNameTwo, PENDING, SINGLE_PLEA));

        final ScanDocumentsResponse pendingScanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> pendingDocument = pendingScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertTrue(pendingDocument.isPresent());

        final UUID scanEnvelopeId = pendingDocument.get().getScanEnvelopeId();
        final UUID scanDocumentId = pendingDocument.get().getId();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanEnvelopeId, scanDocumentId));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient2 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_BULK_SCAN_MATERIAL_FOLLOWUP)
                .getMessageConsumerClient();

        expireDocument(submissionId);

        assertThat(publicJmsMessageConsumerClient2.retrieveMessageAsJsonEnvelope().isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchExpiredScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertTrue(followupDocumentOptional.isPresent());

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateExpiredScanDocument(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP");
        verifyDocumentMarkedForFollowUpPublicEventReceived(followupDocument.getId(), publicJmsMessageConsumerClient1);
    }

    @Test
    public void handleDefendantAdditionalDetailsUpdate() {
        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final String emailAddress = "jsmith@email.com";
        final String contactNumber = "07901111111";
        final UUID submissionId = randomUUID();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));
        
        final JmsMessageConsumerClient privateJmsMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient1 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient1.retrieveMessage().isPresent(), is(true));
        assertThat(privateJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));


        Predicate<JSONObject> predicate = payload -> payload.getString("email").equals(emailAddress) &&
                payload.getJSONObject("contactNumber").getString("mobile").equals(contactNumber);

        verifySetDefendantCommandInvoked(predicate, caseId, defendantId);
    }

    @Test
    public void shouldUpdateDrivingLicenseNumberAndAutoActionedWithValidDrivingLicenseNumber() {
        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/envelope-with-offence-plea-document-for-driving-license-number.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .put("drivingLicenseNumber", "RAMIS858080R98GB")
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateEventConsumer = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicEventConsumer.retrieveMessage().isPresent(), is(true));
        assertThat(privateEventConsumer.retrieveMessage().isPresent(), is(true));

        final Predicate<JSONObject> predicate = payload -> payload.getString("driverNumber").equals("RAMIS858080R98GB");

        verifySetDefendantCommandInvoked(predicate, caseId, defendantId);

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "AUTO_ACTIONED", "SJPN", SINGLE_PLEA);
    }

    @Test
    public void shouldRaiseFollowUp_WhenDrivingLicenseNumberIsNotSameAsExistingDrivingLicenseNumberOfTheDefendant() {
        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();
        
        final JmsMessageConsumerClient publicJmsMessageConsumerClient1 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(DOCUMENT_MARKED_FOR_FOLLOW_UP_PUBLIC_EVENT)
                .getMessageConsumerClient();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details-with-driving-license-number.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/envelope-with-offence-plea-document-for-driving-license-number.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("dateTime", VENDOR_RECEIVED_DATE.toString())
                .put("wishToComeToCourt", true)
                .put("drivingLicenseNumber", "RAMAA858080R98GB")
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateEventConsumer = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicEventConsumer.retrieveMessage().isPresent(), is(true));
        assertThat(privateEventConsumer.retrieveMessage().isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertThat(followupDocumentOptional.isPresent(), is(true));

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateFollowUpScanDocument(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP", "SJPN", SINGLE_PLEA);
        assertThat(publicJmsMessageConsumerClient1.retrieveMessage().isPresent(), is(true));

    }

    @Test
    public void shouldRaiseFollowUp_WhenDrivingLicenseNumberIsInvalid() {
        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient1 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(DOCUMENT_MARKED_FOR_FOLLOW_UP_PUBLIC_EVENT)
                .getMessageConsumerClient();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details-with-driving-license-number.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/envelope-with-offence-plea-document-for-driving-license-number.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("dateTime", VENDOR_RECEIVED_DATE.toString())
                .put("wishToComeToCourt", true)
                .put("drivingLicenseNumber", "RAMAA858080R98G")
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient2 = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient2.retrieveMessage().isPresent(), is(true));
        assertThat(privateJMSMessageConsumerClient.retrieveMessage().isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertThat(followupDocumentOptional.isPresent(), is(true));

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateFollowUpScanDocument(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP", "SJPN", SINGLE_PLEA);
        verifyDocumentMarkedForFollowUpPublicEventReceived(followupDocument.getId(), publicJmsMessageConsumerClient1);

    }

    @Test
    public void shouldAttachDisqualificationReplyDocumentAutoActioned() {
        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-disqualified-reply-slip.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("documentName", DISQUALIFICATION_REPLY_SLIP)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .put("drivingLicenceNumber", "RAMAA858080R98GR")
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertThat(privateJMSMessageConsumerClient.retrieveMessage().isPresent(), is(true));

        queryAndValidateActionedScanDocument(scanEnvelopeId, scanDocumentId, documentFileNameOne, "AUTO_ACTIONED", "SJPN", "DISQUALIFICATION_REPLY_SLIP");
    }

    @Test
    public void shouldAttachDisqualificationReplyDocumentWhenCaseCompletedAndRaiseFollowUp() {
        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-attached-with-follow-up")
                .getMessageConsumerClient();

        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details-with-case-is-completed.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-disqualified-reply-slip.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("documentName", DISQUALIFICATION_REPLY_SLIP)
                .put("dateTime", VENDOR_RECEIVED_DATE.toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertThat(privateJMSMessageConsumerClient.retrieveMessage().isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertThat(followupDocumentOptional.isPresent(), is(true));

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateFollowUpScanDocument(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP", "SJPN", DISQUALIFICATION_REPLY_SLIP);

    }

    @Test
    public void shouldAttachDisqualificationReplyDocumentWhenCaseCompletedAndRaiseFollowUpCasePtiUrn() {
        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-attached-with-follow-up")
                .getMessageConsumerClient();

        // Mock SJP GET request for case details by URN
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileNameOne = STRING.next();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details-with-case-is-completed.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", offenceId)
                .build());

        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-disqualified-reply-slip-with-case-pti-urn.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileNameOne)
                .put("documentName", DISQUALIFICATION_REPLY_SLIP)
                .put("dateTime", VENDOR_RECEIVED_DATE.toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileNameOne))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertThat(privateJMSMessageConsumerClient.retrieveMessage(40000L).isPresent(), is(true));

        final ScanDocumentsResponse followupScanDocuments = fetchLiveScanDocumentsContains(documentFileNameOne);
        final Optional<ScanDocument> followupDocumentOptional = followupScanDocuments.getScanDocuments().stream().filter(d -> d.getDocumentFileName().equals(documentFileNameOne)).findFirst();
        assertThat(followupDocumentOptional.isPresent(), is(true));

        final ScanDocument followupDocument = followupDocumentOptional.get();

        queryAndValidateFollowUpScanDocumentWithCasePtiUrn(followupDocument.getScanEnvelopeId(), followupDocument.getId(), documentFileNameOne, "FOLLOW_UP", "SJPN", DISQUALIFICATION_REPLY_SLIP);
    }

    @Test
    public void shouldActionSjpDocumentAsAutoActionedAndUpdateFinancialMeans() {
        final String documentFileName = STRING.next();
        final ScanDocument scanDocument = mockDefendantFinancialMeansRequest("stub-data/stagingbulkscan.scan-envelope-with-financial-means.json", documentFileName);

        queryAndValidateFinancialMeansDocument(scanDocument.getScanEnvelopeId(), scanDocument.getId(), documentFileName);
    }

    @Test
    public void shouldAttachSjpDocumentAndUpdateDocumentAsFollowupAndUpdateFinancialMeans() {
        final String documentFileName = STRING.next();
        final ScanDocument scanDocument = mockDefendantFinancialMeansRequest("stub-data/stagingbulkscan.scan-envelope-with-financial-means-for-followup.json", documentFileName);

        queryAndValidateFinancialMeansDocumentForFollowup(scanDocument.getScanEnvelopeId(), scanDocument.getId(), documentFileName);
    }

    @Test
    public void handleGuiltyPleaWithRequestHearingDetailsUpdate() {
        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileName = STRING.next();

        SjpServiceStub.stubSetPleaCommandInvoked(caseId);
        SjpServiceStub.registerCaseDetailsByUrn(caseUrn, caseId, offenceId, defendantId);
        SjpServiceStub.registerCaseDetailsByCaseId(caseId, offenceId, defendantId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");
        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());
        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));

        Predicate<JSONObject> predicate = payload -> payload.getJSONObject("defendantCourtOptions").getBoolean("welshHearing")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("interpreter").getString("language").equals("welsh")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("interpreter").getBoolean("needed")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("disabilityNeeds").getString("disabilityNeeds").equals("wheel chair access required")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("disabilityNeeds").getBoolean("needed")
                && payload.getJSONArray("pleas").getJSONObject(0).getString("offenceId").equals(offenceId.toString())
                && payload.getJSONArray("pleas").getJSONObject(0).getString("defendantId").equals(defendantId.toString())
                && payload.getJSONArray("pleas").getJSONObject(0).getString("pleaType").equals("GUILTY_REQUEST_HEARING");
        verifySetPleaCommandInvoked(predicate, caseId);
    }

    @Test
    public void handleGuiltyPleaDetailsUpdate() {
        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileName = STRING.next();

        SjpServiceStub.registerCaseDetailsByCaseId(caseId, offenceId, defendantId);
        SjpServiceStub.registerCaseDetailsByUrn(caseUrn, caseId, offenceId, defendantId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");
        SjpServiceStub.stubSjpSetDefendantCommand(caseId, defendantId);
        SjpServiceStub.stubSjpSetPleaCommand(caseId);

        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-single-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", false)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        Predicate<JSONObject> predicate = payload -> payload.getJSONObject("defendantCourtOptions").getBoolean("welshHearing")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("interpreter").getString("language").equals("welsh")
                && payload.getJSONObject("defendantCourtOptions").getJSONObject("interpreter").getBoolean("needed")
                && payload.getJSONArray("pleas").getJSONObject(0).getString("offenceId").equals(offenceId.toString())
                && payload.getJSONArray("pleas").getJSONObject(0).getString("defendantId").equals(defendantId.toString())
                && payload.getJSONArray("pleas").getJSONObject(0).getString("pleaType").equals("GUILTY");

        verifySetPleaCommandInvoked(predicate, caseId);
    }

    @Test
    public void handleGuiltyPleaWithRequestHearingForMultipleOffences() {
        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileName = STRING.next();

        SjpServiceStub.registerCaseDetailsWithMultipleOffences(caseUrn, caseId, offenceId, offenceId_1, defendantId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "NO_VERDICT", "NO_VERDICT");

        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-multiple-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());
        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        final JSONObject responseJSONObject = SjpServiceStub.pollForSetPleaCommandInvokedForMultipleOffences(hasSize(1), caseId).get(0);

        final JsonObject expectedPayload = getFileContentAsJson("stub-data/expected-multiple-offences-payload.json",
                ImmutableMap.of("defendantId", defendantId, "offenceId", offenceId, "offenceId_1", offenceId_1));
        assertThat(JsonHelper.lenientCompare(expectedPayload.getJsonArray("pleas").getJsonObject(0),
                responseJSONObject.getJSONArray("pleas").getJSONObject(0)), is(true));
        assertThat(JsonHelper.lenientCompare(expectedPayload.getJsonArray("pleas").getJsonObject(1),
                responseJSONObject.getJSONArray("pleas").getJSONObject(1)), is(true));
    }

    @Test
    public void handleGuiltyPleaWithRequestHearingForMultipleOffencesWithOneAlreadyConvicted() {
        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileName = STRING.next();

        SjpServiceStub.registerCaseDetailsWithMultipleOffences(caseUrn, caseId, offenceId, offenceId_1, defendantId);
        SjpServiceStub.registerCaseResults(caseId, offenceId, offenceId_1, "PROVEN_SJP", "NO_VERDICT");

        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-multiple-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", ZonedDateTime.now(ZoneOffset.UTC).toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());
        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        final JSONObject responseJSONObject = SjpServiceStub.pollForSetPleaCommandInvokedForMultipleOffences(hasSize(1), caseId).get(0);

        final JsonObject expectedPayload = getFileContentAsJson("stub-data/expected-multiple-offences-payload.json",
                ImmutableMap.of("defendantId", defendantId, "offenceId", offenceId, "offenceId_1", offenceId_1));

        assertThat(responseJSONObject.getJSONArray("pleas").toList(), hasSize(2));

        assertThat(
                JsonHelper.lenientCompare(expectedPayload.getJsonArray("pleas").getJsonObject(0),
                        responseJSONObject.getJSONArray("pleas").getJSONObject(0)),
                is(true)
        );
    }

    @Test
    public void multipleOffencesCaseHasFinalDecisionThenFollowUp() {
        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();
        final String caseUrn = "12295672";
        final String documentFileName = STRING.next();

        SjpServiceStub.registerCaseDetailsWithOffenceHasFinalDecision(caseUrn, caseId, offenceId, offenceId_1, defendantId);

        SjpServiceStub.stubSjpSetPleaCommand(caseId);
        final JsonObject registerEnvelopePayload = getFileContentAsJson("stub-data/scan-envelope-with-multiple-offence-plea-document.json", ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", VENDOR_RECEIVED_DATE.toString())
                .put("wishToComeToCourt", true)
                .build());

        registerScanEnvelope(registerEnvelopePayload);
        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));


        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient privateJMSMessageConsumerClient2 = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames( "stagingbulkscan.events.document-auto-actioned-with-follow-up")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());
        assertTrue(privateJMSMessageConsumerClient2.retrieveMessage().isPresent());
        queryAndValidateFollowUpScanDocument(scanDocument.getScanEnvelopeId(), scanDocument.getId(), documentFileName, "FOLLOW_UP", PROSECUTOR_NAME, SINGLE_PLEA);
    }

    private ScanDocument mockDefendantFinancialMeansRequest(final String jsonPath, final String documentFileName) {
        final String caseUrn = "12295672";
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final JsonObject caseUrPayload = getFileContentAsJson("stub-data/sjp.query.case-details.json", ImmutableMap.<String, Object>builder()
                .put("caseUrn", caseUrn)
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .put("offenceId", randomUUID())
                .build());
        final JsonObject financialMeansPayload = getFileContentAsJson("stub-data/sjp.query.all-financial-means.json", ImmutableMap.<String, Object>builder()
                .put("caseId", caseId)
                .put("defendantId", defendantId)
                .build());
        SjpServiceStub.stubSjpQueryByUrn(caseUrPayload, caseUrn);
        SjpServiceStub.stubSjpQueryByCaseId(caseUrPayload, caseId);
        SjpServiceStub.stubSjpQueryAllFinancialMeans(financialMeansPayload, defendantId);
        SjpServiceStub.stubSjpFinancialMeansCommand(caseId, defendantId);
        SjpServiceStub.stubSjpNiNumberCommand(caseId, defendantId);


        final UUID submissionId = randomUUID();
        final UUID scanEnvelopeId = randomUUID();
        final UUID scanDocumentId = randomUUID();

        final JsonObject scanEnvelopePayload = getFileContentAsJson(jsonPath, ImmutableMap.<String, Object>builder()
                .put("scanEnvelopeId", scanEnvelopeId)
                .put("scanDocumentId", scanDocumentId)
                .put("documentFileName", documentFileName)
                .put("dateTime", ZonedDateTime.now(UTC).toString())
                .build());

        registerScanEnvelope(scanEnvelopePayload);

        final ScanDocumentsResponse scanDocuments = fetchPendingScanDocumentsContains(documentFileName);
        final Optional<ScanDocument> scanDocumentOptional = scanDocuments.getScanDocuments().stream().filter(
                        document -> document.getDocumentFileName().equalsIgnoreCase(documentFileName))
                .findFirst();

        final ScanDocument scanDocument = scanDocumentOptional.get();
        stubIdMapperReturningExistingAssociation(submissionId, constructDocumentReference(scanDocument.getScanEnvelopeId(), scanDocument.getId()));

        final JmsMessageConsumerClient privateJMSMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider(STAGING_BULK_SCAN)
                .withEventNames("stagingbulkscan.events.document-auto-actioned")
                .getMessageConsumerClient();

        final JmsMessageConsumerClient publicJmsMessageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                .getMessageConsumerClient();

        addSjpCaseMaterialEvent(submissionId);


        assertTrue(privateJMSMessageConsumerClient.retrieveMessage().isPresent());

        assertThat(publicJmsMessageConsumerClient.retrieveMessage().isPresent(), is(true));
        return scanDocument;
    }

    private String constructDocumentReference(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        return scanEnvelopeId.toString() + ":" + scanDocumentId.toString();
    }

    private void verifyDocumentMarkedForFollowUpPublicEventReceived(final UUID documentId, final JmsMessageConsumerClient jmsMessageConsumerClient) {
        final JsonPath topicMessage = jmsMessageConsumerClient.retrieveMessageAsJsonPath().get();
        final Map<String, String> documentPayload = topicMessage.getJsonObject("document");
        assertThat(documentPayload.get("id"), is(documentId.toString()));
    }
}
