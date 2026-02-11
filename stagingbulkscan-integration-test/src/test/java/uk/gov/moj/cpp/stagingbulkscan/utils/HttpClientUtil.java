package uk.gov.moj.cpp.stagingbulkscan.utils;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Assertions;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {
    private static final RestClient restClient = new RestClient();

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);
    private static final String BASE_URI = System.getProperty("baseUri", "http://localhost:8080");
    private static final String WRITE_BASE_URL = BASE_URI;
    private static final String READ_BASE_URL = BASE_URI;

    public static UUID makePostCall(final UUID USER_ID, final String url, final String mediaType, final String payload) {
        return makePostCall(USER_ID, url, mediaType, payload, Response.Status.ACCEPTED);
    }

    public static UUID makePostCall(final UUID userId, final String url, final String mediaType, final String payload, final Response.Status expectedStatus) {
        final UUID correlationId = UUID.randomUUID();

        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        map.add(HeaderConstants.CLIENT_CORRELATION_ID, correlationId);

        final String writeUrl = getWriteUrl(url);
        final Response response = restClient.postCommand(writeUrl, mediaType, payload, map);
        LOGGER.info("Post call made: \n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tUser = {}\n",
                writeUrl, mediaType, payload, userId);
        String value = null;
        try {
            value = response.readEntity(String.class);
        } catch (Exception e) {
            // ignore
        }
        assertThat(format("Post returned not expected status code with body: %s", value),
                response.getStatus(), is(expectedStatus.getStatusCode()));

        return correlationId;
    }

    public static Response makeGetCall(final String url, final String mediaType, final UUID userId) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        map.add(HttpHeaders.ACCEPT, mediaType);
        final String readUrl = getReadUrl(url);
        LOGGER.info("Get call made:" + System.lineSeparator()
                        + "Endpoint = {}" + System.lineSeparator()
                        + "Media type = {}" + System.lineSeparator()
                        + "User = {}" + System.lineSeparator(),
                readUrl, mediaType, userId);
        final Response response =  restClient.query(readUrl, mediaType, map);
        assertThat(format("GET returned not expected status code with body: %s", response.readEntity(String.class)),
                response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        return response;
    }

    private static String getWriteUrl(final String resource) {
        return WRITE_BASE_URL + resource;
    }

    public static String getReadUrl(final String resource) {
        return READ_BASE_URL + resource;
    }
}
