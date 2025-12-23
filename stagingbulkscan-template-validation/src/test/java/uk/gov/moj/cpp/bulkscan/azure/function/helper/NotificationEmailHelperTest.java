package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.bulkscan.azure.function.exception.BulkScanConfigurationMissingException;
import uk.gov.moj.cpp.bulkscan.azure.function.model.ClientWrapper;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class NotificationEmailHelperTest {

    private final Logger logger = mock(Logger.class);
    private final Client client = mock(Client.class);
    private final ClientWrapper clientWrapper = mock(ClientWrapper.class);
    private final WebTarget webTarget = mock(WebTarget.class);
    private final Invocation.Builder builder = mock(Invocation.Builder.class);
    private ArgumentCaptor<Entity> entityArgumentCaptor = ArgumentCaptor.forClass(Entity.class);

    private NotificationEmailHelper notificationEmailHelper;

    @BeforeEach
    public void setUp() {
        notificationEmailHelper = spy(new NotificationEmailHelper());
        notificationEmailHelper.setClientWrapper(clientWrapper);
    }

    @Test
    public void givenAllResourcesAvailable_sendNotificationEmail_shouldReturnAcceptedStatus() {

        when(notificationEmailHelper.getNotificationAPIUrl()).thenReturn("http://notificationnotify.com");
        when(notificationEmailHelper.getFormCheckerTemplateId()).thenReturn("5465466 546 465");
        when(notificationEmailHelper.getCPPUID()).thenReturn("87yy98798uo");

        Response response = Response.accepted().build();

        when(notificationEmailHelper.initializeClient()).thenReturn(client);
        when(client.target(Mockito.anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.headers(any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);

        assertEquals(notificationEmailHelper.sendFormCheckerResultEmail("validationResponse", UUID.randomUUID().toString(),
                "test@test.com", "Form Validation", logger), response);

        verify(builder).post(entityArgumentCaptor.capture());
        assertTrue(entityArgumentCaptor.getValue().toString()
                .contains("{\"templateId\":\"5465466 546 465\",\"sendToAddress\":\"test@test.com\",\"personalisation\":{\"subject\":\"Form Validation\",\"result\":\"validationResponse\"}}"));
    }

    @Test
    public void givenApiUrlIsEmpty_sendNotificationEmail_shouldThrowsException() {

        when(notificationEmailHelper.getFormCheckerTemplateId()).thenReturn("5465466 546 465");
        when(notificationEmailHelper.getCPPUID()).thenReturn("87yy98798uo");

        Response response = Response.ok().build();
        when(notificationEmailHelper.initializeClient()).thenReturn(client);
        when(notificationEmailHelper.getFormCheckerTemplateId()).thenReturn(UUID.randomUUID().toString());

        when(client.target(Mockito.anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.headers(any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        String notificationId = UUID.randomUUID().toString();

        assertThrows(BulkScanConfigurationMissingException.class,
                () -> notificationEmailHelper.sendFormCheckerResultEmail("validationResponse", notificationId, "test@test.com", "Subject", logger),
                "Environment variable not found: notification_email_api_url");

        verify(logger).severe("Environment variable not found: notification_email_api_url");
    }

    @Test
    public void givenTemplateIsEmpty_sendNotificationEmail_shouldThrowsException() {

        when(notificationEmailHelper.getNotificationAPIUrl()).thenReturn("http://notificationnotify.com");
        when(notificationEmailHelper.getCPPUID()).thenReturn("87yy98798uo");

        Response response = Response.ok().build();
        when(notificationEmailHelper.initializeClient()).thenReturn(client);
        when(notificationEmailHelper.getNotificationAPIUrl()).thenReturn("http://testnotify.com");

        when(client.target(Mockito.anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.headers(any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        String notificationId = UUID.randomUUID().toString();

        assertThrows( BulkScanConfigurationMissingException.class,
                () -> notificationEmailHelper.sendFormCheckerResultEmail("validationResponse", notificationId, "test@test.com", "Subject", logger),
                "Environment variable not found: form_checker_email_template_id");

        verify(logger).severe("Environment variable not found: form_checker_email_template_id");

    }
    @Test
    public void givenCppUserIdIsEmpty_sendNotificationEmail_shouldThrowsException() {

        when(notificationEmailHelper.getNotificationAPIUrl()).thenReturn("http://notificationnotify.com");
        when(notificationEmailHelper.getFormCheckerTemplateId()).thenReturn("5465466 546 465");

        Response response = Response.ok().build();
        when(notificationEmailHelper.initializeClient()).thenReturn(client);
        when(notificationEmailHelper.getNotificationAPIUrl()).thenReturn("http://testnotify.com");

        when(client.target(Mockito.anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.headers(any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        String notificationId = UUID.randomUUID().toString();

        assertThrows( BulkScanConfigurationMissingException.class,
                () -> notificationEmailHelper.sendFormCheckerResultEmail("validationResponse", notificationId, "test@test.com", "Subject", logger),
                "Environment variable not found: staging-prosecutor-user");

        verify(logger).severe("Environment variable not found: staging-prosecutor-user");

    }

}