package uk.gov.moj.cpp.stagingbulkscan.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.collect.ImmutableMap.of;
import static java.time.Duration.ofMinutes;
import static org.awaitility.Awaitility.await;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.stagingbulkscan.utils.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.json.JSONObject;

public class SjpServiceStub {
    public static final String SJP_QUERY_BY_URN = "/sjp-service/query/api/rest/sjp/cases.*";
    public static final String SJP_QUERY_BY_CASE_ID = "/sjp-service/query/api/rest/sjp/cases/%s";
    public static final String SJP_CASE_QUERY_BY_URN_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn+json";
    public static final String SJP_CASE_QUERY_MEDIA_TYPE = "application/vnd.sjp.query.case+json";
    public static final String SJP_QUERY_ALL_FINANCIAL_MEANS = "/sjp-service/query/api/rest/sjp/defendant/%s/all-financial-means";
    public static final String SJP_CASE_QUERY_FINANCIAL_MEANS_MEDIA_TYPE = "application/vnd.sjp.query.all-financial-means+json";

    private static final String SJP_ALL_FINANCIAL_MEANS_COMMAND_URL = "/sjp-service/command/api/rest/sjp/cases/%s/defendant/%s/all-financial-means";
    private static final String SJP_ALL_FINANCIAL_MEANS_MEDIA_TYPE = "application/vnd.sjp.update-all-financial-means+json";
    private static final String SJP_NI_NUMBER_COMMAND_URL = "/sjp-service/command/api/rest/sjp/cases/%s/defendant/%s/nationalInsuranceNumber";
    private static final String SJP_NI_NUMBER_MEDIA_TYPE = "application/vnd.sjp.update-defendant-national-insurance-number+json";
    private static final String SJP_DEFENDANT_COMMAND_URL = "/sjp-service/command/api/rest/sjp/cases/%s/defendant/%s";
    private static final String SJP_DEFENDANT_DETAILS_MEDIA_TYPE = "application/vnd.sjp.update-defendant-details+json";

    public static final String SJP_QUERY_RESULTS_BY_CASE_ID = "/sjp-service/query/api/rest/sjp/cases/%s/results";
    public static final String SJP_QUERY_RESULTS_BY_CASE_MEDIA_TYPE = "application/vnd.sjp.query.case-results+json";

    private static final String SJP_CASE_DETAILS_URL = "/sjp-service/query/api/rest/sjp/cases";
    private static final String SJP_CASE_DETAILS_BY_URN_QUERY_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn+json";
    private static final String SJP_UPDATE_PLEA_COMMAND_URL = "/sjp-service/command/api/rest/sjp/cases/%s/set-pleas";
    private static final String SJP_UPDATE_PLEA_COMMAND_MEDIA_TYPE = "application/vnd.sjp.set-pleas+json";

    public static void stubSjpQueryByUrn(final JsonObject payload, final String caseUrn) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        stubFor(get(urlPathMatching(SJP_QUERY_BY_URN))
                .withHeader(ACCEPT, equalTo(SJP_CASE_QUERY_BY_URN_MEDIA_TYPE))
                .withQueryParam("urn", equalTo(caseUrn))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_CASE_QUERY_BY_URN_MEDIA_TYPE)
                        .withBody(payload.toString())));
    }

    public static void stubSjpQueryByCaseId(final JsonObject payload, final UUID caseId) {

        InternalEndpointMockUtils.stubPingFor("sjp-service");

        final String urlPath = String.format(SJP_QUERY_BY_CASE_ID, caseId.toString());

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(SJP_CASE_QUERY_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_CASE_QUERY_MEDIA_TYPE)
                        .withBody(payload.toString())));
    }

    public static void stubSjpQueryResultsByCaseId(final UUID caseId, final JsonObject payload) {

        InternalEndpointMockUtils.stubPingFor("sjp-service");

        final String urlPath = String.format(SJP_QUERY_RESULTS_BY_CASE_ID, caseId.toString());

        stubFor(get(urlPathEqualTo(urlPath))
                .withHeader(ACCEPT, equalTo(SJP_QUERY_RESULTS_BY_CASE_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_QUERY_RESULTS_BY_CASE_MEDIA_TYPE)
                        .withBody(payload.toString())));
    }

    public static void stubSjpQueryAllFinancialMeans(final JsonObject payload, final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String urlPath = format(SJP_QUERY_ALL_FINANCIAL_MEANS, defendantId.toString());
        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_CASE_QUERY_FINANCIAL_MEANS_MEDIA_TYPE)
                        .withBody(payload.toString())));
    }

    public static void stubSjpFinancialMeansCommand(final UUID caseId, final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String urlPath = format(SJP_ALL_FINANCIAL_MEANS_COMMAND_URL, caseId.toString(), defendantId.toString());
        stubFor(post(urlPathEqualTo(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(SJP_ALL_FINANCIAL_MEANS_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void stubSjpNiNumberCommand(final UUID caseId, final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String urlPath = format(SJP_NI_NUMBER_COMMAND_URL, caseId.toString(), defendantId.toString());
        stubFor(post(urlPathEqualTo(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(SJP_NI_NUMBER_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void stubSjpSetDefendantCommand(final UUID caseId, final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String urlPath = format(SJP_DEFENDANT_COMMAND_URL, caseId.toString(), defendantId.toString());
        stubFor(post(urlPathEqualTo(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(SJP_DEFENDANT_DETAILS_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                    .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifySetDefendantCommandInvoked(final Predicate<JSONObject> commandPayloadPredicate, final UUID caseId,
                                                        final UUID defendantId) {
        await("set defendant command sent").timeout(ofMinutes(2)).until(() ->
                findAll(postRequestedFor(urlPathEqualTo(String.format(SJP_DEFENDANT_COMMAND_URL, caseId, defendantId))))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }

    public static void registerCaseResults(final UUID caseId, final UUID offenceId, final UUID offenceId_1,
                                        final String verdict1, final String verdict2) {

        final JsonObject payload = getFileContentAsJson(
                "stub-data/sjp.query.case-results.json", of("caseId", caseId,
                                "offenceId_1", offenceId,
                                "offenceId_2", offenceId_1,
                                "verdict_1", verdict1,
                                "verdict_2", verdict2));

        stubSjpQueryResultsByCaseId(caseId, payload);
    }

    public static void registerCaseDetailsByUrn(final String caseUrn, final UUID caseId, UUID offenceId, UUID defendantId) {

        final JsonObject payload = getFileContentAsJson(
                "stub-data/sjp.query.case-details.json", of("caseId", caseId,
                "offenceId", offenceId,
                "defendantId", defendantId));

        stubSjpCaseDetails(caseUrn, payload);

    }

    public static void registerCaseDetailsByCaseId(final UUID caseId, UUID offenceId, UUID defendantId) {

        final JsonObject payload = getFileContentAsJson("stub-data/sjp.query.case-details.json", of("caseId", caseId,
                "offenceId", offenceId,
                "defendantId", defendantId));

        stubSjpQueryByCaseId(payload, caseId);

    }

    public static void registerCaseDetailsWithMultipleOffences(final String caseUrn, final UUID caseId,
                                                               final UUID offenceId, final UUID offenceId_1,
                                                               final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");

        final JsonObject payload = getFileContentAsJson("stub-data/case-with-multiple-offences.json", of("caseId", caseId,
                "offenceId", offenceId,
                "offenceId_1", offenceId_1,
                "defendantId", defendantId,
                "hasFinalDecision", false));

        stubSjpCaseDetails(caseUrn, payload);
        stubSjpQueryByCaseId(payload, caseId);


    }

    public static void registerCaseDetailsWithOffenceHasFinalDecision(final String caseUrn, final UUID caseId,
                                                                      final UUID offenceId, final UUID offenceId_1,
                                                                      final UUID defendantId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");

        final JsonObject payload = getFileContentAsJson("stub-data/case-with-multiple-offences.json", of("caseId", caseId,
                "offenceId", offenceId,
                "offenceId_1", offenceId_1,
                "defendantId", defendantId,
                "hasFinalDecision", true));

        stubSjpCaseDetails(caseUrn, payload);
        stubSjpQueryByCaseId(payload, caseId);

    }

    private static void stubSjpCaseDetails(String caseUrn, JsonObject payload) {
        stubFor(get(urlPathEqualTo(SJP_CASE_DETAILS_URL))
                .withQueryParam("urn", equalTo(caseUrn))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_CASE_DETAILS_BY_URN_QUERY_MEDIA_TYPE)
                        .withBody(payload.toString())));

        waitForStubToBeReady(SJP_CASE_DETAILS_URL + "?urn=" + caseUrn, SJP_CASE_DETAILS_BY_URN_QUERY_MEDIA_TYPE);
    }

    public static void stubSjpSetPleaCommand(final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String urlPath = String.format(SJP_UPDATE_PLEA_COMMAND_URL, caseId.toString());
        stubFor(post(urlPathEqualTo(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(SJP_UPDATE_PLEA_COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));
    }

    public static void stubSetPleaCommandInvoked(final UUID caseId) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");

        String url = String.format(SJP_UPDATE_PLEA_COMMAND_URL, caseId);
        System.out.println(url);
        stubFor(post(urlPathMatching(url))
                .withHeader(CONTENT_TYPE, equalTo(SJP_UPDATE_PLEA_COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifySetPleaCommandInvoked(final Predicate<JSONObject> commandPayloadPredicate, final UUID caseId) {
        await("set pleas command sent").until(() ->
                findAll(postRequestedFor(urlPathEqualTo(format(SJP_UPDATE_PLEA_COMMAND_URL, caseId))))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }

    public static List<JSONObject> pollForSetPleaCommandInvokedForMultipleOffences(final Matcher<Collection<?>> matcher, final UUID caseId) {
        try {
            final List<JSONObject> postRequests = await().until(() ->
                    findAll(postRequestedFor(urlPathMatching(String.format(SJP_UPDATE_PLEA_COMMAND_URL, caseId))))
                            .stream()
                            .map(LoggedRequest::getBodyAsString)
                            .map(JSONObject::new)
                            .collect(Collectors.toList()), matcher);

            return postRequests;
        } catch (final ConditionTimeoutException timeoutException) {
            return emptyList();
        }
    }

}
