package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createEmptyPdfDocument;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createInvalidPdfDocument;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishPdfDocument;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidMultilingualPdfDocument;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.DocumentType.ENGLISH;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.DocumentType.INVALID;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.DocumentType.MULTILINGUAL;

import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateValidationHelperTest {

    @Test
    @DisplayName("Should validate valid English document successfully")
    void shouldValidateEnglishDocumentSuccessfully() throws Exception {
        // Given
        byte[] pdfContent = createValidEnglishPdfDocument();

        // When
        ValidationResult result = TemplateValidationHelper.validateDocument(pdfContent);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertTrue(result.getDetails().size() > 0);
    }

    @Test
    @DisplayName("Should validate valid multilingual document successfully")
    void shouldValidateMultilingualDocumentSuccessfully() throws Exception {
        // Given
        byte[] pdfContent = createValidMultilingualPdfDocument();

        // When
        ValidationResult result = TemplateValidationHelper.validateDocument(pdfContent);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(MULTILINGUAL.name(), result.getDocumentType());
        assertTrue(result.getDetails().size() > 0);
    }

    @Test
    @DisplayName("Should return invalid when first page footer is invalid")
    void shouldReturnInvalidWhenFirstPageFooterIsInvalid() throws Exception {
        // Given
        byte[] pdfContent = createInvalidPdfDocument();

        // When
        ValidationResult result = TemplateValidationHelper.validateDocument(pdfContent);

        // Then
        assertNotNull(result);
        assertEquals(INVALID.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid document: First page footer must contain"));
    }

    @Test
    @DisplayName("Should return invalid when document empty")
    void shouldHandleEmptyDocument() throws Exception {
        // Given
        byte[] pdfContent = createEmptyPdfDocument();

        // When
        ValidationResult result = TemplateValidationHelper.validateDocument(pdfContent);

        // Then
        assertNotNull(result);
        assertEquals(INVALID.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Invalid document: Document does not have enough pages"));
    }

    @Test
    @DisplayName("Should handle IOException")
    void shouldHandleIOException() {
        // Given
        byte[] invalidPdfContent = "invalid pdf content".getBytes();

        // When & Then
        assertThrows(IOException.class, () -> {
            TemplateValidationHelper.validateDocument(invalidPdfContent);
        });
    }
}