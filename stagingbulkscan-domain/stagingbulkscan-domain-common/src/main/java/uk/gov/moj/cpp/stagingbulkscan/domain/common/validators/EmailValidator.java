package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class EmailValidator implements Validator {

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$";

    public boolean isValidPattern(final String email) {
        if (isBlank(email)) {
            return true;
        }

        return Pattern.matches(EMAIL_REGEX, email);
    }
}
