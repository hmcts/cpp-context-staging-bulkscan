package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.json.schemas.domains.sjp.command.UpdateDefendantDetailsApi;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.Gender;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.justice.stagingbulkscan.domain.Title;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantCourtOptions;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantPlea;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DisabilityNeeds;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.Interpreter;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

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
public class DefendantDetailsEventProcessorTest {

    private static final String DEFENDANT_DETAILS_UPDATED_EVENT = "stagingbulkscan.events.defendant-details-updated";
    private static final String DEFENDANT_PLEA_DETAILS_UPDATED_EVENT = "stagingbulkscan.events.plea-details-updated";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    private DefendantDetailsEventProcessor defendantDetailsEventProcessor;

    @Mock
    private SJPService sjpService;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleDefendantDetailsUpdateRequested() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID scanEnvelopeId = UUID.randomUUID();
        final String caseUrn = "TVL987654321";
        final DefendantDetailsUpdateRequested defendantDetailsUpdateRequested = new DefendantDetailsUpdateRequested(scanEnvelopeId, scanDocumentId, caseUrn);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DefendantDetailsUpdateRequested.EVENT_NAME),
                this.objectToJsonObjectConverter.convert(defendantDetailsUpdateRequested));
        final Defendant defendant = Defendant.defendant().build();
        when(sjpService.getDefendantDetailsByCaseUrn(caseUrn, event, scanEnvelopeId, scanDocumentId)).thenReturn(defendant);
        defendantDetailsEventProcessor.defendantDetailsUpdateRequested(event);
        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final Defendant defendantMatcher = (Defendant) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(defendant, is(defendantMatcher));
    }

    @Test
    public void shouldHandleDefendantDetailsUpdateCommand() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID envelopeId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final ContactDetails contactDetails = ContactDetails.contactDetails()
                .withEmail("jsmith@hmcts.net")
                .withMobile("0790418411")
                .build();

        final Address address = Address.address().withAddress1("line 1").withAddress2("line2").withAddress3("line3").build();

        final DefendantDetailsUpdated.Builder builder = new DefendantDetailsUpdated.Builder();
        builder.withScanDocumentId(scanDocumentId)
                .withCaseId(caseId)
                .withScanEnvelopeId(envelopeId)
                .withId(defendantId)
                .withTitle(Title.MR).withFirstName("john").withLastName("Smith").withDateOfBirth("01/01/2010")
                .withGender(Gender.MALE)
                .withEmail("jsmith@hmcts.net")
                .withNationalInsuranceNumber("ABC1212121").withContactDetails(contactDetails)
                .withAddress(address);
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(builder.build()));

        defendantDetailsEventProcessor.updateDefendantAdditionalDetails(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final JsonObject jsonObject = (JsonObject) this.envelopeArgumentCaptor.getValue().payload();
        final UpdateDefendantDetailsApi updateDefendantDetailsApi = jsonObjectToObjectConverter.convert(jsonObject, UpdateDefendantDetailsApi.class);
        assertThat(updateDefendantDetailsApi.getAddress().getAddress1(), is(address.getAddress1()));
        assertThat(updateDefendantDetailsApi.getEmail(), is(contactDetails.getEmail()));
        assertThat(updateDefendantDetailsApi.getContactNumber().getMobile(), is(contactDetails.getMobile()));
    }

    @Test
    public void shouldHandleDefendantPleaUpdateCommand() {
        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final DefendantCourtOptions defendantCourtOptions =
                new DefendantCourtOptions(false, new Interpreter("english", false), new DisabilityNeeds("none", false));

        List<DefendantPlea> defendantPleas = new ArrayList<>();
        defendantPleas.add(new DefendantPlea(caseId, offenceId, PleaType.GUILTY, false));

        final PleaDetailsUpdated pleaDetailsUpdated = new PleaDetailsUpdated(caseId, defendantCourtOptions, defendantPleas);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(DEFENDANT_PLEA_DETAILS_UPDATED_EVENT),
                this.objectToJsonObjectConverter.convert(pleaDetailsUpdated));

        final JsonObject fakePayload = JsonObjects.createObjectBuilder().add("caseId", caseId.toString()).build();

        when(sjpService.updateDefendantPlea(event)).thenReturn(fakePayload);

        defendantDetailsEventProcessor.updateDefendantPlea(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        final JsonObject jsonObject = (JsonObject) this.envelopeArgumentCaptor.getValue().payload();
        assertThat(jsonObject, is(fakePayload));
    }

}
