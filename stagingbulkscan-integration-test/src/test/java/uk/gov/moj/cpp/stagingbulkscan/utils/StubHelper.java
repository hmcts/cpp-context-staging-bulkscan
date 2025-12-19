package uk.gov.moj.cpp.stagingbulkscan.utils;


import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.waitAtMost;
import static javax.ws.rs.client.Entity.entity;
import static uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory.clientBuilder;
import static uk.gov.moj.cpp.stagingbulkscan.utils.WiremockTestHelper.getBaseUri;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.time.Duration;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class StubHelper {

    public static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(ofSeconds(10)).until(() -> restClient.postCommand(getBaseUri() + resource, mediaType, "").getStatus() == expectedStatus.getStatusCode());
    }

    public static void waitForGetStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(ofSeconds(10)).until(() -> restClient.query(getBaseUri() + resource, mediaType).getStatus() == expectedStatus.getStatusCode());
    }

    public static void waitForPutStubToBeReady(final String resource, final String contentType, final Response.Status expectedStatus) {
        waitAtMost(ofSeconds(10))
                .until(() -> put(getBaseUri() + resource, contentType)
                        .getStatus() == expectedStatus.getStatusCode());
    }

    private static Response put(final String url, final String contentType) {
        return clientBuilder().build()
                .target(url)
                .request()
                .put(entity("", MediaType.valueOf(contentType)));
    }

}
