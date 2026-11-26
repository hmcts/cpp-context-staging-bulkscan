package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ActionedDocumentDeleted;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class StagingBulkScanEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingBulkScanEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    @Transactional
    @Handles("stagingbulkscan.events.actioned-document-deleted")
    public void deleteActionedDocumentsAfterLimit(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.actioned-document-deleted {}", event.toObfuscatedDebugString());
        }

        final ActionedDocumentDeleted deleteEvent = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ActionedDocumentDeleted.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(deleteEvent.getScanDocumentId(), deleteEvent.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);

        if(nonNull(scanDocument)) {
            scanDocument.setDeleted(true);
            scanDocument.setDeletedDate(deleteEvent.getDeletedDate());
            scanDocumentRepository.save(scanDocument);
        }
    }
}
