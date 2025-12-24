package uk.gov.moj.cpp.bulkscan.azure.function.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

public class TemplateValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValidationService.class);

    public String validateTemplate(final Optional<byte[]> request) throws IOException {

        final byte[] documentContent = request.orElse(null);

        if (documentContent == null) {
            LOGGER.info("Received document as null");
        } else {
            LOGGER.info("Received document with size: {} bytes", documentContent.length);
        }

        if (documentContent == null || documentContent.length == 0) {
            return "Please send document content in the request body.";
        }

        final ValidationResult validationResult = TemplateValidationHelper.validateDocument(documentContent);

        if (validationResult.isValid()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("Document validated successfully:\n");
            builder.append("Document type: ").append(validationResult.getDocumentType()).append("\n");
            validationResult.getDetails().forEach(detail -> builder.append("  * ").append(detail).append("\n"));
            return builder.toString();
        } else {
            final StringBuilder builder = new StringBuilder();
            builder.append("Document validation failed with errors:\n");
            builder.append("Document type: ").append(validationResult.getDocumentType()).append("\n");
            validationResult.getErrors().forEach(error -> builder.append("- ").append(error).append("\n"));
            validationResult.getDetails().forEach(detail -> builder.append("  * ").append(detail).append("\n"));
            return builder.toString();
        }
    }
}
