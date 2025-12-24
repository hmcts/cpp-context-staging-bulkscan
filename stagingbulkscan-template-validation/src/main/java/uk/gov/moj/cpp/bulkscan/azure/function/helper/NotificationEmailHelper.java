package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static java.lang.System.getenv;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.client.Entity.entity;


import uk.gov.moj.cpp.bulkscan.azure.function.model.ClientWrapper;
import uk.gov.moj.cpp.bulkscan.azure.function.exception.BulkScanConfigurationMissingException;

import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

public class NotificationEmailHelper {

    private static final String NOTIFICATION_EMAIL_API_URL_KEY = "notification_email_api_url";
    private static final String NOTIFICATION_NOTIFY_SYSTEM_USER = "notification-notify-system-user";

    private static final String FORM_CHECKER_EMAIL_TEMPLATE_ID = "form_checker_email_template_id";
    private static final String NOTIFY_EMAIL_CONTENT_TYPE = "application/vnd.notificationnotify.email+json";
    private static final String PERSONALISATION = "personalisation";
    private ClientWrapper clientWrapper = new ClientWrapper();

    public Response sendFormCheckerResultEmail(String nppValidationResponse, String notificationId, String sendToEmailAddress, String subject, Logger logger) {
        final JsonObject payload = buildFormCheckerPayload(sendToEmailAddress, subject,  nppValidationResponse, logger);
        final String apiUrl = getNotificationAPIUrl();
        if (StringUtils.isEmpty(apiUrl)) {
            logger.severe("Environment variable not found: notification_email_api_url");
            throw new BulkScanConfigurationMissingException("Environment variable not found: notification_email_api_url");
        }
        final String emailUrl = String.format(apiUrl, notificationId);
        final MultivaluedMap<String, Object> headers = constructHeaders(logger);
        return postEmailWith(emailUrl, payload, headers);
    }

    protected Client initializeClient() {
        return clientWrapper.getClient();
    }

    protected void setClientWrapper(ClientWrapper clientWrapper) {
        this.clientWrapper = clientWrapper;
    }

    private JsonObject buildFormCheckerPayload(String sendToEmailAddress, String subject, String nppValidationResponse, Logger logger) {
        final String templateId = getFormCheckerTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            logger.severe("Environment variable not found: form_checker_email_template_id");
            throw new BulkScanConfigurationMissingException("Environment variable not found: form_checker_email_template_id");
        }
        return createObjectBuilder()
                .add("templateId", templateId)
                .add("sendToAddress", sendToEmailAddress)
                .add(PERSONALISATION, createObjectBuilder()
                        .add("subject", subject)
                        .add("result", nppValidationResponse)
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

    protected String getFormCheckerTemplateId() {
        return getenv(FORM_CHECKER_EMAIL_TEMPLATE_ID);
    }

    private MultivaluedMap<String, Object> constructHeaders(Logger logger) {

        final String userId = getCPPUID();
        if (StringUtils.isEmpty(userId)) {
            logger.severe("Environment variable not found: staging-prosecutor-user");
            throw new BulkScanConfigurationMissingException("Environment variable not found: staging-prosecutor-user");
        }
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, NOTIFY_EMAIL_CONTENT_TYPE);
        headers.add("CJSCPPUID", userId);
        return headers;
    }
}
