package uk.gov.moj.cpp.stagingbulkscan.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.stagingbulkscan.domain.ScanDocument.scanDocument;
import static uk.gov.justice.stagingbulkscan.domain.ScanEnvelope.scanEnvelope;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.stagingbulkscan.command.ExpireDocument;
import uk.gov.justice.stagingbulkscan.command.RejectDocument;
import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.AutoActionDocument;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentExpired;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentRejected;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.StagingBulkScanAggregate;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAttachedAndFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.ProblemValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanCommandHandlerTest {
    private static final String USER_ID = "726b6391-7bc2-445a-8a0c-9bcfd963703e";
    private static final UUID DOCUMENT_ID_1 = UUID.fromString("ec998ef3-4775-4166-9ad8-f5eeba78ded1");
    private static final String PDF_FILE_1 = "PDF_FILE_1.pdf";
    private static final String ZIP_FILE_1 = "ZIP_FILE_1.zip";
    private static final String MARK_AS_ACTIONED_EVENT = "stagingbulkscan.events.mark-as-actioned";
    private static final String MARK_AS_ACTIONED_COMMAND = "stagingbulkscan.command.mark-as-action";
    private static final String DELETE_ACTIONED_DOCUMENTS_AFTER_LIMIT_COMMAND = "stagingbulkscan.command.delete-actioned-document";
    private static final String DELETE_ACTIONED_DOCUMENTS_AFTER_LIMIT_EVENT = "stagingbulkscan.events.actioned-document-deleted";
    private static final String DOCUMENT_AUTO_ACTIONED_EVENT = "stagingbulkscan.events.document-auto-actioned";
    private static final String AUTO_ACTION_DOCUMENT_COMMAND = "stagingbulkscan.command.auto-action-scan-document";
    private static final String DOCUMENT_REJECTED = "stagingbulkscan.events.scan-document-rejected";
    private static final String DOCUMENT_EXPIRED = "stagingbulkscan.events.scan-document-expired";
    private static final String COMMAND_REGISTER_SCAN_ENVELOPE = "stagingbulkscan.command.register-scan-envelope";
    private static final String SCAN_ENVELOPE_REGISTERED_EVENT = "stagingbulkscan.events.scan-envelope-registered";

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            ScanEnvelopeRegistered.class,
            ScanDocumentManuallyActioned.class,
            ActionedDocumentDeleted.class,
            ScanDocumentAutoActioned.class,
            ScanDocumentRejected.class,
            ScanDocumentExpired.class,
            ScanDocumentFollowedUp.class,
            ScanDocumentAttachedAndFollowedUp.class);

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private StagingBulkScanCommandHandler stagingBulkScanCommandHandler;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void registerScanEnvelopeWithAllValues() throws EventStreamException, IOException {

        final ScanEnvelope scanEnvelope = object("stagingbulkscan.command.register-scan-envelope.json", ScanEnvelope.class);

        final Envelope<ScanEnvelope> scanEnvelopeEnvelope = envelope(COMMAND_REGISTER_SCAN_ENVELOPE, scanEnvelope);

        setupMockedEventStream(scanEnvelope.getScanEnvelopeId(), this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.registerEnvelope(scanEnvelopeEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(scanEnvelopeEnvelope).withName(SCAN_ENVELOPE_REGISTERED_EVENT),
                        payloadIsJson(allOf(withJsonPath("$.scanEnvelope.zipFileName", is(scanEnvelope.getZipFileName())))))));
    }

    @Test
    public void registerScanEnvelopeWithOnlyMandatoryValues() throws EventStreamException, IOException {

        final ScanEnvelope scanEnvelope = object("stagingbulkscan.command.register-scan-envelope_mandatory.json", ScanEnvelope.class);

        final Envelope<ScanEnvelope> scanEnvelopeEnvelope = envelope(COMMAND_REGISTER_SCAN_ENVELOPE, scanEnvelope);

        setupMockedEventStream(scanEnvelope.getScanEnvelopeId(), this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.registerEnvelope(scanEnvelopeEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(scanEnvelopeEnvelope).withName(SCAN_ENVELOPE_REGISTERED_EVENT),
                        payloadIsJson(allOf(withJsonPath("$.scanEnvelope.zipFileName",
                                is(scanEnvelope.getZipFileName())))))));
    }

    @Test
    public void markDocumentAsActioned() throws EventStreamException, IOException {
        final JsonObject jsonObject = getJsonObject("stagingbulkscan.command.mark-as-action.json");

        final UUID scanEnvelopeId = UUID.fromString(jsonObject.getString("scanEnvelopeId"));
        final UUID scanDocumentId = UUID.fromString(jsonObject.getString("scanDocumentId"));

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(MARK_AS_ACTIONED_COMMAND)
                .withUserId(USER_ID), objectToJsonObjectConverter.convert(jsonObject));

        final ScanEnvelope scanEnvelope = generateScanEnvelopeObject(scanEnvelopeId, scanDocumentId);
        final StagingBulkScanAggregate stagingBulkScanAggregate = new StagingBulkScanAggregate();
        stagingBulkScanAggregate.apply(new ScanEnvelopeRegistered(scanEnvelope));

        setupMockedEventStream(scanEnvelopeId, this.eventStream, stagingBulkScanAggregate);

        stagingBulkScanCommandHandler.markAsAction(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName(MARK_AS_ACTIONED_EVENT).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(jsonObject.getString("scanDocumentId"))),
                                withJsonPath("$.scanEnvelopeId", is(jsonObject.getString("scanEnvelopeId"))),
                                withJsonPath("$.actionedBy", is(jsonObject.getString("actionedBy"))),
                                withJsonPath("$.actionedDate", is(notNullValue()))
                        )))));
    }

    @Test
    public void deleteActionedDocumentsAfterLimit() throws EventStreamException, IOException {
        final JsonObject jsonObject = getJsonObject("stagingbulkscan.command.delete-actioned-document.json");

        final UUID scanEnvelopeId = UUID.fromString(jsonObject.getString("scanEnvelopeId"));
        final UUID scanDocumentId = UUID.fromString(jsonObject.getString("scanDocumentId"));

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(DELETE_ACTIONED_DOCUMENTS_AFTER_LIMIT_COMMAND)
                        .withUserId(USER_ID), objectToJsonObjectConverter.convert(jsonObject));

        final ScanEnvelope scanEnvelope = generateScanEnvelopeObject(scanEnvelopeId, scanDocumentId);
        final StagingBulkScanAggregate stagingBulkScanAggregate = new StagingBulkScanAggregate();
        stagingBulkScanAggregate.apply(new ScanEnvelopeRegistered(scanEnvelope));

        setupMockedEventStream(scanEnvelopeId, this.eventStream, stagingBulkScanAggregate);

        stagingBulkScanCommandHandler.deleteActionedDocuments(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName(DELETE_ACTIONED_DOCUMENTS_AFTER_LIMIT_EVENT).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", equalTo(jsonObject.getString("scanDocumentId"))),
                                withJsonPath("$.scanEnvelopeId", equalTo(jsonObject.getString("scanEnvelopeId")))
                        )))));
    }

    @Test
    public void updateDocumentAsAutoActioned() throws EventStreamException, IOException {

        final AutoActionDocument autoActionDocument = object("stagingbulkscan.command.auto-action-scan-document.json", AutoActionDocument.class);
        final Envelope<AutoActionDocument> scanEnvelopeEnvelope = envelope(AUTO_ACTION_DOCUMENT_COMMAND, autoActionDocument);

        final ScanEnvelope scanEnvelope = generateScanEnvelopeObject(autoActionDocument.getScanEnvelopeId(), autoActionDocument.getScanDocumentId());
        final StagingBulkScanAggregate stagingBulkScanAggregate = new StagingBulkScanAggregate();
        stagingBulkScanAggregate.apply(new ScanEnvelopeRegistered(scanEnvelope));

        setupMockedEventStream(scanEnvelope.getScanEnvelopeId(), this.eventStream, stagingBulkScanAggregate);

        stagingBulkScanCommandHandler.autoActionDocument(scanEnvelopeEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(scanEnvelopeEnvelope).withName(DOCUMENT_AUTO_ACTIONED_EVENT).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(autoActionDocument.getScanDocumentId().toString())),
                                withJsonPath("$.scanEnvelopeId", is(autoActionDocument.getScanEnvelopeId().toString())),
                                withJsonPath("$.actionedBy", is(autoActionDocument.getActionedBy().toString())),
                                withJsonPath("$.statusUpdatedDate", is(notNullValue()))
                        )))));
    }

    @Test
    public void rejectDocumentHandler() throws EventStreamException {

        final UUID scanEnvelopeId = randomUUID();

        final RejectDocument rejectDocument = RejectDocument.rejectDocument()
                .withScanEnvelopeId(scanEnvelopeId)
                .withScanDocumentId(DOCUMENT_ID_1)
                .withErrors(buildProblems())
                .build();

        final Envelope<RejectDocument> rejectDocumentEnvelope = envelope("stagingbulkscan.command.reject-document", rejectDocument);

        setupMockedEventStream(scanEnvelopeId, this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.rejectDocument(rejectDocumentEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(rejectDocumentEnvelope).withName(DOCUMENT_REJECTED).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(DOCUMENT_ID_1.toString())),
                                withJsonPath("$.scanEnvelopeId", is(scanEnvelopeId.toString())),
                                withJsonPath("$.rejectedDate", is(notNullValue())),
                                withJsonPath("$.errors[*]", hasItem(isJson(allOf(
                                        withJsonPath("code", is("INVALID_FILE_TYPE")),
                                        withJsonPath("values", hasItem(isJson(allOf(
                                                withJsonPath("id", is("id")),
                                                withJsonPath("key", is("fileType")),
                                                withJsonPath("value", is("CCNIP"))
                                        ))))
                                ))))
                        )))));
    }

    @Test
    public void expireDocumentHandler() throws EventStreamException {

        final UUID scanEnvelopeId = randomUUID();
        final ZonedDateTime expireDate = new UtcClock().now();

        ExpireDocument expireDocument = ExpireDocument.expireDocument()
                .withScanEnvelopeId(scanEnvelopeId)
                .withScanDocumentId(DOCUMENT_ID_1)
                .withErrors(buildProblems())
                .withExpireDate(expireDate)
                .build();

        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("stagingbulkscan.command.expire-document").withUserId(USER_ID).build());
        final Envelope<ExpireDocument> expireDocumentEnvelope = envelopeFrom(metadataBuilder, expireDocument);

        setupMockedEventStream(scanEnvelopeId, this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.expireDocument(expireDocumentEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(expireDocumentEnvelope).withName(DOCUMENT_EXPIRED).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(DOCUMENT_ID_1.toString())),
                                withJsonPath("$.scanEnvelopeId", is(scanEnvelopeId.toString())),
                                withJsonPath("$.expiredDate", is(expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))))
                        )))));
    }

    @Test
    public void shouldHandleRaiseDocumentFollowUpEvent() throws EventStreamException {
        final UUID scanEnvelopeId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("scanEnvelopeId", scanEnvelopeId.toString())
                .add("scanDocumentId", DOCUMENT_ID_1.toString())
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("stagingbulkscan.command.raise-document-follow-up").withUserId(USER_ID).build());
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder, objectToJsonObjectConverter.convert(payload));

        setupMockedEventStream(scanEnvelopeId, this.eventStream, new StagingBulkScanAggregate());
        stagingBulkScanCommandHandler.raiseDocumentFollowUpEvent(envelope);

        Stream<JsonEnvelope> jsonEnvelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        JsonEnvelopeMatcher jsonEnvelopeMatcher = jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("stagingbulkscan.events.document-attached-with-follow-up").withUserId(USER_ID),
                payloadIsJson(allOf(
                        withJsonPath("$.scanDocumentId", is(DOCUMENT_ID_1.toString())),
                        withJsonPath("$.scanEnvelopeId", is(scanEnvelopeId.toString()))
                )));
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(jsonEnvelopeMatcher));

    }

    public void updateDefendantFinancialMeans() throws EventStreamException {

        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanEnvelopeId(randomUUID())
                .withScanDocumentId(DOCUMENT_ID_1)
                .build();

        final Envelope<AllFinancialMeans> envelope = envelope("stagingbulkscan.command.update-defendant-financial-means", allFinancialMeans);

        setupMockedEventStream(allFinancialMeans.getScanEnvelopeId(), this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.updateDefendantFinancialMeans(envelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("stagingbulkscan.events.defendant-financial-means-updated").withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(DOCUMENT_ID_1.toString()))
                        )))));
    }

    public void updateDefendantDetails() throws EventStreamException {

        final Defendant defendantDetails = Defendant.defendant().withScanEnvelopeId(randomUUID()).withScanDocumentId(DOCUMENT_ID_1).build();

        final Envelope<Defendant> expireDocumentEnvelope = envelope("stagingbulkscan.command.update-defendant-additional-details", defendantDetails);

        setupMockedEventStream(defendantDetails.getScanEnvelopeId(), this.eventStream, new StagingBulkScanAggregate());

        stagingBulkScanCommandHandler.updateDefendantDetails(expireDocumentEnvelope);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(withMetadataEnvelopedFrom(expireDocumentEnvelope).withName(DOCUMENT_EXPIRED).withUserId(USER_ID),
                        payloadIsJson(allOf(
                                withJsonPath("$.scanDocumentId", is(DOCUMENT_ID_1.toString()))
                        )))));
    }

    private List<Problem> buildProblems() {
        List<Problem> problems = new ArrayList<>();
        List<ProblemValue> problemValues = new ArrayList<>();
        problemValues.add(new ProblemValue("id", "fileType", "CCNIP"));
        problems.add(new Problem("INVALID_FILE_TYPE", problemValues));
        return problems;
    }

    private ScanEnvelope generateScanEnvelopeObject(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        return scanEnvelope()
                .withScanEnvelopeId(scanEnvelopeId)
                .withZipFileName(ZIP_FILE_1)
                .withAssociatedScanDocuments(asList(
                        scanDocument()
                                .withScanDocumentId(scanDocumentId)
                                .withFileName(PDF_FILE_1)
                                .build()))
                .build();
    }

    private <T extends Aggregate> void setupMockedEventStream(UUID id, EventStream eventStream, T aggregate) {
        when(this.eventSource.getStreamById(id)).thenReturn(eventStream);
        Class<T> clz = (Class<T>) aggregate.getClass();
        when(this.aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(USER_ID).build());
        return envelopeFrom(metadataBuilder, t);
    }

    private <T> T object(final String fileName, final Class<T> clz) throws IOException {
        final File file = new File(ClassLoader.getSystemClassLoader().getResource(fileName).getFile());
        final String eventPayloadString = new String(Files.readAllBytes(file.toPath()));
        return jsonObjectToObjectConverter.convert(new StringToJsonObjectConverter().convert(eventPayloadString), clz);
    }

    private JsonObject getJsonObject(final String s) throws IOException {
        final File file = new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource(
                s)).getFile());

        final String eventPayloadString = new String(Files.readAllBytes(file.toPath()));
        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }
}
