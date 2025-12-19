package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TelephoneValidator implements Validator {

    private static final String PHONE_REGEX = "^[0-9+\\ \\-]{10,}$";

    public boolean isValidPattern(final String value) {
        if (isBlank(value)) {
            return true;
        }

        return Pattern.matches(PHONE_REGEX, value);
    }
}
