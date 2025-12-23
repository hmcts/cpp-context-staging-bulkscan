package uk.gov.moj.cpp.bulkscan.azure.function;

import static java.lang.System.getenv;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.bulkscan.azure.function.helper.NotificationEmailHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.service.Aes256CbcService;
import uk.gov.moj.cpp.bulkscan.azure.function.service.TemplateValidationService;
import uk.gov.moj.cpp.bulkscan.azure.function.model.DecryptedNppForm;
import uk.gov.moj.cpp.bulkscan.azure.function.model.EncryptedAttachment;
import uk.gov.moj.cpp.bulkscan.azure.function.model.EncryptedNppForm;

import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jwt.EncryptedJWT;

public class NppFormCheckerFunction {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private Aes256CbcService aes256CbcService = new Aes256CbcService();

    private TemplateValidationService templateValidationService = new TemplateValidationService();

    private NotificationEmailHelper notificationEmailHelper = new NotificationEmailHelper();

    @FunctionName("nppFormChecker")
    public HttpResponseMessage checkNppForm(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<EncryptedNppForm> request,
            final ExecutionContext context) {

        context.getLogger().info("NppFormCheckerFunction started");

        try {
            // Parse input JSON
            final EncryptedNppForm nppForm = request.getBody();
            final String jweToken = nppForm.getToken();

            if (jweToken == null || jweToken.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Missing 'token' field in request body.")
                        .build();
            }

            final String sharedKeyJson = getNppSharedKeyJson();

            // Parse the shared key from environment
            if (sharedKeyJson == null || sharedKeyJson.isEmpty()) {
                context.getLogger().severe("The key is not found.");

                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Server configuration error: missing JWE_SHARED_KEY_JSON env var.")
                        .build();
            }

            context.getLogger().info("The request content will be decrypted.");

            final DecryptedNppForm decryptedNppForm = getDecryptedNppForm(jweToken, sharedKeyJson);

            final EncryptedAttachment encryptedAttachment = decryptedNppForm.getAttachments().get(0);

            context.getLogger().info("downloadAndDecryptAes256Cbc will be called for url  " + encryptedAttachment.getUrl());

            final byte[] decrypted = aes256CbcService.downloadAndDecryptAes256Cbc(encryptedAttachment.getUrl(), encryptedAttachment.getEncryption_key(),
                    encryptedAttachment.getEncryption_iv(), true, true);

            context.getLogger().info("validateTemplate will be called for url  " + encryptedAttachment.getUrl());

            final String nppValidationResponse = templateValidationService.validateTemplate(Optional.of(decrypted));

            notificationEmailHelper.sendFormCheckerResultEmail(nppValidationResponse,UUID.randomUUID().toString(),
                    decryptedNppForm.getSubmissionAnswers().getEmail_address(),
                    "Form Checker", context.getLogger());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(nppValidationResponse)
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error decrypting JWE: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid or corrupt JWE token.")
                    .build();
        }
    }


    protected DecryptedNppForm getDecryptedNppForm(final String jweToken, final String sharedKeyJson) throws ParseException, JOSEException {
        final OctetSequenceKey sharedKey = OctetSequenceKey.parse(sharedKeyJson);

        // Parse and decrypt the JWE
        EncryptedJWT encryptedJWT = EncryptedJWT.parse(jweToken);
        DirectDecrypter decrypter = new DirectDecrypter(sharedKey);
        encryptedJWT.decrypt(decrypter);
        // Return decrypted payload

        final JsonObject jsonObject = stringToJsonObjectConverter.convert(encryptedJWT.getPayload().toString());

        return jsonObjectToObjectConverter.convert(jsonObject, DecryptedNppForm.class);
    }

    protected void setAes256CbcService(final Aes256CbcService aes256CbcService){
        this.aes256CbcService = aes256CbcService;
    }

    protected void setTemplateValidationService(final TemplateValidationService templateValidationService){
        this.templateValidationService = templateValidationService;
    }

    protected String getNppSharedKeyJson() {
        return getenv("npp_shared_key_json");
    }

    protected void setNotificationEmailHelper(final NotificationEmailHelper notificationEmailHelper) {
        this.notificationEmailHelper = notificationEmailHelper;
    }
}
