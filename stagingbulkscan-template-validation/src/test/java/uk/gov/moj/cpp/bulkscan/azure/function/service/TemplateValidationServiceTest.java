package uk.gov.moj.cpp.bulkscan.azure.function.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("unchecked")
class TemplateValidationServiceTest {

    private TemplateValidationService templateValidationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        templateValidationService = new TemplateValidationService();
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when request body is empty")
    void testValidateTemplateWithEmptyBody() throws IOException {
       final String result = templateValidationService.validateTemplate(Optional.empty());

       assertEquals(result, "Please send document content in the request body.");
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when request body is null")
    void testValidateTemplateWithNullBody() throws IOException {
       final String result = templateValidationService.validateTemplate(Optional.ofNullable(null));

        assertEquals(result, "Please send document content in the request body.");
    }

    @Test
    @DisplayName("Should return OK with success message when document is valid")
    void testValidateTemplateWithValidDocument() throws IOException {
        byte[] documentContent = "valid document content".getBytes();

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");
        validResult.addDetail("Document validated successfully");
        validResult.addDetail("All sections found");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            final String result  = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document validated successfully") &&
                    result.contains("Document type: ENGLISH") &&
                    result.contains("Document validated successfully") &&
                    result.contains("All sections found"));
        }
    }

    @Test
    @DisplayName("Should return BAD_REQUEST with error messages when document is invalid")
    void testValidateTemplateWithInvalidDocument() throws IOException {
        byte[] documentContent = "invalid document content".getBytes();

        ValidationResult invalidResult = new ValidationResult();
        invalidResult.setDocumentType("INVALID");
        invalidResult.addError("Missing required header");
        invalidResult.addError("Invalid footer format");
        invalidResult.addDetail("Total pages: 5");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(invalidResult);

            final String result = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document validation failed with errors") &&
                    result.contains("Document type: INVALID") &&
                    result.contains("Missing required header") &&
                    result.contains("Invalid footer format") &&
                    result.contains("Total pages: 5"));
        }
    }

    @Test
    @DisplayName("Should handle IOException from validation helper")
    void testValidateTemplateWithIOException(){
        byte[] documentContent = "problematic document content".getBytes();

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenThrow(new IOException("PDF parsing error"));

            assertThrows(IOException.class, () -> {
                templateValidationService.validateTemplate(Optional.of(documentContent));
            });
        }
    }

    @Test
    @DisplayName("Should handle large document content")
    void testValidateTemplateWithLargeDocument() throws IOException {
        byte[] largeDocumentContent = new byte[1024 * 1024];
        Arrays.fill(largeDocumentContent, (byte) 'A');

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("MULTILINGUAL");
        validResult.addDetail("Large document processed successfully");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(largeDocumentContent))
                    .thenReturn(validResult);

            final String result = templateValidationService.validateTemplate(Optional.of(largeDocumentContent));

            assertTrue(result.contains("Document type: MULTILINGUAL"));
        }
    }

    @Test
    @DisplayName("Should format success response correctly with multiple details")
    void testSuccessResponseFormatting() throws IOException {
        byte[] documentContent = "test document".getBytes();

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");
        validResult.addDetail("Detail 1: Page count verified");
        validResult.addDetail("Detail 2: Headers validated");
        validResult.addDetail("Detail 3: Footers validated");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            final String result = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document validated successfully:\n") &&
                    result.contains("Document type: ENGLISH\n") &&
                    result.contains("  * Detail 1: Page count verified\n") &&
                    result.contains("  * Detail 2: Headers validated\n") &&
                    result.contains("  * Detail 3: Footers validated\n"));
        }
    }

    @Test
    @DisplayName("Should format error response correctly with multiple errors and details")
    void testErrorResponseFormatting() throws IOException {
        byte[] documentContent = "test document".getBytes();

        ValidationResult invalidResult = new ValidationResult();
        invalidResult.setDocumentType("INVALID");
        invalidResult.addError("Error 1: Missing header");
        invalidResult.addError("Error 2: Invalid footer");
        invalidResult.addDetail("Detail 1: Total pages found: 3");
        invalidResult.addDetail("Detail 2: Document size: 1024 bytes");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(invalidResult);

            final String result = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document validation failed with errors:\n") &&
                    result.contains("Document type: INVALID\n") &&
                    result.contains("- Error 1: Missing header\n") &&
                    result.contains("- Error 2: Invalid footer\n") &&
                    result.contains("  * Detail 1: Total pages found: 3\n") &&
                    result.contains("  * Detail 2: Document size: 1024 bytes\n"));
        }
    }

    @Test
    @DisplayName("Should handle validation result with null document type")
    void testValidationWithNullDocumentType() throws IOException {
        byte[] documentContent = "test document".getBytes();

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType(null);
        validResult.addDetail("Processed successfully");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            final String result = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document type: null"));
        }
    }

    @Test
    @DisplayName("Should handle validation result with empty lists")
    void testValidationWithEmptyLists() throws IOException {
        byte[] documentContent = "test document".getBytes();

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            final String result = templateValidationService.validateTemplate(Optional.of(documentContent));

            assertTrue(result.contains("Document validated successfully:\n") &&
                    result.contains("Document type: ENGLISH\n") &&
                    !result.contains("  * "));
        }
    }
}