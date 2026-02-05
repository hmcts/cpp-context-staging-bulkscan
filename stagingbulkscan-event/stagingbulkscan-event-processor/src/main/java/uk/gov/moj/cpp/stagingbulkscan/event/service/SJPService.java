package uk.gov.moj.cpp.stagingbulkscan.event.service;


import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleaType.GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleaType.NOT_GUILTY;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.json.schemas.domains.sjp.command.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.command.SetPleas;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.DisabilityNeeds;
import uk.gov.justice.stagingbulkscan.domain.FinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.Gender;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Title;
import uk.gov.moj.cpp.core.sjp.DefendantCourtOptions;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SJPService {
    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter converter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Sender sender;

    private static final String CASE_ID = "caseId";

    public JsonObject getCaseDetails(final String caseUrn, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("urn", caseUrn).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case-by-urn"), payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload();
    }

    public JsonObject getCaseResults(final String caseId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.case-results"), payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload();
    }

    public Defendant getDefendantDetailsByCaseUrn(final String caseUrn, final JsonEnvelope envelope, final UUID envelopeId, final UUID scanDocumentId) {

        final JsonObject caseDetails = getCaseDetails(caseUrn, envelope);
        final JsonObject caseDetailsPayload = getFullCaseDetails(UUID.fromString(caseDetails.getString("id")), envelope);

        final JsonObject defendantPayload = caseDetailsPayload.getJsonObject("defendant");
        final JsonObject personalDetailsPayload = defendantPayload.getJsonObject("personalDetails");
        final JsonObject addressPayload = personalDetailsPayload.getJsonObject("address");
        final JsonObject contactDetailsPayload = personalDetailsPayload.getJsonObject("contactDetails");

        final Address defendantAddress = of(converter.convert(addressPayload, Address.class)).orElse(null);
        final ContactDetails contactDetails = of(converter.convert(contactDetailsPayload, ContactDetails.class)).orElse(null);
        final Title title = Title.valueFor(personalDetailsPayload.getString("title", null)).orElse(null);
        final Gender gender = Gender.valueFor(personalDetailsPayload.getString("gender")).orElse(null);

        final String email = of(contactDetails).map(ContactDetails::getEmail).orElse(null);

        final Defendant.Builder defendantBuilder = Defendant.defendant();
        defendantBuilder
                .withCaseId(fromString(caseDetailsPayload.getString("id")))
                .withId(fromString(defendantPayload.getString("id")))
                .withTitle(title)
                .withFirstName(personalDetailsPayload.getString("firstName"))
                .withLastName(personalDetailsPayload.getString("lastName"))
                .withDateOfBirth(personalDetailsPayload.getString("dateOfBirth", null))
                .withGender(gender)
                .withEmail(email)
                .withAddress(defendantAddress)
                .withContactDetails(contactDetails)
                .withScanEnvelopeId(envelopeId)
                .withScanDocumentId(scanDocumentId)
                .withNationalInsuranceNumber(personalDetailsPayload.getString("nationalInsuranceNumber", null))
                .withDriverNumber(personalDetailsPayload.getString("driverNumber", null))
                .withPlea(buildPlea(defendantPayload.getJsonArray("offences"),
                        defendantPayload.getJsonObject("interpreter"),
                        defendantPayload.getBoolean("speakWelsh", false),
                        defendantPayload.getJsonObject("disabilityNeeds")));

        return defendantBuilder.build();
    }

    private uk.gov.justice.stagingbulkscan.domain.Plea buildPlea(final JsonArray offences, final JsonObject interpreter, final boolean speakWelsh, final JsonObject disabilityNeeds) {
        final uk.gov.justice.stagingbulkscan.domain.Plea.Builder builder = new uk.gov.justice.stagingbulkscan.domain.Plea.Builder();
        ofNullable(interpreter).ifPresent(i -> builder.withInterpreter(new uk.gov.justice.stagingbulkscan.domain.Interpreter(i.getString("language", null),
                i.getBoolean("needed"))));
        builder.withWelshHearing(speakWelsh);
        ofNullable(disabilityNeeds).ifPresent(i-> builder.withDisabilityNeeds(new DisabilityNeeds(i.getString("disabilityNeeds", null), i.getBoolean("needed"))));
        final List<Offence> offenceList = buildOffences(offences);

        final boolean wishToComeToCourt = (offences.getValuesAs(JsonObject.class).stream()
                .anyMatch(offence -> offence.containsKey("plea") && ofNullable(offence.getString("plea"))
                        .map(o -> o.equals(GUILTY_REQUEST_HEARING.name())).isPresent()));

        builder.withWishToComeToCourt(wishToComeToCourt);

        builder.withOffences(offenceList);
        return builder.build();
    }

    private boolean offenceHasVerdict(final JsonObject offence) {
        return offence.containsKey("verdict") && !"NO_VERDICT".equals(offence.getString("verdict"));
    }

    private boolean caseDecisionHasVerdict(final JsonObject caseDecision) {

        final JsonArray offences = caseDecision.getJsonArray("offences");

        for (final JsonValue offence : offences) {
            if (offenceHasVerdict((JsonObject) offence)) {
                return true;
            }
        }

        return false;
    }

    public boolean caseHasOffencesWithVerdicts(final JsonEnvelope envelope) {
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject results = getCaseResults(caseId, envelope);
        final JsonArray caseDecisions = results != null ? results.getJsonArray("caseDecisions"): createArrayBuilder().build();

        for (final JsonValue caseDecision : caseDecisions) {
            if (caseDecisionHasVerdict((JsonObject) caseDecision)) {
                return true;
            }
        }

        return false;
    }

    public JsonObject updateDefendantPlea(final JsonEnvelope envelope) {
        final JsonObject jsonObject = envelope.payloadAsJsonObject();
        final JsonObject inputDefendantCourtOptions = jsonObject.getJsonObject("defendantCourtOptions");
        final JsonObject inputInterpreter = inputDefendantCourtOptions.getJsonObject("interpreter");
        final Interpreter interpreter = new Interpreter(inputInterpreter.getString("language", null),
                inputInterpreter.getBoolean("needed"));
        final JsonObject inputDisabilityNeeds = inputDefendantCourtOptions.getJsonObject("disabilityNeeds");
        final uk.gov.moj.cpp.core.sjp.DisabilityNeeds disabilityNeeds = new uk.gov.moj.cpp.core.sjp.DisabilityNeeds(inputDisabilityNeeds.getString("disabilityNeeds", null),
                inputDisabilityNeeds.getBoolean("needed"));
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(disabilityNeeds, interpreter,
                inputDefendantCourtOptions.getBoolean("welshHearing"));
        final JsonArray inputDefendantPleas = jsonObject.getJsonArray("defendantPleas");
        final List<Plea> pleas = new ArrayList<>();
        inputDefendantPleas.getValuesAs(JsonObject.class).forEach(inputPlea ->
                pleas.add(new Plea(UUID.fromString(inputPlea.getString("defendantId")),
                        null,
                        null,
                        UUID.fromString(inputPlea.getString("offenceId")),
                        mapToSjpPleaType(uk.gov.justice.stagingbulkscan.domain.PleaType.valueOf(inputPlea.getString("pleaType")),
                                inputPlea.getBoolean("wishToComeToCourt")))
                )
        );

        final SetPleas setPleas = new SetPleas(defendantCourtOptions, pleas);
        final JsonObject payload = objectToJsonObjectConverter.convert(setPleas);
        return createObjectBuilder(payload).add(CASE_ID, jsonObject.getString(CASE_ID)).build();
    }

    private List<Offence> buildOffences(final JsonArray offences) {
        return offences.getValuesAs(JsonObject.class).stream()
                .map(offence -> {
                    final Offence.Builder builder = new Offence.Builder()
                            .withId(UUID.fromString(offence.getString("id")))
                            .withTitle(offence.getString("title"))
                            .withHasFinalDecision(offence.getBoolean("hasFinalDecision"));
                    if (offence.containsKey("plea")) {
                        final Optional<PleaType> pleaTypeOptional = ofNullable(offence.getString("plea")).map(PleaType::valueOf);
                        pleaTypeOptional.flatMap(this::mapPleaType).ifPresent(builder::withPleaValue);
                    }
                    return builder.build();
                }).collect(Collectors.toList());
    }

    private Optional<uk.gov.justice.stagingbulkscan.domain.PleaType> mapPleaType(final PleaType pleaType) {
        if (pleaType.equals(GUILTY_REQUEST_HEARING) || pleaType.equals(GUILTY)) {
            return Optional.of(uk.gov.justice.stagingbulkscan.domain.PleaType.GUILTY);
        }
        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            return Optional.of(uk.gov.justice.stagingbulkscan.domain.PleaType.NOT_GUILTY);
        }
        return Optional.empty();
    }

    private PleaType mapToSjpPleaType(final uk.gov.justice.stagingbulkscan.domain.PleaType pleaType, final Boolean wishToComeToCourt) {
        if (pleaType.equals(uk.gov.justice.stagingbulkscan.domain.PleaType.GUILTY)) {
            if (wishToComeToCourt) {
                return GUILTY_REQUEST_HEARING;
            } else {
                return GUILTY;
            }
        }
        return NOT_GUILTY;
    }

    public FinancialMeans getDefendantFinancialMeans(final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("defendantId", defendantId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.all-financial-means").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);

        return converter.convert(response.payloadAsJsonObject(), FinancialMeans.class);
    }

    public JsonObject getFullCaseDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.case").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

}
