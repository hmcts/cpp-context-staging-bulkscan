package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SuppressWarnings("java:S5998")
public class EmailValidator implements Validator {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@(?=.*\\.[A-Za-z]{2,}$)[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*+$";

    public boolean isValidPattern(final String email) {
        if (isBlank(email)) {
            return true;
        }

        return Pattern.matches(EMAIL_REGEX, email);
    }
}
