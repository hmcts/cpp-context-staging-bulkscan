package uk.gov.moj.cpp.stagingbulkscan.utils;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.stagingbulkscan.it.BaseIntegrationTest.USER_ID_CROWN_COURT_USER;
import static uk.gov.moj.cpp.stagingbulkscan.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.cpp.stagingbulkscan.utils.HttpClientUtil.getReadUrl;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WireMockStubUtils.setupAsSystemUser;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;

import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;

public class QueryUtil {
    private static JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    public static final String QUERY_SCAN_DOCUMENTS_URL = "/stagingbulkscan-query-api/query/api/rest/stagingbulkscan/scan-documents";
    public static final String QUERY_SCAN_DOCUMENT_REQUEST_TYPE = "application/vnd.stagingbulkscan.get-all-documents-by-status+json";
    public static final String QUERY_SCAN_GET_DOCUMENT_URL = "/stagingbulkscan-query-api/query/api/rest/stagingbulkscan/scan-envelope/%s/scan-documents/%s";
    public static final String QUERY_SCAN_GET_DOCUMENT_REQUEST_TYPE = "application/vnd.stagingbulkscan.get-scan-document+json";
    private static final String FIELD_ID = "id";
    private static final long POLL_INTERVAL_IN_SECONDS = 5;
    private static final long POLL_TIMEOUT_IN_SECONDS = 120;

    public static ScanDocumentsResponse fetchLiveScanDocumentsContains(final String documentFileName) {
        return getScanDocumentsResponse(documentFileName, "/FOLLOW_UP");
    }

    public static ScanDocumentsResponse fetchPendingScanDocumentsContains(final String documentFileName) {
        return getScanDocumentsResponse(documentFileName, "/PENDING");
    }

    public static ScanDocumentsResponse fetchExpiredScanDocumentsContains(final String documentFileName) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");

        final ResponseData responseData =
                poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/FOLLOW_UP"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                        .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                        .timeout(20, SECONDS)
                        .until(status().is(OK),
                                payload().isJson(anyOf(
                                        withJsonPath("$.scanDocuments[*]", hasItem(isJson(allOf(
                                                withJsonPath("documentFileName", equalTo(documentFileName)),
                                                withJsonPath("statusCode", equalTo("CASE_NOT_FOUND"))
                                        )))))));

        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(responseData.getPayload());

        return jsonObjectToObjectConverter.convert(jsonObject, ScanDocumentsResponse.class);
    }

    public static ScanDocumentsResponse fetchActionedScanDocuments(final String documentFileName) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");

        final ResponseData responseData =
                poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + "/MANUALLY_ACTIONED,AUTO_ACTIONED"), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                        .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                        .until(status().is(OK),
                                payload().that(containsString(documentFileName)));

        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(responseData.getPayload());

        final ScanDocumentsResponse scanDocumentsResponse = jsonObjectToObjectConverter.convert(jsonObject, ScanDocumentsResponse.class);
        return scanDocumentsResponse;
    }

    private static ScanDocumentsResponse getScanDocumentsResponse(String documentFileName, String documentStatus) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");

        final ResponseData responseData =
                poll(requestParams(getReadUrl(QUERY_SCAN_DOCUMENTS_URL + documentStatus), QUERY_SCAN_DOCUMENT_REQUEST_TYPE)
                        .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                        .timeout(20, SECONDS)
                        .until(status().is(OK),
                                payload().that(containsString(documentFileName)));

        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        final JsonObject jsonObject = new StringToJsonObjectConverter().convert(responseData.getPayload());

        return jsonObjectToObjectConverter.convert(jsonObject, ScanDocumentsResponse.class);
    }

    public static JsonPath getScanDocument(final UUID envelopeId,
                                           final UUID documentId) {
        setupAsSystemUser(USER_ID_CROWN_COURT_USER);
        stubForUserDetails(USER_ID_CROWN_COURT_USER, "ALL");

        final ResponseData responseData =
                poll(requestParams(getReadUrl(format(QUERY_SCAN_GET_DOCUMENT_URL, envelopeId.toString(), documentId.toString())), QUERY_SCAN_GET_DOCUMENT_REQUEST_TYPE)
                        .withHeader(HeaderConstants.USER_ID, USER_ID_CROWN_COURT_USER))
                        .with().logging()
                        .pollInterval(POLL_INTERVAL_IN_SECONDS, SECONDS)
                        .timeout(POLL_TIMEOUT_IN_SECONDS, SECONDS)
                        .pollDelay(0, MILLISECONDS)
                        .until(payload().isJson(withJsonPath(format("$.%s", FIELD_ID))));

        setField(jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        return new JsonPath(responseData.getPayload());
    }
}
