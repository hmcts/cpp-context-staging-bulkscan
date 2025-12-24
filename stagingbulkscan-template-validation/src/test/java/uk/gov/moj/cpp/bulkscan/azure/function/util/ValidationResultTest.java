package uk.gov.moj.cpp.bulkscan.azure.function.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ValidationResultTest {

    private ValidationResult validationResult;

    @BeforeEach
    void setUp() {
        validationResult = new ValidationResult();
    }

    @Test
    void testInitialState() {
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.getErrors().isEmpty());
        assertTrue(validationResult.getDetails().isEmpty());
        assertNull(validationResult.getDocumentType());
    }

    @Test
    void testAddError() {
        String error = "Test error message";
        validationResult.addError(error);

        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals(error, validationResult.getErrors().get(0));
    }

    @Test
    void testAddMultipleErrors() {
        String error1 = "Error 1";
        String error2 = "Error 2";

        validationResult.addError(error1);
        validationResult.addError(error2);

        assertFalse(validationResult.isValid());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(error1, validationResult.getErrors().get(0));
        assertEquals(error2, validationResult.getErrors().get(1));
    }

    @Test
    void testAddDetail() {
        String detail = "Test detail message";
        validationResult.addDetail(detail);

        assertTrue(validationResult.isValid()); // Details don't affect validity
        assertEquals(1, validationResult.getDetails().size());
        assertEquals(detail, validationResult.getDetails().get(0));
    }

    @Test
    void testAddMultipleDetails() {
        String detail1 = "Detail 1";
        String detail2 = "Detail 2";

        validationResult.addDetail(detail1);
        validationResult.addDetail(detail2);

        assertEquals(2, validationResult.getDetails().size());
        assertEquals(detail1, validationResult.getDetails().get(0));
        assertEquals(detail2, validationResult.getDetails().get(1));
    }

    @Test
    void testSetAndGetDocumentType() {
        String documentType = "ENGLISH";
        validationResult.setDocumentType(documentType);

        assertEquals(documentType, validationResult.getDocumentType());
    }

    @Test
    void testIsValidWithErrors() {
        validationResult.addError("Some error");
        assertFalse(validationResult.isValid());
    }

    @Test
    void testIsValidWithoutErrors() {
        validationResult.addDetail("Some detail");
        assertTrue(validationResult.isValid());
    }

    @Test
    void testGetErrorsReturnsDefensiveCopy() {
        validationResult.addError("Original error");
        var errors = validationResult.getErrors();
        errors.add("Modified error");

        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Original error", validationResult.getErrors().get(0));
    }

    @Test
    void testGetDetailsReturnsDefensiveCopy() {
        validationResult.addDetail("Original detail");
        var details = validationResult.getDetails();
        details.add("Modified detail");

        assertEquals(1, validationResult.getDetails().size());
        assertEquals("Original detail", validationResult.getDetails().get(0));
    }

    @Test
    void testMixedErrorsAndDetails() {
        validationResult.addError("Error 1");
        validationResult.addDetail("Detail 1");
        validationResult.addError("Error 2");
        validationResult.addDetail("Detail 2");

        assertFalse(validationResult.isValid());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(2, validationResult.getDetails().size());
    }

    @Test
    void testNullDocumentType() {
        validationResult.setDocumentType(null);
        assertNull(validationResult.getDocumentType());
    }

    @Test
    void testEmptyStringDocumentType() {
        validationResult.setDocumentType("");
        assertEquals("", validationResult.getDocumentType());
    }
}