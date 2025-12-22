package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class DefendantDetailsRequestValidatorTest {

    @Test
    public void shouldUpdateValidInfo() {
        final String existingEmailAddress = "existing@email.test";
        final String newEmailAddress = "new@email.test";
        final String existingPhoneNumber = "07904180411";
        final String newPhoneNumber = "07904180422";
        final String drivingLicenseNumber = "RAMIP858080P99GB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                existingEmailAddress,
                newEmailAddress,
                existingPhoneNumber,
                newPhoneNumber, null, drivingLicenseNumber);

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(true));
        assertThat(defendantDetailsRequestValidator.getEmail(), is(newEmailAddress));
        assertThat(defendantDetailsRequestValidator.getPhoneNumber(), is(newPhoneNumber));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), is(drivingLicenseNumber));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch(), is(false));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberValid(), is(true));
    }

    @Test
    public void shouldNotUpdate() {
        final String existingEmailAddress = "existing@email.test";
        final String newEmailAddress = "invalidemail.test";
        final String existingPhoneNumber = "07904180411";
        final String newPhoneNumber = "invalidnumber";
        final String drivingLicenseNumber = "RAMIP858080P99GB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                existingEmailAddress,
                newEmailAddress,
                existingPhoneNumber,
                newPhoneNumber, drivingLicenseNumber, drivingLicenseNumber);

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(false));
        assertThat(defendantDetailsRequestValidator.getEmail(), is(existingEmailAddress));
        assertThat(defendantDetailsRequestValidator.getPhoneNumber(), is(existingPhoneNumber));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), is(drivingLicenseNumber));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch(), is(false));
    }

    @Test
    public void shouldNotUpdateIfAtLeastOneIsValid() {
        final String existingEmailAddress = "existing@email.test";
        final String newEmailAddress = "invalidemail.test";
        final String existingPhoneNumber = "07904180411";
        final String newPhoneNumber = "07904180422";
        final String drivingLicenseNumber = "RAMIS878080P99GW";
        final String existingDrivingLicenseNumber = "RAMIS878080P99BB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                existingEmailAddress,
                newEmailAddress,
                existingPhoneNumber,
                newPhoneNumber, existingDrivingLicenseNumber, drivingLicenseNumber);

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(true));
        assertThat(defendantDetailsRequestValidator.getEmail(), is(existingEmailAddress));
        assertThat(defendantDetailsRequestValidator.getPhoneNumber(), is(newPhoneNumber));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), is(existingDrivingLicenseNumber));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberValid(), is(false));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch(), is(true));
    }

    @Test
    public void shouldNotUpdateIfOnlyDrivingLicenseIsInvalid() {
        final String existingEmailAddress = "existing@email.test";
        final String newEmailAddress = "new@email.test";
        final String existingPhoneNumber = "07904180411";
        final String newPhoneNumber = "07904180422";
        final String drivingLicenseNumber = "RAMIT878080P99GB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                existingEmailAddress,
                newEmailAddress,
                existingPhoneNumber,
                newPhoneNumber, null, drivingLicenseNumber);

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(true));
        assertThat(defendantDetailsRequestValidator.getEmail(), is(newEmailAddress));
        assertThat(defendantDetailsRequestValidator.getPhoneNumber(), is(newPhoneNumber));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), nullValue());
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberValid(), is(false));
    }

    @Test
    public void shouldNotUpdateIfNewDrivingNumberIsNotProvided() {
        final String existingDrivingLicence = "RAMIP878080P99GB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                null,
                null,
                null,
                null, existingDrivingLicence, "");

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(false));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), is(existingDrivingLicence));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberValid(), is(true));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch(), is(false));
    }


    @Test
    public void shouldUpdateValidInfoWhenExistingEmailAddressIsEmpty() {
        final String existingEmailAddress = null;
        final String newEmailAddress = "new@email.test";
        final String existingPhoneNumber = "";
        final String newPhoneNumber = "07904180422";
        final String drivingLicenseNumber = "RAMIP858080P99GB";
        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                existingEmailAddress,
                newEmailAddress,
                existingPhoneNumber,
                newPhoneNumber, null, drivingLicenseNumber);

        assertThat(defendantDetailsRequestValidator.isDefendantDetailsUpdated(), is(true));
        assertThat(defendantDetailsRequestValidator.getEmail(), is(newEmailAddress));
        assertThat(defendantDetailsRequestValidator.getPhoneNumber(), is(newPhoneNumber));
        assertThat(defendantDetailsRequestValidator.getDrivingLicenseNumber(), is(drivingLicenseNumber));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch(), is(false));
        assertThat(defendantDetailsRequestValidator.isDrivingLicenseNumberValid(), is(true));
    }

}
