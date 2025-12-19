package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Gender;
import uk.gov.justice.stagingbulkscan.domain.Title;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DefendantDetailsUpdated.EVENT_NAME)
public class DefendantDetailsUpdated implements Serializable {

    public static final String EVENT_NAME = "stagingbulkscan.events.defendant-details-updated";

    private static final long serialVersionUID = 1L;

    private final Address address;

    private final UUID caseId;

    private final ContactDetails contactDetails;

    private final String dateOfBirth;

    private final String email;

    private final String firstName;

    private final Gender gender;

    private final UUID id;

    private final String lastName;

    private final String nationalInsuranceNumber;

    private final UUID scanDocumentId;

    private final UUID scanEnvelopeId;

    private final Title title;

    private final String driverNumber;

    @JsonCreator
    public DefendantDetailsUpdated(
            final UUID scanDocumentId,
            final UUID caseId,
            final UUID scanEnvelopeId,
            @JsonProperty("id") final UUID id,
            @JsonProperty("title") final Title title,
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("dateOfBirth") final String dateOfBirth,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("email") final String email,
            @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
            @JsonProperty("contactDetails") final ContactDetails contactDetails,
            @JsonProperty("address") final Address address,
            @JsonProperty("driverNumber") final String driverNumber) {
        this.address = address;
        this.caseId = caseId;
        this.contactDetails = contactDetails;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.firstName = firstName;
        this.gender = gender;
        this.id = id;
        this.lastName = lastName;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.scanDocumentId = scanDocumentId;
        this.scanEnvelopeId = scanEnvelopeId;
        this.title = title;
        this.driverNumber = driverNumber;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Title getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public Address getAddress() {
        return address;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public static final class Builder {

        private Address address;

        private UUID caseId;

        private ContactDetails contactDetails;

        private String dateOfBirth;

        private String email;

        private String firstName;

        private Gender gender;

        private UUID id;

        private String lastName;

        private String nationalInsuranceNumber;

        private UUID scanDocumentId;

        private UUID scanEnvelopeId;

        private Title title;

        private String driverNumber;

        public DefendantDetailsUpdated.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantDetailsUpdated.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public DefendantDetailsUpdated.Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public DefendantDetailsUpdated.Builder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public DefendantDetailsUpdated.Builder withDateOfBirth(final String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }


        public DefendantDetailsUpdated.Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public DefendantDetailsUpdated.Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantDetailsUpdated.Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantDetailsUpdated.Builder withScanDocumentId(final UUID scanDocumentId) {
            this.scanDocumentId = scanDocumentId;
            return this;
        }

        public DefendantDetailsUpdated.Builder withScanEnvelopeId(final UUID scanEnvelopeId) {
            this.scanEnvelopeId = scanEnvelopeId;
            return this;
        }

        public DefendantDetailsUpdated.Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantDetailsUpdated.Builder withTitle(final Title title) {
            this.title = title;
            return this;
        }

        public DefendantDetailsUpdated.Builder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public DefendantDetailsUpdated.Builder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }


        public DefendantDetailsUpdated build() {
            return new DefendantDetailsUpdated(this.scanDocumentId, this.caseId, this.scanEnvelopeId, this.id,
                    this.title, this.firstName, this.lastName, this.dateOfBirth, this.gender, this.email,
                    this.nationalInsuranceNumber, this.contactDetails, this.address, this.driverNumber);
        }
    }
}
