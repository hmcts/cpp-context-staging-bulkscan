package uk.gov.moj.cpp.bulkscan.azure.function;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.bulkscan.azure.function.helper.NotificationEmailHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.model.DecryptedNppForm;
import uk.gov.moj.cpp.bulkscan.azure.function.service.Aes256CbcService;
import uk.gov.moj.cpp.bulkscan.azure.function.service.TemplateValidationService;
import uk.gov.moj.cpp.bulkscan.azure.function.model.EncryptedAttachment;
import uk.gov.moj.cpp.bulkscan.azure.function.model.EncryptedNppForm;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NppFormCheckerFunctionTest {

    @Mock
    private HttpRequestMessage<EncryptedNppForm> request;

    @Mock
    private Logger logger;

    @Mock
    private ExecutionContext context;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage httpResponseMessage;

    @Mock
    private Aes256CbcService aes256CbcService;

    @Mock
    private TemplateValidationService templateValidationService;

    @Mock
    private NotificationEmailHelper notificationEmailHelper;

    private NppFormCheckerFunction functionToTest;

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        functionToTest = spy(new NppFormCheckerFunction());
        when(context.getLogger()).thenReturn(logger);
        when(responseBuilder.header("Content-Type", "application/json")).thenReturn(responseBuilder);
        when(responseBuilder.body(anyString())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(httpResponseMessage);
    }

    @Test
    public void shouldValidateNppForm() throws Exception {
        final JsonObject payload = getFileContentAsJson("stagingbulkscan.adapt-npp-checker.json", ImmutableMap.<String, Object>builder()
                .build());
        final EncryptedNppForm encryptedNppForm = jsonObjectToObjectConverter.convert(payload, EncryptedNppForm.class);
        final byte[] encryptedFile = new byte[]{};

        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        when(request.getBody()).thenReturn(encryptedNppForm);
        when(aes256CbcService.downloadAndDecryptAes256Cbc("url", "YmB1H9DbauIBTvznD1CPa3quNpkFhtsrTN+NOzlprBg=",
                "hT2fgmfrS9Si9LdGGZjUFw==", true, true)).thenReturn(encryptedFile);
        when(templateValidationService.validateTemplate(Optional.of(encryptedFile))).thenReturn("Document validated successfully");
        when(functionToTest.getNppSharedKeyJson()).thenReturn("{ \"kty\":\"oct\", \"k\":\"CxhKdPjF9jqlPdQB_9lAmQ\" }");
        when(notificationEmailHelper.sendFormCheckerResultEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.ok().build());

        functionToTest.setAes256CbcService(aes256CbcService);
        functionToTest.setTemplateValidationService(templateValidationService);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);

        final HttpResponseMessage httpResponseMessage = functionToTest.checkNppForm(request, context);
        assertEquals(httpResponseMessage.getStatus(), HttpStatus.OK);
        verify(notificationEmailHelper, times(1)).sendFormCheckerResultEmail(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void shouldEncryptThePayload() throws Exception {
        final JsonObject payload = getFileContentAsJson("stagingbulkscan.adapt-npp-checker.json", ImmutableMap.<String, Object>builder()
                .build());
        final EncryptedNppForm encryptedNppForm = jsonObjectToObjectConverter.convert(payload, EncryptedNppForm.class);

        final DecryptedNppForm decryptedNppForm = functionToTest.getDecryptedNppForm(encryptedNppForm.getToken(),
                "{ \"kty\":\"oct\", \"k\":\"CxhKdPjF9jqlPdQB_9lAmQ\" }");
        final EncryptedAttachment encryptedAttachment = decryptedNppForm.getAttachments().get(0);
        assertEquals(encryptedAttachment.getUrl(), "url");
        assertEquals(encryptedAttachment.getEncryption_key(), "YmB1H9DbauIBTvznD1CPa3quNpkFhtsrTN+NOzlprBg=");
        assertEquals(encryptedAttachment.getEncryption_iv(), "hT2fgmfrS9Si9LdGGZjUFw==");
        assertEquals(encryptedAttachment.getMimetype(), "application/pdf");
        assertEquals(encryptedAttachment.getFilename(), "sample.pdf");
    }

    private JsonObject getFileContentAsJson(final String path, final Map<String, Object> namedPlaceholders) {
        return stringToJsonObjectConverter.convert(getFileContent(path, namedPlaceholders));
    }

    private static String getFileContent(final String path, final Map<String, Object> namedPlaceholders) {
        return new StrSubstitutor(namedPlaceholders).replace(getPayload(path));
    }

    private static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
