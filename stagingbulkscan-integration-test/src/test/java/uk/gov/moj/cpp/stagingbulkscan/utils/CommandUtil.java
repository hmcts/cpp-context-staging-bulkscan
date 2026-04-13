package uk.gov.moj.cpp.stagingbulkscan.utils;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.stagingbulkscan.it.BaseIntegrationTest.USER_ID_CROWN_COURT_USER;
import static uk.gov.moj.cpp.stagingbulkscan.it.BaseIntegrationTest.USER_ID_SYSTEM_USER;
import static uk.gov.moj.cpp.stagingbulkscan.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.cpp.stagingbulkscan.utils.HttpClientUtil.makePostCall;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueueUtil.privateCommandQueue;
import static uk.gov.moj.cpp.stagingbulkscan.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WireMockStubUtils.setupAsSystemUser;

import uk.gov.moj.cpp.stagingbulkscan.stub.IdMapperStub;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

public class CommandUtil {

    private static final String REGISTER_SCAN_ENVELOPE_URL = "/stagingbulkscan-command-api/command/api/rest/stagingbulkscan/scan-envelope";
    private static final String REGISTER_SCAN_ENVELOPE_REQUEST_TYPE = "application/vnd.stagingbulkscan.register-scan-envelope+json";

    private static final String DOCUMENT_MARK_AS_ACTION_URL = "/stagingbulkscan-command-api/command/api/rest/stagingbulkscan/scan-envelope/%s";
    private static final String DOCUMENT_MARK_AS_ACTION_REQUEST_TYPE = "application/vnd.stagingbulkscan.mark-as-action+json";

    public static void registerScanEnvelope(final JsonObject payload) {
        stubGetFromIdMapper(payload);

        makePostCall(USER_ID_SYSTEM_USER,
                REGISTER_SCAN_ENVELOPE_URL,
                REGISTER_SCAN_ENVELOPE_REQUEST_TYPE,
                payload.toString());
    }

    private static void stubGetFromIdMapper(final JsonObject registerScanEnvelopePayload) {
        final List<JsonObject> jsonObjects = registerScanEnvelopePayload.getJsonArray("associatedScanDocuments").getValuesAs(JsonObject.class);

        final String caseReference = jsonObjects.stream()
                .map(jsonObject -> jsonObject.getString("caseUrn") + ":" + jsonObject.getString("prosecutorAuthorityId"))
                .findFirst().orElse(null);

        IdMapperStub.stubGetFromIdMapper("OU_URN", caseReference, "CASE_FILE_ID", randomUUID().toString());
    }

    public static void actionScanDocument(final UUID scanEnvelopeId, final String payload) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");
        makePostCall(USER_ID_CROWN_COURT_USER,
                format(DOCUMENT_MARK_AS_ACTION_URL, scanEnvelopeId.toString()),
                DOCUMENT_MARK_AS_ACTION_REQUEST_TYPE,
                payload);
    }

    public static void deleteScanDocument(final UUID envelopeId, final UUID documentId) {
        final String eventName = "stagingbulkscan.command.delete-actioned-document";
        sendMessage(
                privateCommandQueue.createProducer(),
                eventName,
                createObjectBuilder()
                        .add("scanEnvelopeId", envelopeId.toString())
                        .add("scanDocumentId", documentId.toString())
                        .build(),
                metadataWithRandomUUID(eventName).withUserId(randomUUID().toString()).build());
    }
}
