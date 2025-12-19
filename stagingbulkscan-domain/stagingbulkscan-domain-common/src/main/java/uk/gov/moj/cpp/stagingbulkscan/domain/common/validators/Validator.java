package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

public interface Validator {
    boolean isValidPattern(final String email);
}
