package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.stagingbulkscan.event.ProcessorUtil.fixPostCodeSpacing;
import static uk.gov.moj.cpp.stagingbulkscan.event.ProcessorUtil.isUkGovPostCodeValidWithSpace;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Employer;
import uk.gov.justice.stagingbulkscan.domain.FinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Frequency;
import uk.gov.justice.stagingbulkscan.domain.Income;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdated;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;

import java.math.BigDecimal;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateDefendantFinancialMeansProcessor {

    private static final String EVENT_RECEIVED_LOG_TEMPLATE = "{} event received {}";

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantFinancialMeansProcessor.class);

    private static final String COMMAND_SJP_UPDATE_DEFENDANT_FINANCIAL_MEANS = "sjp.update-all-financial-means";

    private static final String COMMAND_UPDATE_DEFENDANT_FINANCIAL_MEANS = "stagingbulkscan.command.update-defendant-financial-means";

    private static final String COMMAND_UPDATE_DEFENDANT_NI_NUMBER = "sjp.update-defendant-national-insurance-number";

    private static final String FIELD_CASE_URN = "caseUrn";

    private static final String FIELD_DEFENDANT = "defendant";

    private static final String FIELD_PERSONAL_DETAILS = "personalDetails";

    private static final String FIELD_ID = "id";

    private static final String FIELD_NI_NUMBER = "nationalInsuranceNumber";

    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";

    private static final String FIELD_SCAN_ENVELOPE_ID = "scanEnvelopeId";

    @Inject
    private Sender sender;

    @Inject
    private SJPService sjpService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles(DefendantFinancialMeansUpdateRequested.EVENT_NAME)
    public void defendantFinancialMeansUpdateRequested(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final String caseUrn = jsonObject.getString(FIELD_CASE_URN);
        final JsonObject caseDetails = sjpService.getCaseDetails(caseUrn, envelope);
        if (nonNull(caseDetails)) {
            final UUID caseId = fromString(caseDetails.getString(FIELD_ID));
            final UUID envelopeId = fromString(jsonObject.getString(FIELD_SCAN_ENVELOPE_ID));
            final UUID scanDocumentId = fromString(jsonObject.getString(FIELD_SCAN_DOCUMENT_ID));

            final UUID defendantId = fromString(caseDetails.getJsonObject(FIELD_DEFENDANT).getString(FIELD_ID));
            final String defendantNiNumber = caseDetails.getJsonObject(FIELD_DEFENDANT).getJsonObject(FIELD_PERSONAL_DETAILS).containsKey(FIELD_NI_NUMBER) ?
                    caseDetails.getJsonObject(FIELD_DEFENDANT).getJsonObject(FIELD_PERSONAL_DETAILS).getString(FIELD_NI_NUMBER) : null;

            final FinancialMeans defendantFinancialMeans = sjpService.getDefendantFinancialMeans(defendantId, envelope);

            final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                    .withScanEnvelopeId(envelopeId)
                    .withScanDocumentId(scanDocumentId)
                    .withCaseId(caseId)
                    .withDefendantId(defendantId)
                    .withNationalInsuranceNumber(defendantNiNumber)
                    .withIncome(defendantFinancialMeans.getIncome())
                    .withBenefits(defendantFinancialMeans.getBenefits())
                    .withEmployment(defendantFinancialMeans.getEmployment())
                    .withEmployer(defendantFinancialMeans.getEmployer())
                    .build();

            sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANT_FINANCIAL_MEANS).build(), allFinancialMeans));
        }
    }

    @Handles("stagingbulkscan.events.defendant-financial-means-updated")
    public void handleDefendantFinancialMeansUpdatedEvent(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_RECEIVED_LOG_TEMPLATE, "stagingbulkscan.events.defendant-financial-means-updated", envelope.toObfuscatedDebugString());
        }

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantFinancialMeansUpdated.class);

        if (isNotBlank(defendantFinancialMeansUpdated.getNationalInsuranceNumber())) {
            handleDefendantNiNumberUpdateCommand(defendantFinancialMeansUpdated, envelope);
        }

        handleDefendantFinancialMeansUpdateCommand(defendantFinancialMeansUpdated, envelope);
    }

    private void handleDefendantNiNumberUpdateCommand(
            final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated,
            final JsonEnvelope envelope) {
        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANT_NI_NUMBER).build(),
                createObjectBuilder()
                        .add(FIELD_NI_NUMBER, defendantFinancialMeansUpdated.getNationalInsuranceNumber())
                        .add("caseId", defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getCaseId().toString())
                        .add("defendantId", defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getDefendantId().toString())
                        .build()));
    }

    private void handleDefendantFinancialMeansUpdateCommand(
            final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated,
            final JsonEnvelope envelope) {
        final FinancialMeans.Builder financialMeansBuilder = FinancialMeans.financialMeans();

        financialMeansBuilder.withIncome(getIncome(defendantFinancialMeansUpdated));

        if (nonNull(defendantFinancialMeansUpdated.getBenefits()) && nonNull(defendantFinancialMeansUpdated.getBenefits().getClaimed())) {
            financialMeansBuilder.withBenefits(defendantFinancialMeansUpdated.getBenefits());
        } else {
            financialMeansBuilder.withBenefits(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getBenefits());
        }

        if (nonNull(defendantFinancialMeansUpdated.getEmployer()) && nonNull(defendantFinancialMeansUpdated.getEmployer().getName())) {
            financialMeansBuilder.withEmployer(defendantFinancialMeansUpdated.getEmployer());
        } else {
            financialMeansBuilder.withEmployer(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getEmployer());
        }

        if (nonNull(defendantFinancialMeansUpdated.getEmployment()) && nonNull(defendantFinancialMeansUpdated.getEmployment().getStatus())) {
            financialMeansBuilder.withEmployment(defendantFinancialMeansUpdated.getEmployment());
        } else {
            financialMeansBuilder.withEmployment(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getEmployment());
        }

        FinancialMeans financialMeans = financialMeansBuilder
                .withCaseId(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getCaseId())
                .withDefendantId(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getDefendantId())
                .build();

        if (!isNull(financialMeans.getEmployer()) && !isNull(financialMeans.getEmployer().getAddress()) && !isUkGovPostCodeValidWithSpace(financialMeans.getEmployer().getAddress().getPostcode())) {
            String validPostCode = fixPostCodeSpacing(financialMeans.getEmployer().getAddress().getPostcode());
            final FinancialMeans.Builder financialMeansBuilderUpdate = FinancialMeans.financialMeans();
            financialMeans = financialMeansBuilderUpdate.withValuesFrom(financialMeans)
                    .withEmployer(Employer.employer().withValuesFrom(financialMeans.getEmployer())
                            .withAddress(Address.address().withValuesFrom(financialMeans.getEmployer().getAddress())
                                    .withPostcode(validPostCode).build()).build()).build();
        }

        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_SJP_UPDATE_DEFENDANT_FINANCIAL_MEANS).build(),
                financialMeans));
    }

    private Income getIncome(final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated) {
        if (nonNull(defendantFinancialMeansUpdated.getIncome()) && nonNull(defendantFinancialMeansUpdated.getIncome().getAmount())) {
            return new Income(defendantFinancialMeansUpdated.getIncome().getAmount(), defendantFinancialMeansUpdated.getIncome().getFrequency());
        } else if (nonNull(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getIncome()) && nonNull(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getIncome().getAmount())) {
            return new Income(defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getIncome().getAmount(),
                    defendantFinancialMeansUpdated.getSjpAllFinancialMeans().getIncome().getFrequency());
        }
        return new Income(BigDecimal.ZERO, Frequency.WEEKLY);
    }
}
