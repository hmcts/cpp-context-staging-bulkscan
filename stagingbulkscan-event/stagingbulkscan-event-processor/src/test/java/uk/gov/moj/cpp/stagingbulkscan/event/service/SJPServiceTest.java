package uk.gov.moj.cpp.stagingbulkscan.event.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.FinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantCourtOptions;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantPlea;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DisabilityNeeds;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.Interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class SJPServiceTest {
    private static final String CASE_BY_URN_QUERY_NAME = "sjp.query.case-by-urn";
    private static final String CASE_QUERY_NAME = "sjp.query.case";
    private static final String CASE_RESULTS_QUERY_NAME = "sjp.query.case-results";
    private static final String FINANCIAL_MEANS_QUERY_NAME = "sjp.query.all-financial-means";    //
    private static final String DEFENDANT_DETAILS_UPDATED_EVENT = "stagingbulkscan.events.defendant-details-updated";
    private static final UUID SCAN_DOCUMENT_ID = randomUUID();
    private static final UUID SCAN_ENVELOPE_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private SJPService sjpService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldGetCaseDetailsByUrn() {
        final JsonObject responseCaseDetails = createObjectBuilder().add("id", CASE_ID.toString()).build();
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID(CASE_BY_URN_QUERY_NAME),
                responseCaseDetails);

        final String caseUrn = STRING.next();
        when(requestCaseByUrn()).thenReturn(responseEnvelope);

        final JsonObject caseDetails = sjpService.getCaseDetails(caseUrn, envelope);
        assertThat(caseDetails, is(responseCaseDetails));
    }

    @Test
    public void shouldGetCaseResults() {
        final JsonObject responseCaseResults = createObjectBuilder().add("caseId", CASE_ID.toString()).build();
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID(CASE_RESULTS_QUERY_NAME),
                responseCaseResults);

        when(requestCaseResults()).thenReturn(responseEnvelope);

        final JsonObject caseResults = sjpService.getCaseResults(STRING.next(), envelope);

        assertThat(caseResults, is(responseCaseResults));
    }

    @Test
    public void shouldGetDefendantDetailsByCaseUrn() {

        final String caseUrn = STRING.next();

        final JsonObject expectedCaseDetails = getFullCaseDetails();
        mockGetCaseByUrn(expectedCaseDetails);
        mockGetCaseById(expectedCaseDetails);

        final Defendant actualDefendantDetails = sjpService.getDefendantDetailsByCaseUrn(caseUrn, envelope, SCAN_ENVELOPE_ID, SCAN_DOCUMENT_ID);

        final JsonObject expectedContactDetails = expectedCaseDetails.getJsonObject("defendant").getJsonObject("personalDetails").getJsonObject("contactDetails");

        assertThat(expectedCaseDetails.getString("id"), is(actualDefendantDetails.getCaseId().toString()));
        assertThat(expectedCaseDetails.getJsonObject("defendant").getString("id"), is(actualDefendantDetails.getId().toString()));
        assertThat(expectedContactDetails.getString("email"), is(actualDefendantDetails.getEmail()));
        assertThat(expectedContactDetails.getString("mobile"), is(actualDefendantDetails.getContactDetails().getMobile()));
        assertThat(SCAN_ENVELOPE_ID, is(actualDefendantDetails.getScanEnvelopeId()));
        assertThat(SCAN_DOCUMENT_ID, is(actualDefendantDetails.getScanDocumentId()));
    }

    @Test
    public void shouldGetDefendantDetailsByCaseUrnForCaseWithMinimumFields() {

        final String caseUrn = STRING.next();

        final JsonObject expectedCaseDetails = getCaseWithMinimFields();
        mockGetCaseByUrn(expectedCaseDetails);
        mockGetCaseById(expectedCaseDetails);

        final Defendant actualDefendantDetails = sjpService.getDefendantDetailsByCaseUrn(caseUrn, envelope, SCAN_ENVELOPE_ID, SCAN_DOCUMENT_ID);

        final JsonObject expectedContactDetails = expectedCaseDetails.getJsonObject("defendant").getJsonObject("personalDetails").getJsonObject("contactDetails");

        assertThat(expectedCaseDetails.getString("id"), is(actualDefendantDetails.getCaseId().toString()));
        assertThat(expectedCaseDetails.getJsonObject("defendant").getString("id"), is(actualDefendantDetails.getId().toString()));
        assertThat(expectedContactDetails.containsKey("email"), is(false));
        assertThat(expectedContactDetails.containsKey("mobile"), is(false));
        assertThat(SCAN_ENVELOPE_ID, is(actualDefendantDetails.getScanEnvelopeId()));
        assertThat(SCAN_DOCUMENT_ID, is(actualDefendantDetails.getScanDocumentId()));
    }

    @Test
    public void shouldUpdateDefendantPleaWhenNoVerdicts() {

        final Map<String, String> offenceVerdicts = ImmutableMap.of(
                UUID.randomUUID().toString(), "NO_VERDICT",
                UUID.randomUUID().toString(), "NO_VERDICT");

        final DefendantCourtOptions defendantCourtOptions =
                new DefendantCourtOptions(false, new Interpreter("english", false), new DisabilityNeeds("none", false));

        List<DefendantPlea> defendantPleas = new ArrayList<>();
        for (final String offenceId : offenceVerdicts.keySet()) {
            defendantPleas.add(new DefendantPlea(CASE_ID, UUID.fromString(offenceId), PleaType.NOT_GUILTY, false));
        }

        final PleaDetailsUpdated pleaDetailsUpdated = new PleaDetailsUpdated(CASE_ID, defendantCourtOptions, defendantPleas);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(pleaDetailsUpdated));

        final JsonObject responseCaseResults = getFakeCaseResults(offenceVerdicts);

        final JsonObject caseDetails = sjpService.updateDefendantPlea(envelope);

        assertThat(caseDetails.getString("caseId"), is(CASE_ID.toString()));
        assertThat(caseDetails.getJsonArray("pleas").size(), is(2));
        assertThat(caseDetails.getJsonObject("defendantCourtOptions").getBoolean("welshHearing"), is(defendantCourtOptions.isWelshHearing()));
    }

    @Test
    public void shouldIdentifyWhenACaseHasAtLeastOneVerdict() {

        final Map<String, String> offenceVerdicts = ImmutableMap.of(
                UUID.randomUUID().toString(), "PROVEN_SJP",
                UUID.randomUUID().toString(), "NO_ENTRY",
                UUID.randomUUID().toString(), "NO_VERDICT");

        final DefendantCourtOptions defendantCourtOptions =
                new DefendantCourtOptions(false, new Interpreter("english", false), new DisabilityNeeds("none", false));

        List<DefendantPlea> defendantPleas = new ArrayList<>();
        for (final String offenceId : offenceVerdicts.keySet()) {
            defendantPleas.add(new DefendantPlea(CASE_ID, UUID.fromString(offenceId), PleaType.NOT_GUILTY, false));
        }

        final PleaDetailsUpdated pleaDetailsUpdated = new PleaDetailsUpdated(CASE_ID, defendantCourtOptions, defendantPleas);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(pleaDetailsUpdated));

        final JsonObject responseCaseResults = getFakeCaseResults(offenceVerdicts);

        final Envelope<JsonObject> responseEnvelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(CASE_RESULTS_QUERY_NAME),
                responseCaseResults);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        assertThat(sjpService.caseHasOffencesWithVerdicts(envelope), is(true));
    }

    @Test
    public void shouldIdentifyWhenACaseHasNoVerdicts() {

        final Map<String, String> offenceVerdicts = ImmutableMap.of(
                UUID.randomUUID().toString(), "NO_ENTRY",
                UUID.randomUUID().toString(), "NO_VERDICT");

        final DefendantCourtOptions defendantCourtOptions =
                new DefendantCourtOptions(false, new Interpreter("english", false), new DisabilityNeeds("none", false));

        List<DefendantPlea> defendantPleas = new ArrayList<>();
        for (final String offenceId : offenceVerdicts.keySet()) {
            defendantPleas.add(new DefendantPlea(CASE_ID, UUID.fromString(offenceId), PleaType.NOT_GUILTY, false));
        }

        final PleaDetailsUpdated pleaDetailsUpdated = new PleaDetailsUpdated(CASE_ID, defendantCourtOptions, defendantPleas);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(pleaDetailsUpdated));

        final JsonObject responseCaseResults = getFakeCaseResults(offenceVerdicts);

        final Envelope<JsonObject> responseEnvelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(CASE_RESULTS_QUERY_NAME),
                responseCaseResults);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        assertThat(sjpService.caseHasOffencesWithVerdicts(envelope), is(false));
    }

    @Test
    public void shouldGetDefendantFinancialMeans() {

        final UUID defendantId = randomUUID();

        final JsonObject response = createObjectBuilder().add("defendantId", defendantId.toString()).build();
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID(FINANCIAL_MEANS_QUERY_NAME),
                response);

        when(requestFinancialMeans()).thenReturn(responseEnvelope);

        final FinancialMeans financialMeans = sjpService.getDefendantFinancialMeans(defendantId, envelope);
        assertThat(financialMeans.getDefendantId(), is(defendantId));
    }

    private void mockGetCaseByUrn(final JsonObject expectedCaseDetails) {
        final JsonEnvelope responseEnvelopeByUrn = envelopeFrom(
                metadataWithRandomUUID(CASE_BY_URN_QUERY_NAME),
                expectedCaseDetails);
        when(requestCaseByUrn()).thenReturn(responseEnvelopeByUrn);
    }

    private JsonObject getFullCaseDetails() {

        final JsonObject address = createObjectBuilder()
                .add("address 1", "line 1")
                .build();

        final JsonObjectBuilder contactDetailsBuilder = createObjectBuilder()
                .add("email", "jsmith@hmcts.com")
                .add("mobile", "07901111111");

        final JsonObjectBuilder personalDetailsBuilder = createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Smith")
                .add("gender", "Male")
                .add("address", address)
                .add("title", "Mr")
                .add("dateOfBirth", "01/01/2000")
                .add("contactDetails", contactDetailsBuilder.build());

        final JsonObject defendantDetails = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("personalDetails", personalDetailsBuilder.build())
                .add("offences", createArrayBuilder().build())
                .build();

        return createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("defendant", defendantDetails)
                .build();

    }

    private JsonObject getCaseWithMinimFields() {

        final JsonObject address = createObjectBuilder()
                .add("address 1", "line 1")
                .build();

        final JsonObjectBuilder contactDetailsBuilder = createObjectBuilder();

        final JsonObjectBuilder personalDetailsBuilder = createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Smith")
                .add("gender", "Male")
                .add("address", address)
                .add("contactDetails", contactDetailsBuilder.build());

        final JsonObject defendantDetails = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("personalDetails", personalDetailsBuilder.build())
                .add("offences", createArrayBuilder().build())
                .build();

        return createObjectBuilder()
                .add("id", CASE_ID.toString())
                .add("defendant", defendantDetails)
                .build();

    }

    private JsonObject getFakeCaseResults(final Map<String, String> offenceVerdicts) {

        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder();

        offenceVerdicts.entrySet().forEach(offenceVerdict -> {
            final JsonObjectBuilder offenceBuilder = createObjectBuilder();
            offenceBuilder.add("id", offenceVerdict.getKey());
            if (!offenceVerdict.getValue().equals("NO_ENTRY")) {
                offenceBuilder.add("verdict", offenceVerdict.getValue());
            }
            offenceBuilder.add("results", createArrayBuilder().build());

            offenceArrayBuilder.add(offenceBuilder.build());
        });

        final JsonObject caseDecision = createObjectBuilder()
                .add("sjpSessionId", randomUUID().toString())
                .add("resultedOn", "2020-05-18T15:10:23.490Z[UTC]")
                .add("offences", offenceArrayBuilder.build())
                .build();

        final JsonArray caseDecisions = createArrayBuilder()
                .add(caseDecision)
                .build();

        return createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", "1111")
                .add("enforcingCourtCode", "2222")
                .add("caseDecisions", caseDecisions)
                .build();

    }

    private Object requestCaseByUrn() {
        return requester.requestAsAdmin(any(), eq(JsonObject.class));
    }

    private Object requestCaseDetails() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(CASE_QUERY_NAME),
                payloadIsJson(notNullValue()))));
    }

    private Object requestCaseResults() {
        return requester.requestAsAdmin(any(), eq(JsonObject.class));
    }

    private Object requestFinancialMeans() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName(FINANCIAL_MEANS_QUERY_NAME),
                payloadIsJson(notNullValue()))));
    }

    private void mockGetCaseById(JsonObject responseCaseDetails) {
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID(CASE_QUERY_NAME),
                responseCaseDetails);

        when(requestCaseDetails()).thenReturn(responseEnvelope);
    }

    private void mockGetCaseResults(JsonObject responseCaseResults) {
        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID(CASE_RESULTS_QUERY_NAME),
                responseCaseResults);

        when(requestCaseResults()).thenReturn(responseEnvelope);
    }
}
