package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings({"squid:S00107"})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mc100s implements Serializable {
    private static final long serialVersionUID = -2287352870561308209L;

    private final String averageIncome;

    private final Boolean claimBenefits;

    private final Boolean employed;

    private final String employerAddressCity;

    private final String employerAddressLine1;

    private final String employerAddressLine2;

    private final String employerAddressLine3;

    private final String employerAddressPostCode;

    private final Boolean frequencyFortnightly;

    private final Boolean frequencyMonthly;

    private final Boolean frequencyWeekly;

    private final Boolean frequencyYearly;

    private final String nameOfOrganization;

    private final String niNumber;

    private final Boolean noIncome;

    private final Boolean otherEmploymentStatus;

    private final String payrollNumber;

    private final Boolean selfEmployed;

    private final Boolean unemployed;

    public Mc100s(final String averageIncome, final Boolean claimBenefits, final Boolean employed, final String employerAddressCity, final String employerAddressLine1, final String employerAddressLine2, final String employerAddressLine3, final String employerAddressPostCode, final Boolean frequencyFortnightly, final Boolean frequencyMonthly, final Boolean frequencyWeekly, final Boolean frequencyYearly, final String nameOfOrganization, final String niNumber, final Boolean noIncome, final Boolean otherEmploymentStatus, final String payrollNumber, final Boolean selfEmployed, final Boolean unemployed) {
        this.averageIncome = averageIncome;
        this.claimBenefits = claimBenefits;
        this.employed = employed;
        this.employerAddressCity = employerAddressCity;
        this.employerAddressLine1 = employerAddressLine1;
        this.employerAddressLine2 = employerAddressLine2;
        this.employerAddressLine3 = employerAddressLine3;
        this.employerAddressPostCode = employerAddressPostCode;
        this.frequencyFortnightly = frequencyFortnightly;
        this.frequencyMonthly = frequencyMonthly;
        this.frequencyWeekly = frequencyWeekly;
        this.frequencyYearly = frequencyYearly;
        this.nameOfOrganization = nameOfOrganization;
        this.niNumber = niNumber;
        this.noIncome = noIncome;
        this.otherEmploymentStatus = otherEmploymentStatus;
        this.payrollNumber = payrollNumber;
        this.selfEmployed = selfEmployed;
        this.unemployed = unemployed;
    }

    public String getAverageIncome() {
        return averageIncome;
    }

    public Boolean getClaimBenefits() {
        return claimBenefits;
    }

    public Boolean getEmployed() {
        return employed;
    }

    public String getEmployerAddressCity() {
        return employerAddressCity;
    }

    public String getEmployerAddressLine1() {
        return employerAddressLine1;
    }

    public String getEmployerAddressLine2() {
        return employerAddressLine2;
    }

    public String getEmployerAddressLine3() {
        return employerAddressLine3;
    }

    public String getEmployerAddressPostCode() {
        return employerAddressPostCode;
    }

    public Boolean getFrequencyFortnightly() {
        return frequencyFortnightly;
    }

    public Boolean getFrequencyMonthly() {
        return frequencyMonthly;
    }

    public Boolean getFrequencyWeekly() {
        return frequencyWeekly;
    }

    public Boolean getFrequencyYearly() {
        return frequencyYearly;
    }

    public String getNameOfOrganization() {
        return nameOfOrganization;
    }

    public String getNiNumber() {
        return niNumber;
    }

    public Boolean getNoIncome() {
        return noIncome;
    }

    public Boolean getOtherEmploymentStatus() {
        return otherEmploymentStatus;
    }

    public String getPayrollNumber() {
        return payrollNumber;
    }

    public Boolean getSelfEmployed() {
        return selfEmployed;
    }

    public Boolean getUnemployed() {
        return unemployed;
    }

    public static Builder documentFinancialMeans() {
        return new Mc100s.Builder();
    }

    @Override
    public String toString() {
        return "DocumentFinancialMeans{" +
                "averageIncome='" + averageIncome + "'," +
                "claimBenefits='" + claimBenefits + "'," +
                "employed='" + employed + "'," +
                "employerAddressCity='" + employerAddressCity + "'," +
                "employerAddressLine1='" + employerAddressLine1 + "'," +
                "employerAddressLine2='" + employerAddressLine2 + "'," +
                "employerAddressLine3='" + employerAddressLine3 + "'," +
                "employerAddressPostCode='" + employerAddressPostCode + "'," +
                "frequencyFortnightly='" + frequencyFortnightly + "'," +
                "frequencyMonthly='" + frequencyMonthly + "'," +
                "frequencyWeekly='" + frequencyWeekly + "'," +
                "frequencyYearly='" + frequencyYearly + "'," +
                "nameOfOrganization='" + nameOfOrganization + "'," +
                "niNumber='" + niNumber + "'," +
                "noIncome='" + noIncome + "'," +
                "otherEmploymentStatus='" + otherEmploymentStatus + "'," +
                "payrollNumber='" + payrollNumber + "'," +
                "selfEmployed='" + selfEmployed + "'," +
                "unemployed='" + unemployed + "'" +
                "}";
    }

    public static class Builder {
        private String averageIncome;

        private Boolean claimBenefits;

        private Boolean employed;

        private String employerAddressCity;

        private String employerAddressLine1;

        private String employerAddressLine2;

        private String employerAddressLine3;

        private String employerAddressPostCode;

        private Boolean frequencyFortnightly;

        private Boolean frequencyMonthly;

        private Boolean frequencyWeekly;

        private Boolean frequencyYearly;

        private String nameOfOrganization;

        private String niNumber;

        private Boolean noIncome;

        private Boolean otherEmploymentStatus;

        private String payrollNumber;

        private Boolean selfEmployed;

        private Boolean unemployed;

        public Builder withAverageIncome(final String averageIncome) {
            this.averageIncome = averageIncome;
            return this;
        }

        public Builder withClaimBenefits(final Boolean claimBenefits) {
            this.claimBenefits = claimBenefits;
            return this;
        }

        public Builder withEmployed(final Boolean employed) {
            this.employed = employed;
            return this;
        }

        public Builder withEmployerAddressCity(final String employerAddressCity) {
            this.employerAddressCity = employerAddressCity;
            return this;
        }

        public Builder withEmployerAddressLine1(final String employerAddressLine1) {
            this.employerAddressLine1 = employerAddressLine1;
            return this;
        }

        public Builder withEmployerAddressLine2(final String employerAddressLine2) {
            this.employerAddressLine2 = employerAddressLine2;
            return this;
        }

        public Builder withEmployerAddressLine3(final String employerAddressLine3) {
            this.employerAddressLine3 = employerAddressLine3;
            return this;
        }

        public Builder withEmployerAddressPostCode(final String employerAddressPostCode) {
            this.employerAddressPostCode = employerAddressPostCode;
            return this;
        }

        public Builder withFrequencyFortnightly(final Boolean frequencyFortnightly) {
            this.frequencyFortnightly = frequencyFortnightly;
            return this;
        }

        public Builder withFrequencyMonthly(final Boolean frequencyMonthly) {
            this.frequencyMonthly = frequencyMonthly;
            return this;
        }

        public Builder withFrequencyWeekly(final Boolean frequencyWeekly) {
            this.frequencyWeekly = frequencyWeekly;
            return this;
        }

        public Builder withFrequencyYearly(final Boolean frequencyYearly) {
            this.frequencyYearly = frequencyYearly;
            return this;
        }

        public Builder withNameOfOrganization(final String nameOfOrganization) {
            this.nameOfOrganization = nameOfOrganization;
            return this;
        }

        public Builder withNiNumber(final String niNumber) {
            this.niNumber = niNumber;
            return this;
        }

        public Builder withNoIncome(final Boolean noIncome) {
            this.noIncome = noIncome;
            return this;
        }

        public Builder withOtherEmploymentStatus(final Boolean otherEmploymentStatus) {
            this.otherEmploymentStatus = otherEmploymentStatus;
            return this;
        }

        public Builder withPayrollNumber(final String payrollNumber) {
            this.payrollNumber = payrollNumber;
            return this;
        }

        public Builder withSelfEmployed(final Boolean selfEmployed) {
            this.selfEmployed = selfEmployed;
            return this;
        }

        public Builder withUnemployed(final Boolean unemployed) {
            this.unemployed = unemployed;
            return this;
        }

        public Mc100s build() {
            return new Mc100s(averageIncome, claimBenefits, employed, employerAddressCity, employerAddressLine1, employerAddressLine2, employerAddressLine3, employerAddressPostCode, frequencyFortnightly, frequencyMonthly, frequencyWeekly, frequencyYearly, nameOfOrganization, niNumber, noIncome, otherEmploymentStatus, payrollNumber, selfEmployed, unemployed);
        }
    }
}
