package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import java.util.regex.Pattern;

public class NiNumberValidator implements Validator {
    private static final Pattern NI_NUMBER_REGEX_PATTERN = Pattern.compile("^(?!BG)(?!GB)(?!NK)(?!KN)(?!TN)(?!NT)(?!ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9][0-9][0-9][0-9][0-9][0-9][A-D]$");

    public boolean isValidPattern(final String niNumber) {
        return NI_NUMBER_REGEX_PATTERN.matcher(niNumber).matches();
    }
}
