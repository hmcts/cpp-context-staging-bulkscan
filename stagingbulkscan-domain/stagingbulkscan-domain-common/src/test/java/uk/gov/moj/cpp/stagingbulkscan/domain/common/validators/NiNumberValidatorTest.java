package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NiNumberValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setup() {
        validator = new NiNumberValidator();
    }

    @Test
    public void testNiNumberRegEx() {
        assertTrue(validator.isValidPattern("AB123456C"));
        assertFalse(validator.isValidPattern("123456"));
    }
}
