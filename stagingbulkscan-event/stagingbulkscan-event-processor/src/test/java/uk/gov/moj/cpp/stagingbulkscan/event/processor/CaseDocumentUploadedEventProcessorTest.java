package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.stagingbulkscan.command.ExpireDocument;
import uk.gov.justice.stagingbulkscan.command.RejectDocument;
import uk.gov.justice.stagingbulkscan.domain.AutoActionDocument;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SJPService;
import uk.gov.moj.cpp.stagingbulkscan.event.service.StagingBulkScanService;
import uk.gov.moj.cpp.stagingbulkscan.event.service.SystemIdMapperService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialFollowup;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.MaterialRejected;

import java.util.ArrayList;
import java.util.UUID;

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
public class CaseDocumentUploadedEventProcessorTest {
    private static final String SJP_DOCUMENT_ADDED_EVENT = "public.sjp.case-document-added";
    private static final String PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    private static final String COMMAND_AUTO_ACTION_SCAN_DOCUMENT = "stagingbulkscan.command.auto-action-scan-document";
    private static final String STAGINGBULKSCAN_EVENTS_DOCUMENT_NEXT_STEP_DECIDED = "stagingbulkscan.events.document-next-step-decided";
    private static final String STAGINGBULKSCAN_COMMAND_DECIDE_DOCUMENT_NEXT_STEP = "stagingbulkscan.command.decide-document-next-step";
    private static final UUID SUBMISSION_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID ADDED_BY = randomUUID();
    private static final UUID SCAN_DOCUMENT_ID = randomUUID();
    private static final UUID SCAN_ENVELOPE_ID = randomUUID();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private StagingBulkScanService stagingBulkScanService;

    @InjectMocks
    private CaseDocumentUploadedEventProcessor caseDocumentUploadedEventProcessor;

    @Mock
    private SJPService sjpService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldHandleCaseDocumentUploadedCommandForAPendingCase() {
        final String caseUrn = "12345";
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                        metadataWithRandomUUID(STAGINGBULKSCAN_EVENTS_DOCUMENT_NEXT_STEP_DECIDED)
                                .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .build())
                .build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                createObjectBuilder()
                        .add("isSjp", true)
                        .add("caseUrn", caseUrn)
                        .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                        .add("scanDocumentId", SCAN_DOCUMENT_ID.toString())
                        .build());

        final JsonObject caseDetails = createObjectBuilder()
                .add("status", "PENDING")
                .add("completed", false)
                .build();
        when(sjpService.getCaseDetails(caseUrn, event)).thenReturn(caseDetails);

        caseDocumentUploadedEventProcessor.handleDocumentNextStepDecided(event);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final AutoActionDocument autoActionDocument = (AutoActionDocument) envelope.payload();
        assertThat(envelope.metadata().name(), is(COMMAND_AUTO_ACTION_SCAN_DOCUMENT));
        assertThat(autoActionDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(autoActionDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
        assertThat(autoActionDocument.getActionedBy(), is(ADDED_BY));
    }

    @Test
    public void shouldHandleCaseDocumentUploadedCommandForCompletedCase() {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(SJP_DOCUMENT_ADDED_EVENT)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .add("submissionId", SUBMISSION_ID.toString()).build()).build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                createObjectBuilder()
                        .add("materialId", MATERIAL_ID.toString())
                        .build());

        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(SCAN_ENVELOPE_ID + ":" + SCAN_DOCUMENT_ID);

        caseDocumentUploadedEventProcessor.handleSjpCaseDocumentUploaded(event);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(STAGINGBULKSCAN_COMMAND_DECIDE_DOCUMENT_NEXT_STEP));
        final JsonObject jsonObject1 = objectToJsonObjectConverter.convert(envelope.payload());
        assertThat(jsonObject1.getString("scanEnvelopeId"), is(SCAN_ENVELOPE_ID.toString()));
        assertThat(jsonObject1.getString("scanDocumentId"), is(SCAN_DOCUMENT_ID.toString()));
        assertThat(jsonObject1.getBoolean("isSjp"), is(true));
    }

    @Test
    void shouldHandleCaseDocumentUploadedCommandForProgressionCase() {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(SJP_DOCUMENT_ADDED_EVENT)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .add("submissionId", SUBMISSION_ID.toString()).build()).build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                createObjectBuilder()
                        .add("materialId", MATERIAL_ID.toString())
                        .build());

        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(SCAN_ENVELOPE_ID + ":" + SCAN_DOCUMENT_ID);

        caseDocumentUploadedEventProcessor.handleProgressionCourtDocumentAdded(event);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(STAGINGBULKSCAN_COMMAND_DECIDE_DOCUMENT_NEXT_STEP));
        final JsonObject jsonObject1 = objectToJsonObjectConverter.convert(envelope.payload());
        assertThat(jsonObject1.getString("scanEnvelopeId"), is(SCAN_ENVELOPE_ID.toString()));
        assertThat(jsonObject1.getString("scanDocumentId"), is(SCAN_DOCUMENT_ID.toString()));
        assertThat(jsonObject1.getBoolean("isSjp"), is(false));
    }



    @Test
    public void shouldHandleCaseDocumentUploadedCommandForCompletedCaseAndReferredToCC() {
        final String caseUrn = "12345";
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(STAGINGBULKSCAN_EVENTS_DOCUMENT_NEXT_STEP_DECIDED)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .add("submissionId", SUBMISSION_ID.toString()).build()).build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                createObjectBuilder()
                        .add("isSjp", true)
                        .add("caseUrn", caseUrn)
                        .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                        .add("scanDocumentId", SCAN_DOCUMENT_ID.toString())
                        .build());

        final JsonObject caseDetails = createObjectBuilder()
                .add("status", "REFERRED_FOR_COURT_HEARING")
                .add("completed", true)
                .build();
        when(sjpService.getCaseDetails(caseUrn, event)).thenReturn(caseDetails);

        caseDocumentUploadedEventProcessor.handleDocumentNextStepDecided(event);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final AutoActionDocument autoActionDocument = (AutoActionDocument) envelope.payload();
        assertThat(envelope.metadata().name(), is(COMMAND_AUTO_ACTION_SCAN_DOCUMENT));
        assertThat(autoActionDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(autoActionDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
        assertThat(autoActionDocument.getActionedBy(), is(ADDED_BY));
    }


    @Test
    public void shouldHandleProgressionCourtDocumentAddedEvent() {
        final String caseUrn = "12345";
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(STAGINGBULKSCAN_EVENTS_DOCUMENT_NEXT_STEP_DECIDED)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .add("submissionId", SUBMISSION_ID.toString()).build()).build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                createObjectBuilder()
                        .add("isSjp", false)
                        .add("caseUrn", caseUrn)
                        .add("scanEnvelopeId", SCAN_ENVELOPE_ID.toString())
                        .add("scanDocumentId", SCAN_DOCUMENT_ID.toString())
                        .build());

        caseDocumentUploadedEventProcessor.handleDocumentNextStepDecided(event);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final AutoActionDocument autoActionDocument = (AutoActionDocument) envelope.payload();

        assertThat(envelope.metadata().name(), is(COMMAND_AUTO_ACTION_SCAN_DOCUMENT));
        assertThat(autoActionDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(autoActionDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
        assertThat(autoActionDocument.getActionedBy(), is(ADDED_BY));
    }

    @Test
    public void shouldHandleProgressionCourtDocumentAddedWhenNoDocumentReferenceExists() {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .add("submissionId", SUBMISSION_ID.toString()).build()).build();
        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                mockCourtDocument());

        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(null);

        caseDocumentUploadedEventProcessor.handleProgressionCourtDocumentAdded(event);
        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleProgressionCourtDocumentAddedWhenSubmissionIdAvailable() {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(createObjectBuilder(
                metadataWithRandomUUID(PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED)
                        .withUserId(ADDED_BY.toString()).build().asJsonObject())
                .build()).build();

        final JsonEnvelope event = envelopeFrom(enrichedMaterialAddedMetadata,
                mockCourtDocument());

        caseDocumentUploadedEventProcessor.handleProgressionCourtDocumentAdded(event);
        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleMaterialRejectedWhenSubmissionIdAvailable() {
        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(SCAN_ENVELOPE_ID + ":" + SCAN_DOCUMENT_ID);

        final MaterialRejected materialRejected = MaterialRejected.materialRejected()
                .withCaseId(randomUUID())
                .withErrors(new ArrayList<>())
                .build();

        final Envelope<MaterialRejected> materialRejectedEnvelope = envelope("public.prosecutioncasefile.material-rejected", materialRejected);

        caseDocumentUploadedEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final RejectDocument rejectDocument = (RejectDocument) envelope.payload();

        assertThat(rejectDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(rejectDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
    }

    @Test
    public void shouldNotSendAnyEventWhenNoDocumentReferenceExists() {
        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(null);

        final MaterialRejected materialRejected = MaterialRejected.materialRejected()
                .withCaseId(randomUUID())
                .withErrors(new ArrayList<>())
                .build();

        final Envelope<MaterialRejected> materialRejectedEnvelope = envelope("public.prosecutioncasefile.material-rejected", materialRejected);

        caseDocumentUploadedEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldNotSendAnyEventWhenSubmissionIdIsMissing() {

        final MaterialRejected materialRejected = MaterialRejected.materialRejected()
                .withCaseId(randomUUID())
                .withErrors(new ArrayList<>())
                .build();

        final Envelope<MaterialRejected> materialRejectedEnvelope = envelopeWithNoSubmissionId("public.prosecutioncasefile.material-rejected", materialRejected);

        caseDocumentUploadedEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleBulkScanMaterialRejected() {
        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(SCAN_ENVELOPE_ID + ":" + SCAN_DOCUMENT_ID);

        final BulkscanMaterialFollowup bulkscanMaterialFollowup = BulkscanMaterialFollowup.bulkscanMaterialFollowup().withCaseId(randomUUID()).withMaterial(Material.material().build()).withErrors(new ArrayList<>()).build();

        final Envelope<BulkscanMaterialFollowup> bulkScanMaterialFollowupEnvelope = envelope("public.prosecutioncasefile.bulkscan-material-followup", bulkscanMaterialFollowup);

        caseDocumentUploadedEventProcessor.handleBulkScanMaterialRejected(bulkScanMaterialFollowupEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final ExpireDocument expireDocument = (ExpireDocument) envelope.payload();

        assertThat(expireDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(expireDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
    }

    @Test
    public void shouldHandleBulkScanMaterialRejectedWithErrors() {
        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(SCAN_ENVELOPE_ID + ":" + SCAN_DOCUMENT_ID);

        final Problem problem = Problem.problem()
                .withCode("123")
                .withValues(singletonList(ProblemValue.problemValue()
                        .withId(randomUUID().toString())
                        .withKey(randomUUID().toString())
                        .withValue(randomUUID().toString()).build()))
                .build();

        final BulkscanMaterialFollowup bulkscanMaterialFollowup = BulkscanMaterialFollowup.bulkscanMaterialFollowup().withCaseId(randomUUID()).withMaterial(Material.material().build()).withErrors(singletonList(problem)).build();

        final Envelope<BulkscanMaterialFollowup> bulkScanMaterialFollowupEnvelope = envelope("public.prosecutioncasefile.bulkscan-material-followup", bulkscanMaterialFollowup);

        caseDocumentUploadedEventProcessor.handleBulkScanMaterialRejected(bulkScanMaterialFollowupEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<?> envelope = this.envelopeArgumentCaptor.getValue();
        final ExpireDocument expireDocument = (ExpireDocument) envelope.payload();

        assertThat(expireDocument.getScanEnvelopeId(), is(SCAN_ENVELOPE_ID));
        assertThat(expireDocument.getScanDocumentId(), is(SCAN_DOCUMENT_ID));
    }

    @Test
    public void shouldHandleBulkScanMaterialRejectedWhenNoDocumentReferenceExists() {
        when(systemIdMapperService.getScanDocumentReferenceFor(SUBMISSION_ID)).thenReturn(null);

        final BulkscanMaterialFollowup bulkscanMaterialFollowup = BulkscanMaterialFollowup.bulkscanMaterialFollowup().withCaseId(randomUUID()).withMaterial(Material.material().build()).withErrors(new ArrayList<>()).build();

        final Envelope<BulkscanMaterialFollowup> bulkScanMaterialFollowupEnvelope = envelope("public.prosecutioncasefile.bulkscan-material-followup", bulkscanMaterialFollowup);

        caseDocumentUploadedEventProcessor.handleBulkScanMaterialRejected(bulkScanMaterialFollowupEnvelope);

        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleBulkScanMaterialRejectedWhenSubmissionIdIsMissing() {

        final BulkscanMaterialFollowup bulkscanMaterialFollowup = BulkscanMaterialFollowup.bulkscanMaterialFollowup().withCaseId(randomUUID()).withMaterial(Material.material().build()).withErrors(new ArrayList<>()).build();

        final Envelope<BulkscanMaterialFollowup> bulkScanMaterialFollowupEnvelope = envelopeWithNoSubmissionId("public.prosecutioncasefile.bulkscan-material-followup", bulkscanMaterialFollowup);

        caseDocumentUploadedEventProcessor.handleBulkScanMaterialRejected(bulkScanMaterialFollowupEnvelope);

        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    private JsonObject mockCourtDocument() {
        return createObjectBuilder().add("courtDocument",
                createObjectBuilder()
                        .add("courtDocumentId", randomUUID().toString())
                        .add("materials", createArrayBuilder()
                                .add(createObjectBuilder().add("id", MATERIAL_ID.toString()))
                        ))
                .add("containsFinancialMeans", false)
                .build();
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final String USER_ID = "726b6391-7bc2-445a-8a0c-9bcfd963703e";
        JsonObject jsonObject = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", name)
                .add("userId", USER_ID)
                .add("submissionId", SUBMISSION_ID.toString())
                .build();
        final MetadataBuilder metadataBuilder = Envelope.metadataFrom(jsonObject);
        return Envelope.envelopeFrom(metadataBuilder, t);
    }

    private <T> Envelope<T> envelopeWithNoSubmissionId(final String name, final T t) {
        final String USER_ID = "726b6391-7bc2-445a-8a0c-9bcfd963703e";
        JsonObject jsonObject = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", name)
                .add("userId", USER_ID)
                .build();
        final MetadataBuilder metadataBuilder = Envelope.metadataFrom(jsonObject);
        return Envelope.envelopeFrom(metadataBuilder, t);
    }
}
