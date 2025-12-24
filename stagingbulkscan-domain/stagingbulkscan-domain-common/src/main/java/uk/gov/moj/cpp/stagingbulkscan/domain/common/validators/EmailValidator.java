package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

public class EmailValidator implements Validator {

    private static final org.apache.commons.validator.routines.EmailValidator
            EMAIL_VALIDATOR = org.apache.commons.validator.routines.EmailValidator.getInstance();

    public boolean isValidPattern(final String email) {
        return EMAIL_VALIDATOR.isValid(email);
    }
}