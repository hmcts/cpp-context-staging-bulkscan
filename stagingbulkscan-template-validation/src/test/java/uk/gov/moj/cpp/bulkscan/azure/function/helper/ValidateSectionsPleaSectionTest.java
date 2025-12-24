package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishPleaContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshMC100Contents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshPleaContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.English;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.Welsh;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_ENG;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_WEL;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.SJP_PLEA;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidateSectionsPleaSectionTest {

    @Test
    @DisplayName("Should validate English plea section successfully")
    void shouldValidateEnglishPleaFormSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(createValidEnglishPleaContents(5));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(English, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Plea Section: First page is on odd sequence")));
    }

    @Test
    @DisplayName("Should validate Welsh plea section successfully")
    void shouldValidateWelshPleaFormSectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(createValidWelshPleaContents(5));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(Welsh, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("Plea Section: First page is on odd sequence")));
    }

    @Test
    @DisplayName("Should add error when plea start page not found")
    void shouldAddErrorWhenPleaFormStartPageNotFound() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(createValidWelshMC100Contents(5));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Missing Plea Form start page")));
    }

    @Test
    @DisplayName("Should add error when English plea first page is on even sequence")
    void shouldAddErrorWhenEnglishPleaFormFirstPageIsOnEvenSequence() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(createValidEnglishPleaContents(6));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("First page is not on odd sequence")));
    }

    @Test
    @DisplayName("Should add error when Welsh plea first page is on even sequence")
    void shouldAddErrorWhenWelshPleaFormFirstPageIsOnEvenSequence() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(createValidWelshPleaContents(6));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("First page is not on odd sequence")));
    }


    @Test
    @DisplayName("Should add errors when missing expected text in English plea")
    void shouldAddErrorsWhenMissingExpectedTextInEnglishPleaForm() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(Arrays.asList(
                new PageContent(5, "URN/Case no: ABC123\n" +
                        "Missing information on Plea Form page 1" + "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(6, "URN/Case no: ABC123\n" +
                        "Missing information on Plea Form page 2" + "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(7, "URN/Case no: ABC123\n" +
                        "Missing information on Plea Form page 3" + "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(8, "URN/Case no: ABC123\n" +
                        "Missing information on Plea Form page 4"),
                new PageContent(9, "Missing information on Plea Form page 5" + "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(10, "URN/Case no: ABC123\n" +
                        "Missing information on Plea Form page 6" + "GALMT00." + SJP_PLEA + "." + MULT_ENG)
        ));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(10, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Make your plea by post' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'1. Your details' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'2. Additional details' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'3. Your plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'4. Guilty plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'5. Not guilty plea' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.PLEA' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'6. Your court hearing' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'URN/Case no' missing")));
    }


    @Test
    @DisplayName("Should add errors when missing expected text in Welsh plea")
    void shouldAddErrorsWhenMissingExpectedTextInWelshPleaForm() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(Arrays.asList(
                new PageContent(5, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 1" + "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(6, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 2" + "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(7, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 3" + "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(8, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 4"),
                new PageContent(9, "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 5" + "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(10, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen Bledio 6" + "GALMT00." + SJP_PLEA + "." + MULT_WEL)
        ));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidatePleaSection(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(10, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Cofnodi eich ple drwy’r post' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'1. Eich manylion' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'2. Manylion Ychwanegol' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'3. Eich ple' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'4. Pledio’n euog' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'5. Pledio’n ddieuog' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.PLEA' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'6. Eich gwrandawiad llys' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Cyfeirnod Unigryw/Rhif yr Achos' missing")));
    }

    private void invokeValidatePleaSection(TemplateValidationHelper.Language language, List<PageContent> pageContents, ValidationResult result) throws Exception {
        Method method = TemplateValidationHelper.class.getDeclaredMethod("validatePleaSection", TemplateValidationHelper.Language.class, List.class, ValidationResult.class);
        method.setAccessible(true);
        method.invoke(null, language, pageContents, result);
    }
}