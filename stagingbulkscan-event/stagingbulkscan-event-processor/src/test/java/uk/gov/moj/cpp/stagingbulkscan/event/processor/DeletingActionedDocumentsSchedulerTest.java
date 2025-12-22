package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.ApplicationParameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

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

    private void assertMethodIsAnnotatedWith(final Method method, final Class<? extends Annotation> annotationClass) {
        assertThat(method.isAnnotationPresent(annotationClass), is(true));
    }
}
