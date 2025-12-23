package uk.gov.moj.cpp.bulkscan.azure.function;

import uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateValidationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValidationProcessor.class);

    @FunctionName("templateValidationProcessor")
    public HttpResponseMessage validateTemplate(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION, dataType = "binary")
            HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) throws IOException {

        final byte[] documentContent = request.getBody().orElse(null);

        LOGGER.info("Received document " + (documentContent == null ? "as null" : "with size: " + documentContent.length + " bytes"));

        if (documentContent == null || documentContent.length == 0) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please send document content in the request body.")
                    .build();
        }

        final ValidationResult validationResult = TemplateValidationHelper.validateDocument(documentContent);

        if (validationResult.isValid()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("Document validated successfully:\n");
            builder.append("Document type: ").append(validationResult.getDocumentType()).append("\n");
            validationResult.getDetails().forEach(detail -> builder.append("  * ").append(detail).append("\n"));
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(builder.toString())
                    .build();
        } else {
            final StringBuilder builder = new StringBuilder();
            builder.append("Document validation failed with errors:\n");
            builder.append("Document type: ").append(validationResult.getDocumentType()).append("\n");
            validationResult.getErrors().forEach(error -> builder.append("- ").append(error).append("\n"));
            validationResult.getDetails().forEach(detail -> builder.append("  * ").append(detail).append("\n"));
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(builder.toString())
                    .build();
        }
    }
}
