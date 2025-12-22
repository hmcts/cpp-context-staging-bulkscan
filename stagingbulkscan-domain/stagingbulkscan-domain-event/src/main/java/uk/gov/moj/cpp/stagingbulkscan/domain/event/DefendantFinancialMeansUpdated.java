package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Benefits;
import uk.gov.justice.stagingbulkscan.domain.Employer;
import uk.gov.justice.stagingbulkscan.domain.Employment;
import uk.gov.justice.stagingbulkscan.domain.Income;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("stagingbulkscan.events.defendant-financial-means-updated")
public class DefendantFinancialMeansUpdated {
    private final UUID scanDocumentId;
    private final Employer employer;
    private final Benefits benefits;
    private final Employment employment;
    private final Income income;
    private final String nationalInsuranceNumber;
    private final AllFinancialMeans sjpAllFinancialMeans;

    @JsonCreator
    public DefendantFinancialMeansUpdated(@JsonProperty("scanDocumentId") final UUID scanDocumentId,
                                          @JsonProperty("employer") final Employer employer,
                                          @JsonProperty("benefits") final Benefits benefits,
                                          @JsonProperty("employment") final Employment employment,
                                          @JsonProperty("income") final Income income,
                                          @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
                                          @JsonProperty("sjpAllFinancialMeans") final AllFinancialMeans sjpAllFinancialMeans) {
        this.scanDocumentId = scanDocumentId;
        this.employer = employer;
        this.benefits = benefits;
        this.employment = employment;
        this.income = income;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.sjpAllFinancialMeans = sjpAllFinancialMeans;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public Employer getEmployer() {
        return employer;
    }

    public Benefits getBenefits() {
        return benefits;
    }

    public Employment getEmployment() {
        return employment;
    }

    public Income getIncome() {
        return income;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public AllFinancialMeans getSjpAllFinancialMeans() {
        return sjpAllFinancialMeans;
    }
}
