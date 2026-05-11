package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.ApplicationParameters;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.openejb.core.timer.TimerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeletingActionedDocumentsSchedulerTest {

    @Mock
    private TimerService timerService;

    @Mock
    private TimerImpl timer;

    @Captor
    private ArgumentCaptor<TimerConfig> timerConfigArgumentCaptor;

    @InjectMocks
    private DeletingActionedDocumentsScheduler underTest;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Test
    public void shouldCreateTimerOnInit() throws Exception {
        final long delay = 30000L;
        final long interval = 1000L;

        when(applicationParameters.getStagingBulkScanEventProcessorSchedulerIntervalMillis()).thenReturn(String.valueOf(interval));
        underTest.init();

        verify(timerService).createIntervalTimer(eq(delay), eq(interval), timerConfigArgumentCaptor.capture());
        final TimerConfig configuredTimerConfig = timerConfigArgumentCaptor.getValue();
        assertThat(configuredTimerConfig.isPersistent(), is(false));
        assertThat(configuredTimerConfig.getInfo(), is("StagingBulkScanEventProcessorScheduler timer triggered."));
        assertMethodIsAnnotatedWith(DeletingActionedDocumentsScheduler.class.getMethod("init"), PostConstruct.class);
    }

    @Test
    public void shouldStopTimerOnCleanup() throws Exception {
        given(timerService.getTimers()).willReturn(singleton(timer));

        underTest.cleanup();

        verify(timer).cancel();
        assertMethodIsAnnotatedWith(DeletingActionedDocumentsScheduler.class.getMethod("cleanup"), PreDestroy.class);
    }

    @Test
    public void startTimer_whenEligibleDocumentsExist_shouldSendDeleteCommandForEach() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final JsonObject responsePayload = buildResponseWithDocuments(2);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        final ScanDocument doc1 = buildScanDocument();
        final ScanDocument doc2 = buildScanDocument();
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ScanDocument.class)))
                .thenReturn(doc1)
                .thenReturn(doc2);

        underTest.startTimer();

        verify(sender, times(2)).send(any());
    }

    @Test
    public void startTimer_whenNoEligibleDocuments_shouldNotSendAnyCommand() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final JsonObject responsePayload = buildResponseWithDocuments(0);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        underTest.startTimer();

        verify(sender, never()).send(any());
    }

    @Test
    public void startTimer_shouldPassCutoffDateBasedOnRetentionDays() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final JsonObject responsePayload = buildResponseWithDocuments(0);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        final ZonedDateTime expectedCutoff = ZonedDateTime.now().minus(90, DAYS);

        underTest.startTimer();

        final ArgumentCaptor<Envelope> requestCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(requester).requestAsAdmin(requestCaptor.capture(), eq(JsonObject.class));
        final JsonObject capturedPayload = (JsonObject) requestCaptor.getValue().payload();
        final ZonedDateTime capturedCutoffDate = ZonedDateTime.parse(capturedPayload.getString("cutoffDate"));

        final long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(expectedCutoff, capturedCutoffDate));
        assertThat(diffSeconds, is(lessThan(5L)));
    }

    @Test
    public void startTimer_shouldPassConfiguredBatchSizeToQuery() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("1000");

        final JsonObject responsePayload = buildResponseWithDocuments(0);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        underTest.startTimer();

        final ArgumentCaptor<Envelope> requestCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(requester).requestAsAdmin(requestCaptor.capture(), eq(JsonObject.class));
        final int capturedMaxResults = ((JsonObject) requestCaptor.getValue().payload()).getInt("maxResults");
        assertThat(capturedMaxResults, is(1000));
    }

    @Test
    public void startTimer_whenBatchSizeConfigMissing_shouldUseDefault50000() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn(null);

        final JsonObject responsePayload = buildResponseWithDocuments(0);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        underTest.startTimer();

        final ArgumentCaptor<Envelope> requestCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(requester).requestAsAdmin(requestCaptor.capture(), eq(JsonObject.class));
        final int capturedMaxResults = ((JsonObject) requestCaptor.getValue().payload()).getInt("maxResults");
        assertThat(capturedMaxResults, is(50_000));
    }

    @Test
    public void startTimer_shouldSendDeleteCommandForAllThreeFetchedDocuments() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final JsonObject responsePayload = buildResponseWithDocuments(3);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        final ScanDocument doc = buildScanDocument();
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ScanDocument.class))).thenReturn(doc);

        underTest.startTimer();

        verify(sender, times(3)).send(any());
    }

    @Test
    public void startTimer_shouldSubmitExactlyAsManyCommandsAsDocumentsFetched() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("90");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final int docCount = 5;
        final JsonObject responsePayload = buildResponseWithDocuments(docCount);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        final ScanDocument doc = buildScanDocument();
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ScanDocument.class))).thenReturn(doc);

        underTest.startTimer();

        verify(sender, times(docCount)).send(any());
    }

    @Test
    public void startTimer_whenDeleteAfterDaysConfigInvalid_shouldUseDefault30Days() {
        when(applicationParameters.getDeleteAfterActionedDays()).thenReturn("not-a-number");
        when(applicationParameters.getDeletionBatchSize()).thenReturn("50000");

        final JsonObject responsePayload = buildResponseWithDocuments(0);
        final Envelope<JsonObject> mockResponse = mockEnvelopeWith(responsePayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(mockResponse);

        underTest.startTimer();

        final ArgumentCaptor<Envelope> requestCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(requester).requestAsAdmin(requestCaptor.capture(), eq(JsonObject.class));
        final ZonedDateTime capturedCutoffDate =
                ZonedDateTime.parse(((JsonObject) requestCaptor.getValue().payload()).getString("cutoffDate"));
        final ZonedDateTime expectedCutoff = ZonedDateTime.now().minus(30, DAYS);
        final long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(expectedCutoff, capturedCutoffDate));
        assertThat(diffSeconds, is(lessThan(5L)));
    }

    @SuppressWarnings("unchecked")
    private Envelope<JsonObject> mockEnvelopeWith(final JsonObject payload) {
        final Envelope<JsonObject> envelope = mock(Envelope.class);
        when(envelope.payload()).thenReturn(payload);
        return envelope;
    }

    private JsonObject buildResponseWithDocuments(final int count) {
        final javax.json.JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < count; i++) {
            arrayBuilder.add(Json.createObjectBuilder().build());
        }
        return Json.createObjectBuilder().add("scanDocuments", arrayBuilder.build()).build();
    }

    private ScanDocument buildScanDocument() {
        final ScanDocument doc = new ScanDocument();
        doc.setId(randomUUID());
        doc.setScanEnvelopeId(randomUUID());
        return doc;
    }

    private void assertMethodIsAnnotatedWith(final Method method, final Class<? extends Annotation> annotationClass) {
        assertThat(method.isAnnotationPresent(annotationClass), is(true));
    }
}
