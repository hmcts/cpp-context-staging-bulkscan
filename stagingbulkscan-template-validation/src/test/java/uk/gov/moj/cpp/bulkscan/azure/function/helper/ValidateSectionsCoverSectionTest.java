package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.ENGLISH_COVER_FOOTER;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.English;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.Welsh;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.WELSH_COVER_FOOTER;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidateSectionsCoverSectionTest {

    @Test
    @DisplayName("Should validate English cover section successfully")
    void shouldValidateEnglishCoverSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = createValidEnglishCoverPageContents(1);
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("First page is on odd sequence")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("URN/Case no' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Posting Date' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("SJP.LETT' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("SJP.LETT' found")));
    }

    @Test
    @DisplayName("Should validate Welsh cover section successfully")
    void shouldValidateWelshCoverSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = createValidWelshCoverPageContents(1);
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(TemplateValidationHelper.Language.Welsh, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("First page is on odd sequence")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Cyfeirnod Unigryw/Rhif yr Achos' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Dyddiad Postio' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("SJP.LETT' found")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("SJP.LETT' found")));
    }

    @Test
    @DisplayName("Should add error when first page is on even sequence")
    void shouldAddErrorWhenFirstPageIsOnEvenSequence() throws Exception {
        // Given
        List<PageContent> pageContents = createValidEnglishCoverPageContents(2);
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("First page is not on odd sequence")));
    }

    @Test
    @DisplayName("Should add error when missing URN in English cover section")
    void shouldAddErrorWhenMissingUrnInEnglishCoverSection() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "Posting Date some content " + ENGLISH_COVER_FOOTER),
                new PageContent(2, "Second page content " + ENGLISH_COVER_FOOTER)
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'URN/Case no' missing")));
    }

    @Test
    @DisplayName("Should add error when missing URN in Welsh cover section")
    void shouldAddErrorWhenMissingUrnInWelshCoverSection() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "Dyddiad Postio: 09/07/2025 " + WELSH_COVER_FOOTER),
                new PageContent(2, "Cyfeirnod Unigryw/Rhif yr Achos:  874133 Annwyl Samuel Brown " + WELSH_COVER_FOOTER)
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Cyfeirnod Unigryw/Rhif yr Achos'")));
    }

    @Test
    @DisplayName("Should add error when missing posting date in English cover section")
    void shouldAddErrorWhenMissingPostingDateInEnglishCoverSection() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "URN/Case no: ABC123 First cover page content " + ENGLISH_COVER_FOOTER),
                new PageContent(2, "URN/Case no: ABC123 Second cover page content " + ENGLISH_COVER_FOOTER)
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Posting Date' missing")));
    }

    @Test
    @DisplayName("Should add error when missing posting date in Welsh cover section")
    void shouldAddErrorWhenMissingPostingDateInWelshCoverSection() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "Cyfeirnod Unigryw/Rhif yr Achos:  874133 uhfdofuhfdoif " + WELSH_COVER_FOOTER),
                new PageContent(2, "Cyfeirnod Unigryw/Rhif yr Achos:  874133 Annwyl Samuel Brown " + WELSH_COVER_FOOTER)
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Dyddiad Postio' missing")));
    }

    @Test
    @DisplayName("Should add error when missing SJP.LETT in first page footer")
    void shouldAddErrorWhenMissingSjpLettInFirstPageFooter() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "URN/Case no: ABC123 Posting Date some content"),
                new PageContent(2, "Second page content " + ENGLISH_COVER_FOOTER)
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.LETT' missing")));
    }

    @Test
    @DisplayName("Should add error when missing SJP.LETT in second page footer")
    void shouldAddErrorWhenMissingSjpLettInSecondPageFooter() throws Exception {
        // Given
        List<PageContent> pageContents = Arrays.asList(
                new PageContent(1, "URN/Case no: ABC123 Posting Date some content " + ENGLISH_COVER_FOOTER),
                new PageContent(2, "URN/Case no: ABC123 Second page content")
        );
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateCoverSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.LETT' missing")));
    }

    private void invokeValidateCoverSection(TemplateValidationHelper.Language language, List<PageContent> pageContents, ValidationResult result) throws Exception {
        Method method = TemplateValidationHelper.class.getDeclaredMethod("validateCoverSection", TemplateValidationHelper.Language.class, List.class, ValidationResult.class);
        method.setAccessible(true);
        method.invoke(null, language, pageContents, result);
    }
}