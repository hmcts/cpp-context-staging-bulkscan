package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.command.UpdateDefendantDetailsApi;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.Title;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdateRequested;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;
import java.util.function.Function;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantDetailsEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDetailsEventProcessor.class);

    private static final String EVENT_RECEIVED_LOG_TEMPLATE = "{} event received {}";

    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";

    private static final String FIELD_SCAN_ENVELOPE_ID = "scanEnvelopeId";

    private static final String FIELD_CASE_URN = "caseUrn";

    private static final String COMMAND_UPDATE_DEFENDANT_ADDITIONAL_DETAILS = "stagingbulkscan.command.update-defendant-additional-details";

    @Inject
    private Sender sender;

    @Inject
    private SJPService sjpService;

    @Inject
    private JsonObjectToObjectConverter converter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles(DefendantDetailsUpdateRequested.EVENT_NAME)
    public void defendantDetailsUpdateRequested(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final UUID envelopeId = fromString(jsonObject.getString(FIELD_SCAN_ENVELOPE_ID));
        final UUID scanDocumentId = fromString(jsonObject.getString(FIELD_SCAN_DOCUMENT_ID));

        final String caseUrn = jsonObject.getString(FIELD_CASE_URN);

        final Defendant defendant = sjpService.getDefendantDetailsByCaseUrn(caseUrn, envelope, envelopeId, scanDocumentId);

        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANT_ADDITIONAL_DETAILS).build(), defendant));
    }

    @Handles(DefendantDetailsUpdated.EVENT_NAME)
    public void updateDefendantAdditionalDetails(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_RECEIVED_LOG_TEMPLATE, DefendantDetailsUpdated.EVENT_NAME, envelope.toObfuscatedDebugString());
        }

        final DefendantDetailsUpdated defendantDetails = converter.convert((JsonObject) envelope.payload(), DefendantDetailsUpdated.class);

        final Address addressApi = getAddress(defendantDetails);

        final ContactDetails contactDetailsApi = getContactDetails(defendantDetails);

        final Gender genderApi = Gender.valueFor(defendantDetails.getGender().toString()).orElse(null);

        final UpdateDefendantDetailsApi defendantDetailsToUpdate = UpdateDefendantDetailsApi.updateDefendantDetailsApi()
                .withAddress(addressApi)
                .withContactNumber(contactDetailsApi)
                .withDateOfBirth(defendantDetails.getDateOfBirth())
                .withEmail(defendantDetails.getEmail())
                .withFirstName(defendantDetails.getFirstName())
                .withGender(genderApi)
                .withLastName(defendantDetails.getLastName())
                .withNationalInsuranceNumber(defendantDetails.getNationalInsuranceNumber())
                .withTitle(ofNullable(defendantDetails.getTitle()).map(Title::toString).orElse(null))
                .withDriverNumber(defendantDetails.getDriverNumber())
                .build();

        final JsonObject defendantPayload = this.objectToJsonObjectConverter.convert(defendantDetailsToUpdate);

        final JsonObject payload = JsonObjects.createObjectBuilder(defendantPayload)
                .add("caseId", defendantDetails.getCaseId().toString())
                .add("defendantId", defendantDetails.getId().toString())
                .build();

        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.update-defendant-details").build(), payload));
    }

    @Handles(PleaDetailsUpdated.EVENT_NAME)
    public void updateDefendantPlea(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_RECEIVED_LOG_TEMPLATE, PleaDetailsUpdated.EVENT_NAME, envelope.toObfuscatedDebugString());
        }

        if (!sjpService.caseHasOffencesWithVerdicts(envelope)) {
            final JsonObject jsonPayload = sjpService.updateDefendantPlea(envelope);
            sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.set-pleas").build(), jsonPayload));
        }
    }

    private Address getAddress(final DefendantDetailsUpdated defendantDetails) {
        final Function<uk.gov.justice.stagingbulkscan.domain.Address, Address> addressMapper = address -> Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withPostcode(address.getPostcode())
                .build();

        return addressMapper.apply(defendantDetails.getAddress());
    }

    private ContactDetails getContactDetails(final DefendantDetailsUpdated defendantDetails) {
        final Function<uk.gov.justice.stagingbulkscan.domain.ContactDetails, ContactDetails> contactDetailsMapper = contactDetails -> ContactDetails.contactDetails()
                .withBusiness(contactDetails.getBusiness())
                .withEmail(contactDetails.getEmail())
                .withEmail2(contactDetails.getEmail2())
                .withHome(contactDetails.getHome())
                .withMobile(contactDetails.getMobile())
                .build();

        return contactDetailsMapper.apply(defendantDetails.getContactDetails());
    }
}
