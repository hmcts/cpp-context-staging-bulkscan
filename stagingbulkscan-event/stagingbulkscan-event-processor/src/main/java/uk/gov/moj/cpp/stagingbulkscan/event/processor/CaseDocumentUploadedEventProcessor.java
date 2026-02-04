package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.stagingbulkscan.command.RejectDocument.rejectDocument;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.command.ExpireDocument;
import uk.gov.justice.stagingbulkscan.command.RejectDocument;
import uk.gov.justice.stagingbulkscan.domain.AutoActionDocument;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.MaterialRejected;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;
import uk.gov.moj.cpp.stagingbulkscan.event.service.StagingBulkScanService;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SystemIdMapperService;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.ProblemValue;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialFollowup;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseDocumentUploadedEventProcessor {

    private static final String COMMAND_AUTO_ACTION_SCAN_DOCUMENT = "stagingbulkscan.command.auto-action-scan-document";

    private static final String PUBLIC_EVENT_SJP_CASE_DOCUMENT_ADDED = "public.sjp.case-document-added";

    private static final String PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";

    private static final String FIELD_SUBMISSION_ID = "submissionId";

    private static final String CASE_STATUS_REFERRED_FOR_COURT_HEARING = "REFERRED_FOR_COURT_HEARING";

    private static final String SCAN_DOCUMENT_ID = "scanDocumentId";

    private static final String SCAN_ENVELOPE_ID = "scanEnvelopeId";

    private static final String STAGINGBULKSCAN_COMMAND_RAISE_DOCUMENT_FOLLOW_UP = "stagingbulkscan.command.raise-document-follow-up";
    private static final String STAGINGBULKSCAN_COMMAND_DECIDE_DOCUMENT_NEXT_STEP = "stagingbulkscan.command.decide-document-next-step";

    @Inject
    private Sender sender;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private SJPService sjpService;

    @Inject
    private StagingBulkScanService stagingBulkScanService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles(PUBLIC_EVENT_SJP_CASE_DOCUMENT_ADDED)
    public void handleSjpCaseDocumentUploaded(final JsonEnvelope envelope) {
        processEvent(envelope, true);
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED)
    public void handleProgressionCourtDocumentAdded(final JsonEnvelope envelope) {
        processEvent(envelope, false);
    }

    @Handles("stagingbulkscan.events.document-next-step-decided")
    public void handleDocumentNextStepDecided(final JsonEnvelope envelope){
        final boolean isSjpCase = envelope.payloadAsJsonObject().getBoolean("isSjp");
        final String caseUrn = envelope.payloadAsJsonObject().getString("caseUrn");
        final String scanEnvelopeId = envelope.payloadAsJsonObject().getString(SCAN_ENVELOPE_ID);
        final String scanDocumentId = envelope.payloadAsJsonObject().getString(SCAN_DOCUMENT_ID);

        if (isSjpCase && isCaseCompletedAndNotReferredToCourt(envelope, caseUrn)) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(STAGINGBULKSCAN_COMMAND_RAISE_DOCUMENT_FOLLOW_UP).build(),
                    createObjectBuilder()
                            .add(SCAN_ENVELOPE_ID, scanEnvelopeId)
                            .add(SCAN_DOCUMENT_ID, scanDocumentId)
                            .build()));
        } else {
            sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_AUTO_ACTION_SCAN_DOCUMENT).build(), AutoActionDocument.autoActionDocument()
                    .withScanEnvelopeId(fromString(scanEnvelopeId))
                    .withScanDocumentId(fromString(scanDocumentId))
                    .withActionedBy(getUserId(envelope))
                    .build()));
        }

    }

    @Handles("public.prosecutioncasefile.material-rejected")
    public void handleMaterialRejected(final Envelope<MaterialRejected> envelope) {
        getSubmissionId(envelope).ifPresent(submissionId -> {
            final String scanDocumentReference = systemIdMapperService.getScanDocumentReferenceFor(fromString(submissionId));
            if (nonNull(scanDocumentReference)) {
                final RejectDocument rejectDocument = rejectDocument()
                        .withScanDocumentId(getScanDocumentId(scanDocumentReference))
                        .withScanEnvelopeId(getScanEnvelopeId(scanDocumentReference))
                        .withErrors(getProblems(envelope.payload().getErrors()))
                        .build();
                sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("stagingbulkscan.command.reject-document").build(), rejectDocument));
            }
        });
    }

    @Handles("public.prosecutioncasefile.bulkscan-material-followup")
    public void handleBulkScanMaterialRejected(final Envelope<BulkscanMaterialFollowup> envelope) {
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem> errors = envelope.payload().getErrors();
        getSubmissionId(envelope).ifPresent(submissionId -> {
                final String scanDocumentReference = systemIdMapperService.getScanDocumentReferenceFor(fromString(submissionId));
                if (nonNull(scanDocumentReference)) {
                    final ExpireDocument expireDocument = ExpireDocument.expireDocument()
                            .withScanDocumentId(getScanDocumentId(scanDocumentReference))
                            .withScanEnvelopeId(getScanEnvelopeId(scanDocumentReference))
                            .withErrors(getProblems(errors))
                            .withExpireDate(ZonedDateTime.now())
                            .build();
                    sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("stagingbulkscan.command.expire-document").build(), expireDocument));
                }
        });
    }

    private UUID getScanEnvelopeId(final String scanDocumentReference) {
        return fromString(scanDocumentReference.split(":")[0]);
    }

    private UUID getScanDocumentId(final String scanDocumentReference) {
        return fromString(scanDocumentReference.split(":")[1]);
    }

    private List<Problem> getProblems(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem> errors) {
        return errors.stream().map(error -> {
            final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue> errorValues = error.getValues();
            final List<ProblemValue> problemValues = errorValues.stream()
                    .map(errorValue -> ProblemValue.problemValue()
                            .withId(errorValue.getId())
                            .withKey(errorValue.getKey())
                            .withValue(errorValue.getValue()).build())
                    .collect(toList());
            return new Problem(error.getCode(), problemValues);
        }).collect(toList());
    }

    private UUID getUserId(final JsonEnvelope envelope) {
        return envelope.metadata().userId().map(UUID::fromString)
                .orElse(null);
    }

    private void processEvent(JsonEnvelope envelope, final boolean isSjpCase) {
        getSubmissionId(envelope).ifPresent(submissionId -> {
                final String scanDocumentReference = systemIdMapperService.getScanDocumentReferenceFor(fromString(submissionId));
                if (nonNull(scanDocumentReference)) {
                    final UUID scanEnvelopeId = getScanEnvelopeId(scanDocumentReference);
                    final UUID scanDocumentId = getScanDocumentId(scanDocumentReference);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(STAGINGBULKSCAN_COMMAND_DECIDE_DOCUMENT_NEXT_STEP).build(),
                            createObjectBuilder()
                                    .add(SCAN_ENVELOPE_ID, scanEnvelopeId.toString())
                                    .add(SCAN_DOCUMENT_ID, scanDocumentId.toString())
                                    .add("isSjp", isSjpCase)
                                    .build()));
                }
        });
    }

    private Optional<String> getSubmissionId(JsonEnvelope envelope) {
        final JsonObject metadata = envelope.metadata().asJsonObject();
        return getSubmissionId(metadata);
    }

    private Optional<String> getSubmissionId(final Envelope<?> envelope) {
        final JsonObject metadata = envelope.metadata().asJsonObject();
        return getSubmissionId(metadata);
    }

    private Optional<String> getSubmissionId(final JsonObject metadata) {
        if (metadata.containsKey(FIELD_SUBMISSION_ID)) {
            return Optional.of(metadata.getString(FIELD_SUBMISSION_ID));
        }
        return Optional.empty();
    }

    private boolean isCaseCompletedAndNotReferredToCourt(final JsonEnvelope envelope, final String caseUrn) {
        if (!isBlank(caseUrn)) {
            final JsonObject caseDetails = sjpService.getCaseDetails(caseUrn, envelope);
            if (nonNull(caseDetails) && caseDetails.containsKey("completed") && caseDetails.containsKey("status")) {
                final boolean completed = caseDetails.getBoolean("completed");
                final String status = caseDetails.getString("status");
                return (completed && !CASE_STATUS_REFERRED_FOR_COURT_HEARING.equalsIgnoreCase(status));
            }
        }
        return false;
    }
}


