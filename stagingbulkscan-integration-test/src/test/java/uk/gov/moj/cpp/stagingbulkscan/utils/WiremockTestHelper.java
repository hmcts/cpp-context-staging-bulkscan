package uk.gov.moj.cpp.stagingbulkscan.utils;


import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.stagingbulkscan.utils.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import javax.ws.rs.core.Response.Status;


/**
 * Provides helper methods for tests to interact with Wiremock instance
 */
public class WiremockTestHelper {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForStubToBeReady(String resource, String mediaType) {
        waitForStubToBeReady(resource, mediaType, Status.OK);
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();
        pollWithDefaults(requestParams).until(status().is(expectedStatus));
    }

    public static String getBaseUri() {
        return BASE_URI;
    }
}
