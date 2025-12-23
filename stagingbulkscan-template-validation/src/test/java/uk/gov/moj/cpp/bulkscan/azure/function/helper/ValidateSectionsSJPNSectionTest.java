package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.English;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.Welsh;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidateSectionsSJPNSectionTest {

    @Test
    @DisplayName("Should validate English SJPN section successfully")
    void shouldValidateEnglishSjpnSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateSJPNSection(English, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("First page is on odd sequence")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("URN/Case no' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Single Justice Procedure Notice' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("SJP.NOTI' found")));
    }

    @Test
    @DisplayName("Should validate Welsh SJPN section successfully")
    void shouldValidateWelshSjpnSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateSJPNSection(Welsh, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Cyfeirnod Unigryw/Rhif yr Achos' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Hysbysiad Gweithdrefn Un Ynad' found")));
    }

    @Test
    @DisplayName("Should add error when no SJPN pages found")
    void shouldAddErrorWhenNoSjpnPagesFound() throws Exception {
        // Given
        List<PageContent> emptyPageContents = Collections.emptyList();
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateSJPNSection(English, emptyPageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("English SJPN Section: Missing SJPN start page")));
    }

    @Test
    @DisplayName("Should add error when not enough pages for SJPN section")
    void shouldAddErrorWhenNotEnoughPagesForSjpnSection() throws Exception {
        // Given
        List<PageContent> pageContents = createValidEnglishCoverPageContents(1);
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateSJPNSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("English SJPN Section: Missing SJPN start page")));
    }

    @Test
    @DisplayName("Should add error when SJPN first page is on even sequence")
    void shouldAddErrorWhenSjpnFirstPageIsOnEvenSequence() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(4));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateSJPNSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("First page is not on odd sequence")));
    }

    private void invokeValidateSJPNSection(TemplateValidationHelper.Language language, List<PageContent> pageContents, ValidationResult result) throws Exception {
        Method method = TemplateValidationHelper.class.getDeclaredMethod("validateSJPNSection", TemplateValidationHelper.Language.class, List.class, ValidationResult.class);
        method.setAccessible(true);
        method.invoke(null, language, pageContents, result);
    }

}
