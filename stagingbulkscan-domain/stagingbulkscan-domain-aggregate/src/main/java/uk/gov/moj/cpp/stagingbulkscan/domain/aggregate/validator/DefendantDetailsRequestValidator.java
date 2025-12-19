package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.DrivingLicenseValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.EmailValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.TelephoneValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.Validator;

import java.util.function.BiPredicate;

public class DefendantDetailsRequestValidator {

    private final String email;

    private final String phoneNumber;

    private final String drivingLicenseNumber;

    private final Validator emailValidator;

    private final Validator telephoneValidator;

    private final Validator drivingLicenseNumberValidator;

    private boolean defendantDetailsUpdated = false;

    private boolean drivingLicenseNumberMismatch = false;

    private boolean drivingLicenseNumberValid = false;

    public DefendantDetailsRequestValidator(final String existingEmailAddress,
                                            final String newEmailAddress,
                                            final String existingPhoneNumber,
                                            final String newPhoneNumber,
                                            final String existingDrivingLicenseNumber,
                                            final String drivingLicenseNumber) {
        this.emailValidator = new EmailValidator();
        this.telephoneValidator = new TelephoneValidator();
        this.drivingLicenseNumberValidator = new DrivingLicenseValidator();
        this.email = setValidEmailAddress(existingEmailAddress, newEmailAddress);
        this.phoneNumber = setValidPhoneNumber(existingPhoneNumber, newPhoneNumber);
        this.drivingLicenseNumber = setValidDrivingLicenseNumber(existingDrivingLicenseNumber, drivingLicenseNumber);
    }

    private String setValidEmailAddress(final String existingEmailAddress, final String newEmailAddress) {

        final BiPredicate<String, String> predicate = isValidPattern(emailValidator);

        if (predicate.test(existingEmailAddress, newEmailAddress)) {
            defendantDetailsUpdated = true;
            return newEmailAddress;
        }

        return existingEmailAddress;
    }

    private String setValidPhoneNumber(final String existingPhoneNumber, final String newPhoneNumber) {

        final BiPredicate<String, String> predicate = isValidPattern(telephoneValidator);

        if (predicate.test(existingPhoneNumber, newPhoneNumber)) {
            defendantDetailsUpdated = true;
            return newPhoneNumber;
        }

        return existingPhoneNumber;
    }

    private BiPredicate<String, String> isValidPattern(final Validator validator) {
        return (existing, newString) -> isNotBlank(newString) && (existing == null || !existing.equalsIgnoreCase(newString)) && validator.isValidPattern(newString);
    }

    private String setValidDrivingLicenseNumber(final String existingDrivingLicenseNum, final String newDrivingLicenseNumber) {
        if (isBlank(newDrivingLicenseNumber)) {
            drivingLicenseNumberValid = true;
            return existingDrivingLicenseNum;
        }
        if (isNotBlank(existingDrivingLicenseNum)) {
            if (!existingDrivingLicenseNum.equals(newDrivingLicenseNumber)) {
                drivingLicenseNumberMismatch = true;
            } else {
                drivingLicenseNumberValid = true;
            }
        } else if (this.drivingLicenseNumberValidator.isValidPattern(newDrivingLicenseNumber)) {
            defendantDetailsUpdated = true;
            drivingLicenseNumberValid = true;
            return newDrivingLicenseNumber;
        }
        return existingDrivingLicenseNum;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isDefendantDetailsUpdated() {
        return defendantDetailsUpdated;
    }

    public String getDrivingLicenseNumber() {
        return drivingLicenseNumber;
    }


    public boolean isDrivingLicenseNumberValid() {
        return drivingLicenseNumberValid;
    }

    public boolean isDrivingLicenseNumberMismatch() {
        return drivingLicenseNumberMismatch;
    }
}
