package uk.gov.moj.cpp.stagingbulkscan.domain.common.validators;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.regex.Pattern;

public class DrivingLicenseValidator implements Validator {

    private static final String DRIVING_LICENSE_NUMBER_REGEX = "^[A-Z0-9]{5}\\d[0156]\\d([0][1-9]|[12]\\d|3[01])\\d[A-Z0-9]{3}[A-Z]{2}$";

    @Override
    public boolean isValidPattern(final String drivingLicenseNumber) {
        if (isBlank(drivingLicenseNumber)) {
            return false;
        }
        return Pattern.compile(DRIVING_LICENSE_NUMBER_REGEX).matcher(drivingLicenseNumber).matches();
    }
}
