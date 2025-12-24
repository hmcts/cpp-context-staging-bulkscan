package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Benefits;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.Employer;
import uk.gov.justice.stagingbulkscan.domain.Employment;
import uk.gov.justice.stagingbulkscan.domain.EmploymentStatus;
import uk.gov.justice.stagingbulkscan.domain.FinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Frequency;
import uk.gov.justice.stagingbulkscan.domain.Income;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelopeDocument;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdated;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefendantFinancialMeansProcessorTest {
    private static final String DEFENDANT_FINANCIAL_MEANS_UPDATED_EVENT = "stagingbulkscan.events.defendant-financial-means-updated";

    @Mock
    private Sender sender;

    @Mock
    private SJPService sjpService;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    private UpdateDefendantFinancialMeansProcessor updateDefendantFinancialMeansProcessor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleDefendantFinancialMeansUpdateRequested() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String caseUrn = "TVL12344567";
        final UUID caseId = UUID.randomUUID();
        final DefendantFinancialMeansUpdateRequested defendantFinancialMeansUpdateRequested = new DefendantFinancialMeansUpdateRequested(scanEnvelopeId, scanDocumentId, caseUrn);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DefendantFinancialMeansUpdateRequested.EVENT_NAME),
                this.objectToJsonObjectConverter.convert(defendantFinancialMeansUpdateRequested));
        final JsonObject sjpCaseJson = Json.createObjectBuilder().add("id", caseId.toString())
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .add("scanDocumentId", scanDocumentId.toString())
                .add("defendant", Json.createObjectBuilder().add("id", defendantId.toString())
                        .add("personalDetails", Json.createObjectBuilder().build())
                ).build();
        when(sjpService.getCaseDetails(caseUrn, event)).thenReturn(sjpCaseJson);
        final FinancialMeans.Builder fiBuilder = new FinancialMeans.Builder();
        fiBuilder.withBenefits(new Benefits(true, "HOUSING"));
        fiBuilder.withEmployer(new Employer(Address.address().build(), "EMP123", "My company", "9876543"));
        fiBuilder.withEmployment(new Employment("am employed", EmploymentStatus.EMPLOYED));
        fiBuilder.withIncome(new Income(new BigDecimal(1234), Frequency.MONTHLY));
        when(sjpService.getDefendantFinancialMeans(defendantId, event)).thenReturn(fiBuilder.build());
        updateDefendantFinancialMeansProcessor.defendantFinancialMeansUpdateRequested(event);
        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final AllFinancialMeans financialMeans = (AllFinancialMeans) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(financialMeans.getCaseId(), is(caseId));
        assertThat(financialMeans.getDefendantId(), is(defendantId));
        assertTrue(financialMeans.getBenefits().getClaimed());
        assertThat(financialMeans.getEmployment().getStatus(), is(EmploymentStatus.EMPLOYED));
        assertThat(financialMeans.getScanEnvelopeId(), is(scanEnvelopeId));
        assertThat(financialMeans.getScanDocumentId(), is(scanDocumentId));
    }

    @Test
    public void shouldHandleSjpFinancialMeansUpdatedCommand() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final Employer employer = Employer.employer().withName("abc").withAddress(Address.address().withAddress1("address 1").withPostcode("EC4Y8EN").build()).build();
        final Income income = Income.income().withAmount(new BigDecimal("1234")).build();
        final Benefits benefits = Benefits.benefits().withClaimed(true).build();
        final Employment employment = Employment.employment().withStatus(EmploymentStatus.EMPLOYED).build();
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = new DefendantFinancialMeansUpdated(scanDocumentId, employer, benefits, employment, income, "ABC1234CB", allFinancialMeans);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_FINANCIAL_MEANS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(defendantFinancialMeansUpdated));

        updateDefendantFinancialMeansProcessor.handleDefendantFinancialMeansUpdatedEvent(event);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final FinancialMeans financialMeans = (FinancialMeans) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(financialMeans.getCaseId(), is(caseId));
        assertThat(financialMeans.getDefendantId(), is(defendantId));
        assertTrue(financialMeans.getBenefits().getClaimed());
        assertThat(financialMeans.getEmployment().getStatus(), is(EmploymentStatus.EMPLOYED));
        assertThat(financialMeans.getEmployer().getAddress().getPostcode(), is("EC4Y 8EN"));
    }

    @Test
    public void shouldHandleSjpFinancialMeansUpdatedCommandWithSjpIncome() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final Employer employer = Employer.employer().withName("abc").withAddress(Address.address().withAddress1("address 1").withAddress2("address 2").build()).build();
        final Income income = Income.income().build();
        final Benefits benefits = Benefits.benefits().withClaimed(true).build();
        final Employment employment = Employment.employment().withStatus(EmploymentStatus.UNEMPLOYED).build();
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withCaseId(caseId)
                .withDefendantId(defendantId).withIncome(new Income(BigDecimal.valueOf(250), Frequency.MONTHLY))
                .build();

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = new DefendantFinancialMeansUpdated(scanDocumentId, employer, benefits, employment, income, "ABC1234CB", allFinancialMeans);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_FINANCIAL_MEANS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(defendantFinancialMeansUpdated));

        updateDefendantFinancialMeansProcessor.handleDefendantFinancialMeansUpdatedEvent(event);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final FinancialMeans financialMeans = (FinancialMeans) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(financialMeans.getCaseId(), is(caseId));
        assertThat(financialMeans.getDefendantId(), is(defendantId));
        assertTrue(financialMeans.getBenefits().getClaimed());
        assertThat(financialMeans.getEmployment().getStatus(), is(EmploymentStatus.UNEMPLOYED));
        assertThat(financialMeans.getIncome().getAmount(), is(BigDecimal.valueOf(250)));
        assertThat(financialMeans.getIncome().getFrequency(), is(Frequency.MONTHLY));
        assertThat(financialMeans.getEmployer().getAddress().getPostcode(), is(nullValue()));
    }

    @Test
    public void shouldHandleSjpFinancialMeansUpdatedCommandWithNoOverallIncome() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final Employer employer = Employer.employer().withName("abc").withAddress(Address.address().withAddress1("Address 1").withPostcode("SW1X77HH").build()).build();
        final Income income = Income.income().build();
        final Benefits benefits = Benefits.benefits().withClaimed(true).build();
        final Employment employment = Employment.employment().withStatus(EmploymentStatus.UNEMPLOYED).build();
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = new DefendantFinancialMeansUpdated(scanDocumentId, employer, benefits, employment, income, "ABC1234CB", allFinancialMeans);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_FINANCIAL_MEANS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(defendantFinancialMeansUpdated));

        updateDefendantFinancialMeansProcessor.handleDefendantFinancialMeansUpdatedEvent(event);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final FinancialMeans financialMeans = (FinancialMeans) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(financialMeans.getEmployment().getStatus(), is(EmploymentStatus.UNEMPLOYED));
        assertThat(financialMeans.getIncome().getAmount(), is(BigDecimal.ZERO));
        assertThat(financialMeans.getIncome().getFrequency(), is(Frequency.WEEKLY));
        assertThat(financialMeans.getEmployer().getAddress().getPostcode(), is(""));
    }

    @Test
    public void shouldHandleSjpFinancialMeansUpdateddWithNoOverallIncome() throws IOException {

        final DefendantFinancialMeansUpdated defendantFinancialMeansUpdated = object("stagingbulkscan.events.defendant-financial-means-updated.json", DefendantFinancialMeansUpdated.class);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_FINANCIAL_MEANS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(defendantFinancialMeansUpdated));

        updateDefendantFinancialMeansProcessor.handleDefendantFinancialMeansUpdatedEvent(event);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final FinancialMeans financialMeans = (FinancialMeans) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(financialMeans.getIncome().getAmount(), is(BigDecimal.ZERO));
        assertThat(financialMeans.getIncome().getFrequency(), is(Frequency.WEEKLY));
    }

    private <T> T object(final String fileName, final Class<T> clz) throws IOException {
        final File file = new File(ClassLoader.getSystemClassLoader().getResource(fileName).getFile());
        final String eventPayloadString = new String(Files.readAllBytes(file.toPath()));
        return jsonObjectToObjectConverter.convert(new StringToJsonObjectConverter().convert(eventPayloadString), clz);
    }
}
