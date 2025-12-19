package uk.gov.moj.cpp.stagingbulkscan.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ProcessorUtilTest {
    @Test
    public void shouldValidateUKPostCode() {
        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("EC1A1BB"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("EC1A 1BB"));

        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("W1A1HQ"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("W1A 1HQ"));

        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("M11AA"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("M1 1AA"));

        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("B338TH"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("B33 8TH"));

        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("CR26XH"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("CR2 6XH"));

        assertFalse(ProcessorUtil.isUkGovPostCodeValidWithSpace("DN551PT"));
        assertTrue(ProcessorUtil.isUkGovPostCodeValidWithSpace("DN55 1PT"));
    }

    @Test
    public void shouldFixUKPostCodeByInsertingSpace() {
        assertEquals("EC1A 1BB", ProcessorUtil.fixPostCodeSpacing("EC1A1BB"));
        assertEquals("W1A 1HQ", ProcessorUtil.fixPostCodeSpacing("W1A1HQ"));
        assertEquals("M1 1AA", ProcessorUtil.fixPostCodeSpacing("M11AA"));
        assertEquals("EC1A 1BB", ProcessorUtil.fixPostCodeSpacing("EC1A1BB"));
        assertEquals("B33 8TH", ProcessorUtil.fixPostCodeSpacing("B338TH"));
        assertEquals("CR2 6XH", ProcessorUtil.fixPostCodeSpacing("CR26XH"));
        assertEquals("DN55 1PT", ProcessorUtil.fixPostCodeSpacing("DN551PT"));
    }
}
