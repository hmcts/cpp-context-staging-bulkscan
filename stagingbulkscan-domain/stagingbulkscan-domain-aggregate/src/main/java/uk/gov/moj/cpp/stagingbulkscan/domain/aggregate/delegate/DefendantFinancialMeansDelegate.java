package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Benefits;
import uk.gov.justice.stagingbulkscan.domain.DocumentFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Employer;
import uk.gov.justice.stagingbulkscan.domain.Employment;
import uk.gov.justice.stagingbulkscan.domain.EmploymentStatus;
import uk.gov.justice.stagingbulkscan.domain.Frequency;
import uk.gov.justice.stagingbulkscan.domain.Income;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.NiNumberValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.PostcodeValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.common.validators.Validator;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

public class DefendantFinancialMeansDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    public DefendantFinancialMeansDelegate(final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento) {
        this.stagingBulkScanAggregateMemento = stagingBulkScanAggregateMemento;
    }

    public Stream<Object> updateDefendantFinancialMeans(final AllFinancialMeans allFinancialMeans, final UUID actionedBy) {
        final ScanDocument scanDocument = this.stagingBulkScanAggregateMemento.getScanEnvelope().getAssociatedScanDocuments().stream()
                .filter(d -> d.getScanDocumentId().equals(allFinancialMeans.getScanDocumentId()))
                .findFirst()
                .orElse(ScanDocument.scanDocument().build());

        if (isNull(scanDocument.getMc100s())) {
            return Stream.empty();
        }

        return updateDefendantFinancialMeans(allFinancialMeans, scanDocument.getMc100s(), scanDocument.getScanDocumentId(), actionedBy);
    }

    private Stream<Object> updateDefendantFinancialMeans(final AllFinancialMeans allFinancialMeans, final DocumentFinancialMeans documentFinancialMeans, final UUID scanDocumentId, final UUID actionedBy) {
        final Employer.Builder employerBuilder = Employer.employer();
        final Income.Builder incomeBuilder = Income.income();
        final Benefits.Builder benefitsBuilder = Benefits.benefits();
        final Employment.Builder employmentBuilder = Employment.employment();
        String niNumber = null;
        boolean documentFollowUpRequired;

        documentFollowUpRequired = processValidIncomeDetails(incomeBuilder, documentFinancialMeans);
        processValidBenefitsDetails(benefitsBuilder, documentFinancialMeans, allFinancialMeans);
        processValidEmployerDetails(employerBuilder, documentFinancialMeans, allFinancialMeans);
        processValidEmploymentDetails(employmentBuilder, documentFinancialMeans, allFinancialMeans);

        final Validator validator = new NiNumberValidator();
        if (isNotBlank(documentFinancialMeans.getNiNumber()) && validator.isValidPattern(documentFinancialMeans.getNiNumber())) {
            niNumber = documentFinancialMeans.getNiNumber();
        }

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = new DefendantFinancialMeansUpdated(
                scanDocumentId,
                employerBuilder.build(),
                benefitsBuilder.build(),
                employmentBuilder.build(),
                incomeBuilder.build(),
                niNumber,
                allFinancialMeans);

        if (documentFollowUpRequired) {
            final ScanDocumentFollowedUp scanDocumentFollowedUp = new ScanDocumentFollowedUp(allFinancialMeans.getScanEnvelopeId(), scanDocumentId, actionedBy, now(UTC));
            return Stream.of(defendantFinancialMeansUpdated, scanDocumentFollowedUp);
        } else {
            return Stream.of(defendantFinancialMeansUpdated);
        }

    }

    private void processValidBenefitsDetails(Benefits.Builder benefitsBuilder, DocumentFinancialMeans documentFinancialMeans, final AllFinancialMeans allFinancialMeans) {
        if (nonNull(documentFinancialMeans.getClaimBenefits()) && documentFinancialMeans.getClaimBenefits()) {
            benefitsBuilder.withClaimed(Boolean.TRUE);
            benefitsBuilder.withType(nonNull(allFinancialMeans.getBenefits()) && isNotBlank(allFinancialMeans.getBenefits().getType()) ? allFinancialMeans.getBenefits().getType() : null);
        } else {
            benefitsBuilder.withClaimed(Boolean.FALSE);
            benefitsBuilder.withType(nonNull(allFinancialMeans.getBenefits()) && isNotBlank(allFinancialMeans.getBenefits().getType()) ? allFinancialMeans.getBenefits().getType() : null);
        }
    }

    private void processValidEmployerDetails(final Employer.Builder employerBuilder, final DocumentFinancialMeans documentFinancialMeans, final AllFinancialMeans allFinancialMeans) {
        if (hasValidAddress(documentFinancialMeans)) {
            buildDefendantEmployerAddressDetails(employerBuilder, documentFinancialMeans, allFinancialMeans);
        }
    }

    private void processValidEmploymentDetails(final Employment.Builder employmentBuilder, final DocumentFinancialMeans documentFinancialMeans, final AllFinancialMeans allFinancialMeans) {
        employmentBuilder.withStatus(getValidEmploymentStatus(documentFinancialMeans));
        employmentBuilder.withDetails(nonNull(allFinancialMeans.getEmployment()) && nonNull(allFinancialMeans.getEmployment().getDetails()) ? allFinancialMeans.getEmployment().getDetails() : null);
    }

    private boolean processValidIncomeDetails(final Income.Builder incomeBuilder, final DocumentFinancialMeans documentFinancialMeans) {
        boolean documentFollowUpRequired = false;
        final boolean hasIncomeFrequency = isTrue(documentFinancialMeans.getFrequencyWeekly()) ||
                isTrue(documentFinancialMeans.getFrequencyFortnightly()) ||
                isTrue(documentFinancialMeans.getFrequencyMonthly()) ||
                isTrue(documentFinancialMeans.getFrequencyYearly());

        if (hasValidIncome(documentFinancialMeans) && !hasNoIncome(documentFinancialMeans)) {
            buildDefendantIncomeDetails(incomeBuilder, documentFinancialMeans);
        } else if (hasNoIncome(documentFinancialMeans) && !hasValidIncome(documentFinancialMeans)) {
            incomeBuilder
                    .withAmount(BigDecimal.ZERO)
                    .withFrequency(Frequency.YEARLY);
        } else if (hasIncomeFrequency &&
                isTrue(documentFinancialMeans.getNoIncome())) {
            documentFollowUpRequired = true;
        }
        return documentFollowUpRequired;
    }

    private boolean hasValidIncome(final DocumentFinancialMeans documentFinancialMeans) {
        final boolean hasIncomeFrequency = isTrue(documentFinancialMeans.getFrequencyWeekly()) ||
                isTrue(documentFinancialMeans.getFrequencyFortnightly()) ||
                isTrue(documentFinancialMeans.getFrequencyMonthly()) ||
                isTrue(documentFinancialMeans.getFrequencyYearly());

        return isNotBlank(documentFinancialMeans.getAverageIncome()) && new BigDecimal(documentFinancialMeans.getAverageIncome()).compareTo(BigDecimal.ZERO) > 0 &&
                hasIncomeFrequency && isNotTrue(documentFinancialMeans.getNoIncome());
    }

    private boolean hasNoIncome(final DocumentFinancialMeans documentFinancialMeans) {
        final boolean noAvgIncome = isNotBlank(documentFinancialMeans.getAverageIncome())
                && new BigDecimal(documentFinancialMeans.getAverageIncome()).compareTo(BigDecimal.ZERO) == 0;

        return (isBlank(documentFinancialMeans.getAverageIncome()) || noAvgIncome) && isTrue(documentFinancialMeans.getNoIncome());
    }

    private void buildDefendantIncomeDetails(final Income.Builder incomeBuilder, DocumentFinancialMeans documentFinancialMeans) {
        Frequency incomeFrequency = null;
        if (documentFinancialMeans.getFrequencyWeekly()) {
            incomeFrequency = Frequency.WEEKLY;
        }
        if (documentFinancialMeans.getFrequencyFortnightly()) {
            incomeFrequency = Frequency.FORTNIGHTLY;
        }
        if (documentFinancialMeans.getFrequencyMonthly()) {
            incomeFrequency = Frequency.MONTHLY;
        }
        if (documentFinancialMeans.getFrequencyYearly()) {
            incomeFrequency = Frequency.YEARLY;
        }
        incomeBuilder
                .withAmount(new BigDecimal(documentFinancialMeans.getAverageIncome()))
                .withFrequency(incomeFrequency);
    }

    private void buildDefendantEmployerAddressDetails(final Employer.Builder employerBuilder, final DocumentFinancialMeans documentFinancialMeans, final AllFinancialMeans allFinancialMeans) {
        final String phoneNumber = nonNull(allFinancialMeans.getEmployer()) && isNotBlank(allFinancialMeans.getEmployer().getPhone()) ? allFinancialMeans.getEmployer().getPhone() : null;
        final String sjpEmployerReference = nonNull(allFinancialMeans.getEmployer()) && isNotBlank(allFinancialMeans.getEmployer().getEmployeeReference()) ? allFinancialMeans.getEmployer().getEmployeeReference() : null;
        final String employerReference = isNotBlank(documentFinancialMeans.getPayrollNumber()) ? documentFinancialMeans.getPayrollNumber() : sjpEmployerReference;

        final Address address = Address.address()
                .withAddress1(documentFinancialMeans.getEmployerAddressLine1())
                .withAddress2(isNotBlank(documentFinancialMeans.getEmployerAddressLine2()) ? documentFinancialMeans.getEmployerAddressLine2() : null)
                .withAddress3(isNotBlank(documentFinancialMeans.getEmployerAddressLine3()) ? documentFinancialMeans.getEmployerAddressLine3() : null)
                .withAddress4(isNotBlank(documentFinancialMeans.getEmployerAddressCity()) ? documentFinancialMeans.getEmployerAddressCity() : null)
                .withAddress5(null)
                .withPostcode(documentFinancialMeans.getEmployerAddressPostCode())
                .build();

        employerBuilder
                .withName(documentFinancialMeans.getNameOfOrganization())
                .withPhone(phoneNumber)
                .withEmployeeReference(employerReference)
                .withAddress(address);
    }

    private EmploymentStatus getValidEmploymentStatus(final DocumentFinancialMeans documentFinancialMeans) {
        final int validStatuses = (int) Stream.of(documentFinancialMeans.getEmployed(), documentFinancialMeans.getSelfEmployed(),
                documentFinancialMeans.getUnemployed(), documentFinancialMeans.getOtherEmploymentStatus())
                .filter(validStatus -> validStatus).count();

        if (validStatuses != 1) {
            return EmploymentStatus.OTHER;
        }
        if (documentFinancialMeans.getEmployed()) {
            return EmploymentStatus.EMPLOYED;
        }
        if (documentFinancialMeans.getSelfEmployed()) {
            return EmploymentStatus.SELF_EMPLOYED;
        }
        if (documentFinancialMeans.getUnemployed()) {
            return EmploymentStatus.UNEMPLOYED;
        }

        return EmploymentStatus.OTHER;
    }

    private boolean hasValidAddress(final DocumentFinancialMeans documentFinancialMeans) {
        final Validator validator = new PostcodeValidator();
        return isNoneBlank(documentFinancialMeans.getNameOfOrganization(), documentFinancialMeans.getEmployerAddressLine1(), documentFinancialMeans.getEmployerAddressPostCode())
                && validator.isValidPattern(documentFinancialMeans.getEmployerAddressPostCode());
    }
}
