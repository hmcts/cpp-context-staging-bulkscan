package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TelephoneValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setup() {
        validator = new TelephoneValidator();
    }

    @Test
    public void isValidPattern() {
        assertTrue(validator.isValidPattern("079041804111"));
        assertTrue(validator.isValidPattern("07904 103 122"));
        assertFalse(validator.isValidPattern("1221212"));
    }
}