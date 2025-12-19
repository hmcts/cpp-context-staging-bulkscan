package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DrivingLicenseValidatorTest {
    private Validator validator;

    @BeforeEach
    public void setUp() {
        validator = new DrivingLicenseValidator();
    }

    @Test
    public void isValidPattern() {
        assertThat(validator.isValidPattern("MORGA657054SM9BF"), is(true));
    }

    @Test
    public void isInvalidPattern() {
        assertThat(validator.isValidPattern("RARA828080P98GB"), is(false));
    }
}