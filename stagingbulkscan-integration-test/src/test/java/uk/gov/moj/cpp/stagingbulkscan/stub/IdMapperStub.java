package uk.gov.moj.cpp.stagingbulkscan.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.cpp.stagingbulkscan.utils.StubHelper.waitForGetStubToBeReady;
import static uk.gov.moj.cpp.stagingbulkscan.utils.StubHelper.waitForPostStubToBeReady;

import java.time.ZonedDateTime;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.ws.rs.core.Response;

import org.apache.http.HttpHeaders;

public class IdMapperStub {

    public static void stubGetFromIdMapper(final String sourceType, final String sourceId, final String targetType, final String targetId) {

        final String responseBody = JsonObjects.createObjectBuilder()
                .add("mappingId", UUID.randomUUID().toString())
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId)
                .add("targetType", targetType)
                .add("createdAt", ZonedDateTime.now().toString()).build().toString();

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings.*"))
                .withQueryParam("sourceId", equalTo(sourceId))
                .withQueryParam("sourceType", equalTo(sourceType))
                .withQueryParam("targetType", equalTo(targetType))
                .withHeader("Accept", equalTo("application/vnd.systemid.mapping+json"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withBody(responseBody)
                )
        );

        waitForGetStubToBeReady(String.format("/system-id-mapper-api/rest/systemid/mappings?sourceId=%s&sourceType=%s&targetType=%s", sourceId, sourceType, targetType), "application/vnd.systemid.mapping+json", OK);
    }

    public static void stubForIdMapperSuccess(final Response.Status status, final UUID id) {
        final String path = "/system-id-mapper-api/rest/systemid.*";
        final String mime = "application/vnd.systemid.map+json";

        stubFor(post(urlPathMatching(path))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(mime))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())
                        .withBody(JsonObjects.createObjectBuilder().add("id", id.toString()).build().toString())
                )
        );

        waitForPostStubToBeReady(path, mime, status);
    }

    public static void stubForIdMapperSuccess(final Response.Status status) {
        stubForIdMapperSuccess(status, UUID.randomUUID());
    }
}
