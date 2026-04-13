package uk.gov.moj.cpp.stagingbulkscan.utils;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.StatusCode.CASE_NOT_FOUND;
import static uk.gov.moj.cpp.stagingbulkscan.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.cpp.stagingbulkscan.utils.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.stagingbulkscan.utils.HttpClientUtil.getReadUrl;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;
import static uk.gov.moj.cpp.stagingbulkscan.it.BaseIntegrationTest.USER_ID_CROWN_COURT_USER;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WireMockStubUtils.setupAsSystemUser;

public class ScanEnvelopeHelper {

    public static final String QUERY_SCAN_DOCUMENTS_URL = "/stagingbulkscan-query-api/query/api/rest/stagingbulkscan/scan-documents";
    public static final String QUERY_SCAN_ACTIONED_DOCUMENTS_REQUEST_TYPE = "application/vnd.stagingbulkscan.get-all-documents-by-status+json";

    public static final String QUERY_SCAN_DOCUMENT_REQUEST_TYPE = "application/vnd.stagingbulkscan.get-all-documents-by-status+json";

    public static final String PUBLIC_SJP_DOCUMENT_ADDED_EVENT = "public.sjp.case-document-added";
    public static final String PUBLIC_CC_DOCUMENT_ADDED_EVENT = "public.progression.court-document-added";
    public static final String PUBLIC_CASE_REJECTED = "public.prosecutioncasefile.material-rejected";
    public static final String PUBLIC_BULK_SCAN_MATERIAL_FOLLOWUP = "public.prosecutioncasefile.bulkscan-material-followup";
    public static final ZonedDateTime VENDOR_RECEIVED_DATE = ZonedDateTime.now(UTC);


    public static final String DOCUMENT_AUTO_ACTIONED_EVENT = "stagingbulkscan.events.document-auto-actioned";

    private static final UUID USER_ID = randomUUID();

    public static final String MARK_DOCUMENT_AS_ACTIONED_PUBLIC_EVENT = "public.stagingbulkscan.mark-as-actioned";
    private static final String DELETE_ACTIONED_DOCUMENT_EVENT = "stagingbulkscan.command.delete-actioned-document";
    private static final String ZIP_FILE_NAME = STRING.next();
    private static final String FIELD_SCAN_DOCUMENTS = "scanDocuments";
    private static final String FIELD_ID = "id";
    private static final String FIELD_ENVELOPE_ID = "scanEnvelopeId";
    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String FIELD_SUBMISSION_ID = "submissionId";
    private static final String FIELD_DOCUMENT_FILE_NAME = "documentFileName";
    private static final String FIELD_VENDOR_RECEIVED_DATE = "vendorReceivedDate";
    private static final String FIELD_CASE_URN = "caseUrn";
    private static final String FIELD_CASE_PTI_URN = "casePTIUrn";
    private static final String FIELD_PROSECUTOR_AUTHORITY_ID = "prosecutorAuthorityId";
    private static final String FIELD_PROSECUTOR_AUTHORITY_CODE = "prosecutorAuthorityCode";
    private static final String FIELD_DOCUMENT_NAME = "documentName";
    private static final String FIELD_ACTIONED = "actioned";
    private static final String FIELD_DELETED = "deleted";
    private static final String FIELD_ACTIONED_BY = "actionedBy";
    private static final String FIELD_THUMBNAILS = "thumbnails";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_STATUS_UPDATED_DATE = "statusUpdatedDate";
    private static final String FIELD_DELETED_DATE = "deletedDate";
    private static final String FIELD_STATUS_CODE = "statusCode";
    private static final String CASE_URN = "12295672";
    private static final String CASE_PTI_URN = "12295672";
    private static final String PROSECUTOR_ID = "AAAJJ11";
    private static final String PROSECUTOR_NAME = "TFL";
    private static final String SINGLE_PLEA = "Single Justice Procedure Notice - Plea (Single)";
    private static final String MC100 = "SJPMC100";

    public ScanEnvelopeHelper() {

    }

    public static void queryAndValidateActionedScanDocument(final UUID envelopeId, final UUID documentId, final String documentFileName, final String documentStatus, final String prosecutorAuthorityCode, final String documentName) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/MANUALLY_ACTIONED,AUTO_ACTIONED"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(anyOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_CASE_URN, equalTo(CASE_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_CODE, equalTo(prosecutorAuthorityCode)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(documentName)),
                                        withJsonPath(FIELD_STATUS, equalTo(documentStatus)),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue()))
                                ))))
                        )));
    }


    public static void queryAndValidateFollowUpScanDocument(final UUID envelopeId, final UUID documentId, final String documentFileName, final String documentStatus, final String prosecutorName, final String documentName) {

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/FOLLOW_UP"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_VENDOR_RECEIVED_DATE, equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                                        withJsonPath(FIELD_CASE_URN, equalTo(CASE_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_CODE, equalTo(prosecutorName)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(documentName)),
                                        withJsonPath(FIELD_STATUS, equalTo(documentStatus)),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue()))
                                ))))
                                )
                        ));
    }

    public static void queryAndValidateFollowUpScanDocumentWithCasePtiUrn(final UUID envelopeId, final UUID documentId, final String documentFileName, final String documentStatus, final String prosecutorName, final String documentName) {

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/FOLLOW_UP"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_VENDOR_RECEIVED_DATE, equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                                        withJsonPath(FIELD_CASE_PTI_URN, equalTo(CASE_PTI_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_CODE, equalTo(prosecutorName)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(documentName)),
                                        withJsonPath(FIELD_STATUS, equalTo(documentStatus)),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue()))
                                ))))
                                )
                        ));
    }


    public static void queryAndValidateExpiredScanDocument(final UUID envelopeId, final UUID documentId, final String documentFileName, final String documentStatus) {

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/FOLLOW_UP"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_VENDOR_RECEIVED_DATE, equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                                        withJsonPath(FIELD_CASE_URN, equalTo(CASE_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_CODE, equalTo(PROSECUTOR_NAME)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(SINGLE_PLEA)),
                                        withJsonPath(FIELD_STATUS, equalTo(documentStatus)),
                                        withJsonPath(FIELD_STATUS_CODE, equalTo(CASE_NOT_FOUND.name())),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue()))
                                ))))
                                )
                        ));
    }

    public static void validateEnvelopeForThumbnails(final JsonPath response,
                                                     final UUID envelopeId,
                                                     final UUID documentId,
                                                     final String documentFileNameOne,
                                                     final String documentFileNameTwo) {
        with(response.prettify()).assertThat(format("$.%s", FIELD_ID), equalTo(documentId.toString()));
        with(response.prettify()).assertThat(format("$.%s", FIELD_ENVELOPE_ID), equalTo(envelopeId.toString()));
        with(response.prettify()).assertThat(format("$.%s", FIELD_DOCUMENT_FILE_NAME), equalTo(documentFileNameOne));
        with(response.prettify()).assertThat(format("$.%s[0].%s", FIELD_THUMBNAILS, FIELD_DOCUMENT_FILE_NAME), equalTo(documentFileNameTwo));
    }


    public static void validateEnvelopeForDeletedScanDocument(final UUID envelopeId, final UUID documentId, final String documentFileName) {
        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/MANUALLY_ACTIONED,AUTO_ACTIONED"), QUERY_SCAN_ACTIONED_DOCUMENTS_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(anyOf(
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_ID), equalTo(documentId.toString())),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_ENVELOPE_ID), equalTo(envelopeId.toString())),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_DOCUMENT_FILE_NAME), equalTo(documentFileName)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_VENDOR_RECEIVED_DATE), equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_CASE_URN), equalTo(CASE_URN)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_PROSECUTOR_AUTHORITY_ID), equalTo(PROSECUTOR_ID)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_PROSECUTOR_AUTHORITY_CODE), equalTo(PROSECUTOR_NAME)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_DOCUMENT_NAME), equalTo(SINGLE_PLEA)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_ACTIONED), equalTo(TRUE)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_DELETED), equalTo(TRUE)),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_DELETED_DATE), Matchers.notNullValue()),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_ACTIONED_BY), equalTo(USER_ID_CROWN_COURT_USER.toString())),
                                withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_STATUS_UPDATED_DATE), Matchers.notNullValue())
                                )
                        ));
    }

    public static JsonObject buildPayloadWithAllValues(final String documentFileNameOne, final String documentFileNameTwo, final DocumentStatus documentStatus, final String documentName) {
        return createObjectBuilder()
                .add("scanEnvelopeId", randomUUID().toString())
                .add("envelopeClassification", "SAMPLE_ENVELOPE_CLASSIFICATION")
                .add("vendorPOBox", "1986")
                .add("jurisdiction", "JURISDICTION")
                .add("vendorOpeningDate", ZonedDateTime.now(UTC).toString())
                .add("zipFileCreatedDate", ZonedDateTime.now(UTC).toString())
                .add("zipFileName", ZIP_FILE_NAME + ".zip")
                .add("notes", "These are envelope test notes")
                .add("associatedScanDocuments", buildAssociatedDocuments(documentFileNameOne, documentFileNameTwo, documentStatus, SINGLE_PLEA))
                .add("extractedDate", ZonedDateTime.now(UTC).toString()).build();
    }

    private static JsonArrayBuilder buildAssociatedDocuments(final String documentFileNameOne, final String documentFileNameTwo, final DocumentStatus documentStatus, final String documentName) {
        return createArrayBuilder()
                .add(buildDocument(documentFileNameOne, randomUUID(), documentStatus, documentName))
                .add(buildDocument(documentFileNameTwo, randomUUID(), documentStatus, documentName));
    }

    private static JsonObjectBuilder buildDocument(final String fileName, final UUID scanDocumentId, final DocumentStatus documentStatus, final String documentName) {
        return createObjectBuilder()
                .add("scanDocumentId", scanDocumentId.toString())
                .add("caseUrn", CASE_URN)
                .add("prosecutorAuthorityId", PROSECUTOR_ID)
                .add("prosecutorAuthorityCode", PROSECUTOR_NAME)
                .add("documentControlNumber", STRING.next())
                .add("documentName", documentName)
                .add("scanningDate", ZonedDateTime.now(UTC).toString())
                .add("manualIntervention", "No")
                .add("nextAction", "NEXT_ACTION")
                .add("nextActionDate", ZonedDateTime.now(UTC).toString())
                .add("notes", "These are file notes for testing")
                .add("status", documentStatus.toString())
                .add("vendorReceivedDate", VENDOR_RECEIVED_DATE.toString())
                .add("fileName", fileName);
    }

    public static String buildScanDocumentPayload(final UUID scanDocumentId) {
        final JsonObject scanDocument = createObjectBuilder()
                .add(FIELD_SCAN_DOCUMENT_ID, scanDocumentId.toString())
                .build();

        return scanDocument.toString();
    }

    public static void addSjpCaseMaterialEvent(final UUID submissionId) {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_SJP_DOCUMENT_ADDED_EVENT)
                        .withUserId(USER_ID.toString()).build().asJsonObject())
                .add(FIELD_SUBMISSION_ID, submissionId.toString()).build()).build();
        final JsonObject payload = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseId", randomUUID().toString())
                .add("materialId", randomUUID().toString())
                .build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata, payload);

        final JmsMessageProducerClient privateJmsMessageProducerClient =  newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

        privateJmsMessageProducerClient.sendMessage(PUBLIC_SJP_DOCUMENT_ADDED_EVENT, event);
    }

    public static void addCcCaseMaterialEvent(final UUID submissionId) {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_CC_DOCUMENT_ADDED_EVENT)
                        .withUserId(USER_ID.toString()).build().asJsonObject())
                .add(FIELD_SUBMISSION_ID, submissionId.toString()).build()).build();

        final JsonObject payload = getFileContentAsJson("stub-data/public.progression.court-document-added.json", emptyMap());

        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata, payload);
        final JmsMessageProducerClient privateJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        privateJmsMessageProducerClient.sendMessage(PUBLIC_CC_DOCUMENT_ADDED_EVENT, event);
    }

    public static void rejectDocument(final UUID submissionId) {
        final Metadata materialRejectedEventMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_CASE_REJECTED)
                        .withUserId(USER_ID.toString()).build().asJsonObject())
                .add(FIELD_SUBMISSION_ID, submissionId.toString()).build()).build();

        final JsonObject payload = getFileContentAsJson("stub-data/public.prosecutioncasefile.material-rejected.json", emptyMap());

        final JsonEnvelope event = envelopeFrom(materialRejectedEventMetadata, payload);


        final JmsMessageProducerClient privateJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        privateJmsMessageProducerClient.sendMessage(PUBLIC_CASE_REJECTED, event);
    }

    public static void expireDocument(final UUID submissionId) {
        final Metadata bulkscanMaterialMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_BULK_SCAN_MATERIAL_FOLLOWUP)
                        .withUserId(USER_ID.toString()).build().asJsonObject())
                .add(FIELD_SUBMISSION_ID, submissionId.toString()).build()).build();

        final JsonObject payload = getFileContentAsJson("stub-data/public.prosecutioncasefile.bulkscan-material-followup.json", emptyMap());

        final JsonEnvelope event = envelopeFrom(bulkscanMaterialMetadata, payload);
        final JmsMessageProducerClient privateJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        privateJmsMessageProducerClient.sendMessage(PUBLIC_BULK_SCAN_MATERIAL_FOLLOWUP, event);
    }

    public static void queryAndValidateFinancialMeansDocument(final UUID envelopeId, final UUID documentId, final String documentFileName) {

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/AUTO_ACTIONED"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .until(status().is(OK),
                        payload().isJson(anyOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_CASE_URN, equalTo(CASE_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(MC100)),
                                        withJsonPath(FIELD_STATUS, equalTo(AUTO_ACTIONED.toString())),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue())),
                                        withJsonPath(FIELD_VENDOR_RECEIVED_DATE, is(notNullValue())),
                                        withJsonPath(FIELD_ACTIONED_BY, is(notNullValue())),
                                        withJsonPath(FIELD_DELETED, is(false))
                                ))))
                        )));
    }

    public static void queryAndValidateFinancialMeansDocumentForFollowup(final UUID envelopeId, final UUID documentId, final String documentFileName) {

        poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/FOLLOW_UP"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                .timeout(20, TimeUnit.SECONDS)
                .until(status().is(OK),
                        payload().isJson(anyOf(
                                withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                        withJsonPath(FIELD_ID, equalTo(documentId.toString())),
                                        withJsonPath(FIELD_ENVELOPE_ID, equalTo(envelopeId.toString())),
                                        withJsonPath(FIELD_DOCUMENT_FILE_NAME, equalTo(documentFileName)),
                                        withJsonPath(FIELD_CASE_URN, equalTo(CASE_URN)),
                                        withJsonPath(FIELD_PROSECUTOR_AUTHORITY_ID, equalTo(PROSECUTOR_ID)),
                                        withJsonPath(FIELD_DOCUMENT_NAME, equalTo(MC100)),
                                        withJsonPath(FIELD_STATUS, equalTo(FOLLOW_UP.toString())),
                                        withJsonPath(FIELD_STATUS_UPDATED_DATE, is(notNullValue())),
                                        withJsonPath(FIELD_VENDOR_RECEIVED_DATE, is(notNullValue())),
                                        withJsonPath(FIELD_ACTIONED_BY, is(notNullValue())),
                                        withJsonPath(FIELD_STATUS_CODE, is(StatusCode.DEFENDANT_DETAILS_UPDATED.toString())),
                                        withJsonPath(FIELD_DELETED, is(false))
                                ))))
                        )));
    }
}
