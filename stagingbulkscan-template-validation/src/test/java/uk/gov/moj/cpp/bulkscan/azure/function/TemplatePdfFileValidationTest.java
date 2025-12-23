package uk.gov.moj.cpp.bulkscan.azure.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.DocumentType.ENGLISH;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.validateDocument;

import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplatePdfFileValidationTest {

    @Test
    @DisplayName("Should validate valid English pdf file successfully")
    void shouldValidateValidEnglishPdfFileSuccessfully() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/valid-document.pdf"));

        // Then
        assertNotNull(result);
        assertNotNull(result.getDocumentType());
        assertTrue(result.isValid(), "Document should be valid");
    }

    @Test
    @DisplayName("Should not validate missing company finances and sjp mc100")
    void shouldNotValidateMissingCompanyFinancesAndSJPMC100() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/company-finances-and-sjp-mc100-missing.pdf"));

        // Then
        assertNotNull(result);
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'11. Company finances' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.MC100' missing")));
    }

    @Test
    @DisplayName("Should not validate missing page on plea")
    void shouldNotValidateMissingPageOnPlea() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/missing-page-on-plea.pdf"));

        // Then
        assertNotNull(result);
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(6, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'4. Guilty plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'5. Not guilty plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'6. Your court hearing' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'7. Your declaration' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.PLEA' missing")));
    }

    @Test
    @DisplayName("Should not validate SJPN not on odd page")
    void shouldNotValidateSJPNNotOnOddPage() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/sjpn-not-on-odd-page.pdf"));

        // Then
        assertNotNull(result);
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("English SJPN Section: First page is not on odd sequence")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("English Plea Section: First page is not on odd sequence")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("English MC100 Section: First page is not on odd sequence")));
    }

    @Test
    @DisplayName("Should not validate URN Case no missing on SJPN")
    void shouldNotValidateURNCaseNoMissingOnSJPN() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/urn-case-no-missing-on-sjpn.pdf"));

        // Then
        assertNotNull(result);
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("SJPN Section: page 1 - 'URN/Case no' missing")));
    }

    @Test
    @DisplayName("Should not validate missing Your plea and sjp plea")
    void shouldNotValidateMissingYourPleaAndSJPPLEA() throws Exception {
        // When
        ValidationResult result = validateDocument(getPdfContent("src/test/resources/test-documents/your-plea-and-sjp-plea-missing.pdf"));

        // Then
        assertNotNull(result);
        assertEquals(ENGLISH.name(), result.getDocumentType());
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'3. Your plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.PLEA' missing")));
    }


    private static byte[] getPdfContent(final String first) throws IOException {
        Path pdfFilePath = Paths.get(first);
        if (!Files.exists(pdfFilePath)) {
            throw new IOException("Test PDF file not found at: " + pdfFilePath.toAbsolutePath());
        }
        byte[] pdfContent = Files.readAllBytes(pdfFilePath);
        return pdfContent;
    }
}