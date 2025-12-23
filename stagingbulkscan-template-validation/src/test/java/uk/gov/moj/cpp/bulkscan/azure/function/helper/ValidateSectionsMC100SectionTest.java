package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishMC100Contents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishPleaContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidEnglishSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidMultilingualPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshCoverPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshPleaContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationData.createValidWelshSJPNPageContents;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.English;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.Language.Welsh;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_ENG;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_WEL;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.SJP_MC100;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidateSectionsMC100SectionTest {

    @Test
    @DisplayName("Should validate English MC100 form section successfully")
    void shouldValidateEnglishMC100SectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = createValidEnglishPageContents(1);
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(English, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("MC100 Section: First page is on odd sequence")));
    }

    @Test
    @DisplayName("Should validate Welsh MC100 form section successfully")
    void shouldValidateWelshMC100SectionSuccessfully() throws Exception {
        // Given
        List<PageContent> pageContents = createValidMultilingualPageContents();
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(Welsh, pageContents, result);

        // Then
        assertEquals(true, result.isValid());
        assertEquals(true, result.getErrors().isEmpty());
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("MC100 Section: First page is on odd sequence")));
    }

    @Test
    @DisplayName("Should add error when MC100 form start page not found")
    void shouldAddErrorWhenMC100StartPageNotFound() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(createValidEnglishPleaContents(5));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Missing MC100 Form start page")));
    }

    @Test
    @DisplayName("Should add error when MC100 form start page not found for Welsh")
    void shouldAddErrorWhenMC100StartPageNotFoundForWelsh() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(createValidWelshPleaContents(5));
        pageContents.addAll(createValidEnglishCoverPageContents(11));
        pageContents.addAll(createValidEnglishSJPNPageContents(13));
        pageContents.addAll(createValidEnglishPleaContents(19));
        pageContents.addAll(createValidEnglishMC100Contents(25));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Missing MC100 Form start page")));
    }

    @Test
    @DisplayName("Should add error when MC100 form first page is on even sequence")
    void shouldAddErrorWhenMC100FirstPageIsOnEvenSequence() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(createValidEnglishPleaContents(5));
        pageContents.addAll(createValidEnglishMC100Contents(12));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("MC100 Section: First page is not on odd sequence")));
    }

    @Test
    @DisplayName("Should add errors when missing expected text in English MC100")
    void shouldAddErrorsWhenMissingExpectedTextInEnglishMC100() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidEnglishCoverPageContents(1));
        pageContents.addAll(createValidEnglishSJPNPageContents(3));
        pageContents.addAll(createValidEnglishPleaContents(9));
        pageContents.addAll(Arrays.asList(
                new PageContent(15, "URN/Case no: ABC123\n" +
                        "Missing information on MC100 Form page 1" + "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(16, "URN/Case no: ABC123\n" +
                        "Missing information on MC100 Form page 2" + "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(17, "URN/Case no: ABC123\n" +
                        "Missing information on MC100 Form page 3" + "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(18, "URN/Case no: ABC123\n" +
                        "Missing information on MC100 Form page 4"),
                new PageContent(19, "Missing information on MC100 Form page 5" + "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(20, "URN/Case no: ABC123\n" +
                        "Missing information on MC100 Form page 6" + "GALMT00." + SJP_MC100 + "." + MULT_ENG)
        ));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(English, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(8, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Your finances' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'8. Your income' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'9. Deductions from your earnings or benefits' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'10. Your monthly outgoings and assets' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'11. Company finances' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Post your completed form to' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.MC100' missing")));
    }


    @Test
    @DisplayName("Should add errors when missing expected text in Welsh MC100")
    void shouldAddErrorsWhenMissingExpectedTextInWelshPleaForm() throws Exception {
        // Given
        List<PageContent> pageContents = new ArrayList<>();
        pageContents.addAll(createValidWelshCoverPageContents(1));
        pageContents.addAll(createValidWelshSJPNPageContents(3));
        pageContents.addAll(createValidWelshPleaContents(9));
        pageContents.addAll(Arrays.asList(
                new PageContent(15, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 1" + "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(16, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 2" + "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(17, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 3" + "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(18, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 4"),
                new PageContent(19, "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 5" + "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(20, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Gwybodaeth ar goll ar dudalen y Ffurflen MC100 6" + "GALMT00." + SJP_MC100 + "." + MULT_WEL)
        ));
        ValidationResult result = new ValidationResult();

        // When
        invokeValidateMC100Section(Welsh, pageContents, result);

        // Then
        assertEquals(false, result.isValid());
        assertEquals(8, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'Eich amgylchiadau ariannol' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'8. Eich incwm' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'9. Didyniadau o’ch enillion neu’ch budd-daliadau' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'10. Eich holl wariant misol a’ch asedau' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'11. Sefyllfa ariannol y cwmni' missing")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("'SJP.MC100' missing")));
    }

    private void invokeValidateMC100Section(TemplateValidationHelper.Language language, List<PageContent> pageContents, ValidationResult result) throws Exception {
        Method method = TemplateValidationHelper.class.getDeclaredMethod("validateMC100Section", TemplateValidationHelper.Language.class, List.class, ValidationResult.class);
        method.setAccessible(true);
        method.invoke(null, language, pageContents, result);
    }
}
