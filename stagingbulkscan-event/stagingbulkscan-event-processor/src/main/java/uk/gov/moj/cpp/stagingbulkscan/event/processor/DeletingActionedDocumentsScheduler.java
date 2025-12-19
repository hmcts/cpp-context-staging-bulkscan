package uk.gov.moj.cpp.stagingbulkscan.event.processor;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.ApplicationParameters;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
@ServiceComponent(EVENT_PROCESSOR)
public class DeletingActionedDocumentsScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletingActionedDocumentsScheduler.class);

    private static final String TIMER_TIMEOUT_INFO = "StagingBulkScanEventProcessorScheduler timer triggered.";
    private static final String STAGING_BULK_SCAN_QUERY_GET_DOCUMENTS_BY_STATUSES = "stagingbulkscan.get-all-documents-by-status";
    private static final String DELETE_ACTIONED_DOCUMENTS = "stagingbulkscan.command.delete-actioned-document";

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Resource
    private TimerService timerService;

    @Handles("dummy")
    public void dummy(final JsonEnvelope envelope) {
        throw new UnsupportedOperationException();
    }

    @PostConstruct
    public void init() {

        timerService.createIntervalTimer(
            30000L,
            Long.parseLong(applicationParameters.getStagingBulkScanEventProcessorSchedulerIntervalMillis()),
            new TimerConfig(TIMER_TIMEOUT_INFO, false));
    }

    @Timeout
    public void startTimer() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StagingBulkScan scheduler triggers.");
        }

        LOGGER.info("DeletingActionedDocumentsScheduler triggers.");

        final List<ScanDocument> actionedDocuments = getAllActionedDocuments();

        final List<ScanDocument> documentsTobeDeleted = actionedDocuments.stream()
                .filter(document -> DAYS.between(document.getStatusUpdatedDate(), ZonedDateTime.now()) > Integer.parseInt(applicationParameters.getDeleteAfterActionedDays()))
                .collect(Collectors.toList());

        LOGGER.info("number of documents to be deleted are {}", documentsTobeDeleted.size());

        documentsTobeDeleted.forEach(document ->
                sender.send(envelopeFrom(metadataBuilder().withId(randomUUID()).withName(DELETE_ACTIONED_DOCUMENTS).build(), buildPayload(document))));
    }

    @PreDestroy
    public void cleanup() {
        timerService.getTimers().forEach(Timer::cancel);
    }

    private List<ScanDocument> getAllActionedDocuments() {

        final Envelope<JsonValue> requestEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID()).withName(STAGING_BULK_SCAN_QUERY_GET_DOCUMENTS_BY_STATUSES).build(),
                createObjectBuilder().add("status","MANUALLY_ACTIONED,AUTO_ACTIONED").build());

        //get all the actioned documents from query
        final Envelope<JsonObject>  actionedDocuments = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return convertToList(actionedDocuments.payload().getJsonArray("scanDocuments"), ScanDocument.class);
    }

    private JsonObject buildPayload(final ScanDocument document) {
        return createObjectBuilder()
                .add("scanEnvelopeId", document.getScanEnvelopeId().toString())
                .add("scanDocumentId", document.getId().toString())
                .build();
    }

    private <T> List<T> convertToList(final JsonArray jsonArray, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(this.jsonObjectToObjectConverter.convert(jsonArray.getJsonObject(i), clazz));
        }
        return list;
    }
}