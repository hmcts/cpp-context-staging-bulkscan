package uk.gov.moj.cpp.stagingbulkscan.utils;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class RestPollerWithDefaults {

    public static final int DELAY_IN_MILLIS = 0;
    public static final int INTERVAL_IN_MILLIS = 100;

    public static RestPoller pollWithDefaults(final RequestParamsBuilder requestParamsBuilder) {
        return pollWithDefaults(requestParamsBuilder.build());
    }

    public static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams)
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)
                .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
    }

    public static JsonObject pollWithDefaultsUntilResponseIsJson(final RequestParams requestParams, final Matcher<? super ReadContext> matcher) {
        final ResponseData responseData = pollWithDefaults(requestParams)
                .until(anyOf(
                        allOf(status().is(OK), payload().isJson(matcher)),
                        status().is(INTERNAL_SERVER_ERROR),
                        status().is(FORBIDDEN)
                ));

        if (responseData.getStatus() != OK) {
            fail("Polling interrupted, please fix the error before continue. Status code: " + responseData.getStatus());
        }

        return JsonHelper.getJsonObject(responseData.getPayload());
    }

    private RestPollerWithDefaults() {
    }
}
