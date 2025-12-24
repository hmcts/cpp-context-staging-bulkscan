package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5976")
public class TemplateValidationHelperUtilityMethodsTest {

    @Nested
    @DisplayName("determineDocumentType tests")
    class DetermineDocumentTypeTest {

        @Test
        @DisplayName("Should return ENGLISH for English cover footer")
        void testEnglishDocumentType() throws Exception {
            Method method = TemplateValidationHelper.class.getDeclaredMethod(
                    "determineDocumentType",
                    Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent"));
            method.setAccessible(true);

            var pageContentClass = Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent");
            var pageContent = pageContentClass.getConstructor(int.class, String.class)
                    .newInstance(1, "header body SJP.LETT.MULT.ENG");

            var result = method.invoke(null, pageContent);
            assertEquals("ENGLISH", result.toString());
        }

        @Test
        @DisplayName("Should return MULTILINGUAL for Welsh cover footer")
        void testMultilingualDocumentType() throws Exception {
            Method method = TemplateValidationHelper.class.getDeclaredMethod(
                    "determineDocumentType",
                    Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent"));
            method.setAccessible(true);

            var pageContentClass = Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent");
            var pageContent = pageContentClass.getConstructor(int.class, String.class)
                    .newInstance(1, "header body SJP.LETT.MULT.WEL");

            var result = method.invoke(null, pageContent);
            assertEquals("MULTILINGUAL", result.toString());
        }

        @Test
        @DisplayName("Should return INVALID for unknown footer")
        void testInvalidDocumentType() throws Exception {
            Method method = TemplateValidationHelper.class.getDeclaredMethod(
                    "determineDocumentType",
                    Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent"));
            method.setAccessible(true);

            var pageContentClass = Class.forName("uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent");
            var pageContent = pageContentClass.getConstructor(int.class, String.class)
                    .newInstance(1, "header body UNKNOWN.FOOTER");

            var result = method.invoke(null, pageContent);
            assertEquals("INVALID", result.toString());
        }
    }
}