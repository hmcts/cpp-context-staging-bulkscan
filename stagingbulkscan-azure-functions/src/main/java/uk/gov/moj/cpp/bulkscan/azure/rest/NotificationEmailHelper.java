package uk.gov.moj.cpp.bulkscan.azure.rest;

import org.apache.commons.lang3.StringUtils;
import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanConfigurationMissingException;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static java.lang.System.getenv;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.client.Entity.entity;

public class NotificationEmailHelper {

    private static final String NOTIFICATION_EMAIL_API_URL_KEY = "notification_email_api_url";
    private static final String NOTIFICATION_NOTIFY_SYSTEM_USER = "notification-notify-system-user";
    private static final String NOTIFICATION_EMAIL_TEMPLATE_ID = "notification_email_template_id";
    private static final String NOTIFY_EMAIL_CONTENT_TYPE = "application/vnd.notificationnotify.email+json";
    private static final String PERSONALISATION = "personalisation";
    private ClientWrapper clientWrapper = new ClientWrapper();

    public Response sendNotificationEmail(String notificationId, String senderEmailAddress, String invalidFileNames, String subject, Logger logger) {
        final JsonObject payload = buildPayload(senderEmailAddress, invalidFileNames, subject,  logger);
        final String notificationAPIUrl = getNotificationAPIUrl();
        if (StringUtils.isEmpty(notificationAPIUrl)) {
            logger.severe("Environment variable not found: notification_email_api_url");
            throw new BulkScanConfigurationMissingException("Environment variable not found: notification_email_api_url");
        }
        final String url = String.format(notificationAPIUrl, notificationId);
        final MultivaluedMap<String, Object> headers = constructHeaders(logger);
        return postEmailWith(url, payload, headers);
    }

    protected Client initializeClient() {
        return clientWrapper.getClient();
    }

    protected void setClientWrapper(ClientWrapper clientWrapper) {
        this.clientWrapper = clientWrapper;
    }

    private MultivaluedMap<String, Object> constructHeaders(Logger logger) {

        final String cppUserId = getCPPUID();
        if (StringUtils.isEmpty(cppUserId)) {
            logger.severe("Environment variable not found: staging-prosecutor-user");
            throw new BulkScanConfigurationMissingException("Environment variable not found: staging-prosecutor-user");
        }
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, NOTIFY_EMAIL_CONTENT_TYPE);
        headers.add("CJSCPPUID", cppUserId);
        return headers;
    }

    private JsonObject buildPayload(String senderEmailAddress, String invalidFileNames, String subject, Logger logger) {

        final String templateId = getTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            logger.severe("Environment variable not found: notification_email_template_id");
            throw new BulkScanConfigurationMissingException("Environment variable not found: notification_email_template_id");
        }
        return createObjectBuilder()
                .add("templateId", templateId)
                .add("sendToAddress", senderEmailAddress)
                .add(PERSONALISATION, createObjectBuilder()
                        .add("file_names", invalidFileNames)
                        .add("subject", subject)
                        .build())
                .build();
    }



    private Response postEmailWith(String url, JsonObject payload, MultivaluedMap<String, Object> headers) {
        final Client client = initializeClient();
        final MediaType customMediaType = MediaType.valueOf(NOTIFY_EMAIL_CONTENT_TYPE);
        return client.target(url).request().headers(headers)
                .post(entity(payload.toString(), customMediaType));
    }

    protected String getNotificationAPIUrl() {
        return getenv(NOTIFICATION_EMAIL_API_URL_KEY);
    }

    protected String getCPPUID() {
        return getenv(NOTIFICATION_NOTIFY_SYSTEM_USER);
    }

    protected String getTemplateId() {
        return getenv(NOTIFICATION_EMAIL_TEMPLATE_ID);
    }
}
