package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.ApplicationParameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.openejb.core.timer.TimerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@ExtendWith(MockitoExtension.class)
public class DeletingActionedDocumentsSchedulerTest {

    private static final String DELETE_AFTER_DAYS = "30";

    @Mock
    private TimerService timerService;

    @Mock
    private TimerImpl timer;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Captor
    private ArgumentCaptor<TimerConfig> timerConfigCaptor;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeCaptor;

    @InjectMocks
    private DeletingActionedDocumentsScheduler underTest;

    @BeforeEach
    public void setUp() {
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateTimerOnInit() throws Exception {
        final long delay = 30000L;
        final long interval = 1000L;

        when(applicationParameters.getStagingBulkScanEventProcessorSchedulerIntervalMillis()).thenReturn(String.valueOf(interval));
        underTest.init();

        verify(timerService).createIntervalTimer(eq(delay), eq(interval), timerConfigCaptor.capture());
        final TimerConfig config = timerConfigCaptor.getValue();
        assertThat(config.isPersistent(), is(false));
        assertThat(config.getInfo(), is("StagingBulkScanEventProcessorScheduler timer triggered."));
        assertMethodIsAnnotatedWith(DeletingActionedDocumentsScheduler.class.getMethod("init"), PostConstruct.class);
    }

    @Test
    public void shouldStopTimerOnCleanup() throws Exception {
        given(timerService.getTimers()).willReturn(Collections.singleton(timer));

        underTest.cleanup();

        verify(timer).cancel();
        assertMethodIsAnnotatedWith(DeletingActionedDocumentsScheduler.class.getMethod("cleanup"), PreDestroy.class);
    }

    @Test
    public void shouldSendDeleteCommandForEachEligibleDocument() {
        final UUID docId1 = randomUUID();
        final UUID envId1 = randomUUID();
        final UUID docId2 = randomUUID();
        final UUID envId2 = randomUUID();

        final JsonArray scanDocuments = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("id", docId1.toString())
                        .add("scanEnvelopeId", envId1.toString()))
                .add(Json.createObjectBuilder()
                        .add("id", docId2.toString())
                        .add("scanEnvelopeId", envId2.toString()))
                .build();

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", scanDocuments)
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn(DELETE_AFTER_DAYS);
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        underTest.startTimer();

        verify(sender, times(2)).send(any());
    }

    @Test
    public void shouldNotSendCommandsWhenNoDocumentsEligible() {
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", Json.createArrayBuilder().build())
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn(DELETE_AFTER_DAYS);
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        underTest.startTimer();

        verify(sender, never()).send(any());
    }

    @Test
    public void shouldComputeCorrectCutoffDateInRequestPayload() {
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", Json.createArrayBuilder().build())
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("10");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");
        when(requester.requestAsAdmin(envelopeCaptor.capture(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        final ZonedDateTime before = ZonedDateTime.now().minusDays(10);
        underTest.startTimer();
        final ZonedDateTime after = ZonedDateTime.now().minusDays(10);

        final JsonObject requestPayload = (JsonObject) envelopeCaptor.getValue().payload();
        final ZonedDateTime captured = ZonedDateTime.parse(requestPayload.getString("cutoffDate"));

        assertThat(captured.isAfter(before) || captured.isEqual(before), is(true));
        assertThat(captured.isBefore(after) || captured.isEqual(after), is(true));
    }

    @Test
    public void shouldUseDefaultBatchSizeWhenNotConfigured() {
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", Json.createArrayBuilder().build())
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn(DELETE_AFTER_DAYS);
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50");
        when(requester.requestAsAdmin(envelopeCaptor.capture(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        underTest.startTimer();

        final JsonObject requestPayload = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(requestPayload.getInt("batchSize"), is(50));
    }

    @Test
    public void shouldUseConfiguredBatchSize() {
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", Json.createArrayBuilder().build())
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn(DELETE_AFTER_DAYS);
        when(applicationParameters.getDeletionBatchSize()).thenReturn("1000");
        when(requester.requestAsAdmin(envelopeCaptor.capture(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        underTest.startTimer();

        final JsonObject requestPayload = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(requestPayload.getInt("batchSize"), is(1000));
    }

    @Test
    public void shouldUseCorrectQueryName() {
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("scanDocuments", Json.createArrayBuilder().build())
                .build();

        final Envelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingbulkscan.get-documents-for-deletion"),
                responsePayload);

        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn(DELETE_AFTER_DAYS);
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");
        when(requester.requestAsAdmin(envelopeCaptor.capture(), eq(JsonObject.class))).thenReturn(responseEnvelope);

        underTest.startTimer();

        assertThat(envelopeCaptor.getValue().metadata().name(), is("stagingbulkscan.get-documents-for-deletion"));
    }

    private void assertMethodIsAnnotatedWith(final Method method, final Class<? extends Annotation> annotationClass) {
        assertThat(method.isAnnotationPresent(annotationClass), is(true));
    }
}
