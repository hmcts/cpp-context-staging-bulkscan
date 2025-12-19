package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Plea implements Serializable {
    private static final long serialVersionUID = -2287352870561308209L;

    private final String contactNumber;

    private final Boolean detailsCorrect;

    private final String drivingLicenceNumber;

    private final String emailAddress;

    private final Interpreter interpreter;

    private final List<Offence> offences;

    private final Boolean welshHearing;

    private final Boolean wishToComeToCourt;

    public Plea(final String contactNumber, final Boolean detailsCorrect, final String drivingLicenceNumber, final String emailAddress, final Interpreter interpreter, final List<Offence> offences, final Boolean welshHearing, final Boolean wishToComeToCourt) {
        this.contactNumber = contactNumber;
        this.detailsCorrect = detailsCorrect;
        this.drivingLicenceNumber = drivingLicenceNumber;
        this.emailAddress = emailAddress;
        this.interpreter = interpreter;
        this.offences = offences;
        this.welshHearing = welshHearing;
        this.wishToComeToCourt = wishToComeToCourt;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public Boolean getDetailsCorrect() {
        return detailsCorrect;
    }

    public String getDrivingLicenceNumber() {
        return drivingLicenceNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public Boolean getWelshHearing() {
        return welshHearing;
    }

    public Boolean getWishToComeToCourt() {
        return wishToComeToCourt;
    }

    public static Builder plea() {
        return new Plea.Builder();
    }

    @Override
    public String toString() {
        return "Plea{" +
                "contactNumber='" + contactNumber + "'," +
                "detailsCorrect='" + detailsCorrect + "'," +
                "drivingLicenceNumber='" + drivingLicenceNumber + "'," +
                "emailAddress='" + emailAddress + "'," +
                "interpreter='" + interpreter + "'," +
                "offences='" + offences + "'," +
                "welshHearing='" + welshHearing + "'," +
                "wishToComeToCourt='" + wishToComeToCourt + "'" +
                "}";
    }

    public static class Builder {
        private String contactNumber;

        private Boolean detailsCorrect;

        private String drivingLicenceNumber;

        private String emailAddress;

        private Interpreter interpreter;

        private List<Offence> offences;

        private Boolean welshHearing;

        private Boolean wishToComeToCourt;

        public Builder withContactNumber(final String contactNumber) {
            this.contactNumber = contactNumber;
            return this;
        }

        public Builder withDetailsCorrect(final Boolean detailsCorrect) {
            this.detailsCorrect = detailsCorrect;
            return this;
        }

        public Builder withDrivingLicenceNumber(final String drivingLicenceNumber) {
            this.drivingLicenceNumber = drivingLicenceNumber;
            return this;
        }

        public Builder withEmailAddress(final String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder withInterpreter(final Interpreter interpreter) {
            this.interpreter = interpreter;
            return this;
        }

        public Builder withOffences(final List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public Builder withWelshHearing(final Boolean welshHearing) {
            this.welshHearing = welshHearing;
            return this;
        }

        public Builder withWishToComeToCourt(final Boolean wishToComeToCourt) {
            this.wishToComeToCourt = wishToComeToCourt;
            return this;
        }

        public Plea build() {
            return new Plea(contactNumber, detailsCorrect, drivingLicenceNumber, emailAddress, interpreter, offences, welshHearing, wishToComeToCourt);
        }
    }
}
