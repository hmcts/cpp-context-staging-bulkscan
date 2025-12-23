package uk.gov.moj.cpp.bulkscan.azure.function.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PageContentTest {

    @Test
    void testConstructorWithAllParameters() {
        int pageNumber = 1;
        String pageText = "Page content";

        PageContent pageContent = new PageContent(pageNumber, pageText);

        assertEquals(pageNumber, pageContent.getPageNumber());
        assertEquals(pageText, pageContent.getText());
    }

    @Test
    void testConstructorWithNullText() {
        PageContent pageContent = new PageContent(1, null);

        assertEquals("", pageContent.getText());
    }

    @Test
    void testConstructorWithEmptyStrings() {
        PageContent pageContent = new PageContent(2, "");

        assertEquals(2, pageContent.getPageNumber());
        assertEquals("", pageContent.getText());
    }

    @Test
    void testConstructorWithZeroPageNumber() {
        PageContent pageContent = new PageContent(0, "some text");

        assertEquals(0, pageContent.getPageNumber());
    }

    @Test
    void testConstructorWithNegativePageNumber() {
        PageContent pageContent = new PageContent(-1, "some text");

        assertEquals(-1, pageContent.getPageNumber());
    }

    @Test
    void testConstructorWithLargePageNumber() {
        int largePageNumber = Integer.MAX_VALUE;
        PageContent pageContent = new PageContent(largePageNumber, "some text");

        assertEquals(largePageNumber, pageContent.getPageNumber());
    }

    @Test
    void testImmutabilityOfTexts() {
        String pageText = "Original text";

        PageContent pageContent = new PageContent(1, pageText);

        // Verify that the original strings can be modified without affecting the PageContent
        pageText = "Modified text";

        assertEquals("Original text", pageContent.getText());
    }

    @Test
    void testSpecialCharactersInTexts() {
        String pageText = "Text with special chars: àáâãäåæçèéêë";

        PageContent pageContent = new PageContent(1, pageText);

        assertEquals(pageText, pageContent.getText());
    }

    @Test
    void testNewlineAndTabCharacters() {
        String pageText = "Line 1\nLine 2\tTabbed";

        PageContent pageContent = new PageContent(1, pageText);

        assertEquals(pageText, pageContent.getText());
    }

    @Test
    void testVeryLongTexts() {
        String longText = "A".repeat(10000);

        PageContent pageContent = new PageContent(1, longText);

        assertEquals(longText, pageContent.getText());
        assertEquals(10000, pageContent.getText().length());
    }
}