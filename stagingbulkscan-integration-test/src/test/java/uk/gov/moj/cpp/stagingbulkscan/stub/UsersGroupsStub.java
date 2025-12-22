package uk.gov.moj.cpp.stagingbulkscan.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WiremockTestHelper.waitForStubToBeReady;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UsersGroupsStub {

    private static final String USER_GROUPS_USER_DETAILS_QUERY_URL = "/usersgroups-service/query/api/rest/usersgroups/users/%s";
    private static final String USER_GROUPS_USER_DETAILS_QUERY_MEDIA_TYPE = "application/vnd.usersgroups.user-details+json";

    public static JsonObject stubForUserDetails(final UUID userId, final String prosecutingAuthorityAccess) {
        final String userLastName = nonNull(prosecutingAuthorityAccess) ? "User with prosecuting access" : "User without prosecuting access";
        return stubForUserDetails(userId, "IT", userLastName, prosecutingAuthorityAccess);
    }

    public static JsonObject stubForUserDetails(final UUID userId) {
        final String prosecutingAuthority = null;
        return stubForUserDetails(userId, prosecutingAuthority);
    }

    public static JsonObject stubForUserDetails(final UUID userId, final String firstName, final String lastName, final String prosecutingAuthority) {
        final JsonObjectBuilder userDetailsBuilder = Json.createObjectBuilder()
                .add("userId", userId.toString())
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("email", "it@test.net");

        if (nonNull(prosecutingAuthority)) {
            userDetailsBuilder.add("prosecutingAuthorityAccess", prosecutingAuthority);
        }

        final JsonObject userDetails = userDetailsBuilder.build();

        stubPayloadForUserId(userDetails.toString(), userId, USER_GROUPS_USER_DETAILS_QUERY_URL, USER_GROUPS_USER_DETAILS_QUERY_MEDIA_TYPE);
        return userDetails;
    }

    private static void stubPayloadForUserId(final String responsePayload, final UUID userId, final String queryUrl, final String mediaType) {
        final String url = format(queryUrl, userId);

        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responsePayload)));

        waitForStubToBeReady(url, mediaType);
    }
}
