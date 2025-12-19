package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.NiNumberValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.PostcodeValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PostcodeValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setup() {
        validator = new PostcodeValidator();
    }


    @Test
    public void testPostcodeRegEx() {
        assertTrue(validator.isValidPattern("EC1A 1BB"));
        assertFalse(validator.isValidPattern("123456"));
        assertTrue(validator.isValidPattern("EC1A1BB"));
    }
}
