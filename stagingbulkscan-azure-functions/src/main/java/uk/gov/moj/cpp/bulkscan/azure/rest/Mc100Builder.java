package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.util.Map;

class Mc100Builder {

    private static final String AVERAGE_INCOME = "averageIncome";
    private static final String CLAIM_BENEFITS_NO = "claimBenefitsNo";
    private static final String CLAIM_BENEFITS_YES = "claimBenefitsYes";
    private static final String EMPLOYED = "employed";
    private static final String EMPLOYER_ADDRESS_CITY = "employerAddressCity";
    private static final String EMPLOYER_ADDRESS_LINE1 = "employerAddressLine1";
    private static final String EMPLOYER_ADDRESS_LINE2 = "employerAddressLine2";
    private static final String EMPLOYER_ADDRESS_LINE3 = "employerAddressLine3";
    private static final String EMPLOYER_ADDRESS_POST_CODE = "employerAddressPostCode";
    private static final String FREQUENCY_FORTNIGHTLY = "frequencyFortnightly";
    private static final String FREQUENCY_MONTHLY = "frequencyMonthly";
    private static final String FREQUENCY_WEEKLY = "frequencyWeekly";
    private static final String FREQUENCY_YEARLY = "frequencyYearly";
    private static final String NAME_OF_ORGANISATION = "nameOfOrganization";
    private static final String NI_NUMBER = "niNumber";
    private static final String NO_INCOME = "noIncome";
    private static final String OTHER_EMPLOYMENT_STATUS = "otherEmploymentStatus";
    private static final String PAYROLL_NUMBER = "payrollNumber";
    private static final String SELF_EMPLOYED = "selfEmployed";
    private static final String UNEMPLOYED = "unemployed";

    Mc100s buildMC100(final Map<String, String> metadataMap) {
        final Mc100s.Builder builder = Mc100s.documentFinancialMeans();

        processIncomeDetails(builder, metadataMap);
        processEmploymentDetails(builder, metadataMap);
        processBenefitsDetails(builder, metadataMap);
        processEmployerDetails(builder, metadataMap);

        return builder.build();
    }

    private void processEmployerDetails(Mc100s.Builder builder, Map<String, String> metadataMap) {
        if (metadataMap.containsKey(EMPLOYER_ADDRESS_LINE1)) {
            builder.withEmployerAddressLine1(metadataMap.get(EMPLOYER_ADDRESS_LINE1));
        }

        if (metadataMap.containsKey(EMPLOYER_ADDRESS_LINE2)) {
            builder.withEmployerAddressLine2(metadataMap.get(EMPLOYER_ADDRESS_LINE2));
        }

        if (metadataMap.containsKey(EMPLOYER_ADDRESS_LINE3)) {
            builder.withEmployerAddressLine3(metadataMap.get(EMPLOYER_ADDRESS_LINE3));
        }

        if (metadataMap.containsKey(EMPLOYER_ADDRESS_CITY)) {
            builder.withEmployerAddressCity(metadataMap.get(EMPLOYER_ADDRESS_CITY));
        }

        if (metadataMap.containsKey(EMPLOYER_ADDRESS_POST_CODE)) {
            builder.withEmployerAddressPostCode(metadataMap.get(EMPLOYER_ADDRESS_POST_CODE));
        }

        if (metadataMap.containsKey(NAME_OF_ORGANISATION)) {
            builder.withNameOfOrganization(metadataMap.get(NAME_OF_ORGANISATION));
        }

        if (metadataMap.containsKey(NI_NUMBER)) {
            builder.withNiNumber(metadataMap.get(NI_NUMBER));
        }

        if (metadataMap.containsKey(PAYROLL_NUMBER)) {
            builder.withPayrollNumber(metadataMap.get(PAYROLL_NUMBER));
        }

    }

    private void processBenefitsDetails(Mc100s.Builder builder, Map<String, String> metadataMap) {

        final boolean claimBenefitsYes = metadataMap.containsKey(CLAIM_BENEFITS_YES) && Boolean.parseBoolean(metadataMap.get(CLAIM_BENEFITS_YES));
        final boolean claimBenefitsNo = metadataMap.containsKey(CLAIM_BENEFITS_NO) && Boolean.parseBoolean(metadataMap.get(CLAIM_BENEFITS_NO));

        builder.withClaimBenefits(false);

        if(claimBenefitsYes && !claimBenefitsNo) {
            builder.withClaimBenefits(true);
        }

        if(claimBenefitsYes && claimBenefitsNo) {
            builder.withClaimBenefits(null);
        }
    }

    private void processEmploymentDetails(Mc100s.Builder builder, Map<String, String> metadataMap) {

        final boolean employed = metadataMap.containsKey(EMPLOYED) && Boolean.parseBoolean(metadataMap.get(EMPLOYED));
        final boolean selfEmployed = metadataMap.containsKey(SELF_EMPLOYED) && Boolean.parseBoolean(metadataMap.get(SELF_EMPLOYED));
        final boolean unemployed = metadataMap.containsKey(UNEMPLOYED) && Boolean.parseBoolean(metadataMap.get(UNEMPLOYED));
        final boolean otherEmploymentStatus = metadataMap.containsKey(OTHER_EMPLOYMENT_STATUS) && Boolean.parseBoolean(metadataMap.get(OTHER_EMPLOYMENT_STATUS));

        builder.withEmployed(employed);
        builder.withSelfEmployed(selfEmployed);
        builder.withUnemployed(unemployed);
        builder.withOtherEmploymentStatus(otherEmploymentStatus);
    }

    private void processIncomeDetails(final Mc100s.Builder builder, final Map<String, String> metadataMap) {

        final boolean frequencyWeekly = metadataMap.containsKey(FREQUENCY_WEEKLY) && Boolean.parseBoolean(metadataMap.get(FREQUENCY_WEEKLY));
        final boolean frequencyFortnightly = metadataMap.containsKey(FREQUENCY_FORTNIGHTLY) && Boolean.parseBoolean(metadataMap.get(FREQUENCY_FORTNIGHTLY));
        final boolean frequencyMonthly = metadataMap.containsKey(FREQUENCY_MONTHLY) && Boolean.parseBoolean(metadataMap.get(FREQUENCY_MONTHLY));
        final boolean frequencyYearly = metadataMap.containsKey(FREQUENCY_YEARLY) && Boolean.parseBoolean(metadataMap.get(FREQUENCY_YEARLY));
        final boolean noIncome = metadataMap.containsKey(NO_INCOME) && Boolean.parseBoolean(metadataMap.get(NO_INCOME));

        builder.withFrequencyWeekly(frequencyWeekly);
        builder.withFrequencyFortnightly(frequencyFortnightly);
        builder.withFrequencyMonthly(frequencyMonthly);
        builder.withFrequencyYearly(frequencyYearly);
        builder.withNoIncome(noIncome);

        if (metadataMap.containsKey(AVERAGE_INCOME)) {
            builder.withAverageIncome(metadataMap.get(AVERAGE_INCOME));
        }
    }
}
