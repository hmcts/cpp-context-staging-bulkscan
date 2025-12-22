package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EmailValidatorTest {

    private Validator validator;

    @BeforeEach
    public void setup() {
        validator = new EmailValidator();
    }

    @Test
    public void isValidPattern() {
        assertTrue(validator.isValidPattern("jsmith@hmcts.net"));
        assertFalse(validator.isValidPattern("test@tes."));
    }
}