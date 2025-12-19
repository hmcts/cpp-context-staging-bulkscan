package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentExpired;
import uk.gov.justice.stagingbulkscan.domain.event.ScanDocumentRejected;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAttachedAndFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentAutoActioned;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentManuallyActioned;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class ScanDocumentEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanDocumentEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    @Transactional
    @Handles("stagingbulkscan.events.mark-as-actioned")
    public void updateScanDocumentToMarkAsActioned(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.mark-as-actioned {}", event.toObfuscatedDebugString());
        }

        final ScanDocumentManuallyActioned scanDocumentManuallyActioned = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentManuallyActioned.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentManuallyActioned.getScanDocumentId(), scanDocumentManuallyActioned.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(MANUALLY_ACTIONED);
            scanDocument.setStatusUpdatedDate(scanDocumentManuallyActioned.getActionedDate());
            if (nonNull(scanDocumentManuallyActioned.getActionedBy())) {
                scanDocument.setActionedBy(scanDocumentManuallyActioned.getActionedBy());
            }
            scanDocumentRepository.save(scanDocument);
        }
    }

    @Transactional
    @Handles("stagingbulkscan.events.document-auto-actioned")
    public void updateScanDocumentToMarkAsAutoActioned(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.document-auto-actioned {}", event.toObfuscatedDebugString());
        }

        final ScanDocumentAutoActioned scanDocumentAutoActioned = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentAutoActioned.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentAutoActioned.getScanDocumentId(), scanDocumentAutoActioned.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(AUTO_ACTIONED);
            scanDocument.setStatusUpdatedDate(scanDocumentAutoActioned.getStatusUpdatedDate());
            scanDocument.setActionedBy(scanDocumentAutoActioned.getActionedBy());
            scanDocumentRepository.save(scanDocument);
        }
    }

    @Transactional
    @Handles("stagingbulkscan.events.document-auto-actioned-with-follow-up")
    public void updateScanDocumentToMarkAsAutoActionedWithFollowUp(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.document-auto-actioned-with-follow-up {}", event.toObfuscatedDebugString());
        }

        final ScanDocumentFollowedUp scanDocumentFollowedUp = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentFollowedUp.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentFollowedUp.getScanDocumentId(), scanDocumentFollowedUp.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(FOLLOW_UP);
            scanDocument.setStatusUpdatedDate(scanDocumentFollowedUp.getStatusUpdatedDate());
            scanDocument.setActionedBy(scanDocumentFollowedUp.getActionedBy());
            scanDocument.setStatusCode(StatusCode.DEFENDANT_DETAILS_UPDATED);
            scanDocumentRepository.save(scanDocument);
        }
    }

    @Transactional
    @Handles("stagingbulkscan.events.scan-document-rejected")
    public void handleDocumentRejected(final JsonEnvelope event) {
        final ScanDocumentRejected scanDocumentRejected = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentRejected.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentRejected.getScanDocumentId(), scanDocumentRejected.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(FOLLOW_UP);
            scanDocument.setStatusUpdatedDate(scanDocumentRejected.getRejectedDate());
            scanDocumentRepository.save(scanDocument);
        }
    }

    @Transactional
    @Handles("stagingbulkscan.events.scan-document-expired")
    public void handleDocumentExpired(final JsonEnvelope event) {
        final ScanDocumentExpired scanDocumentExpired = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentExpired.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentExpired.getScanDocumentId(), scanDocumentExpired.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(FOLLOW_UP);
            scanDocument.setStatusUpdatedDate(scanDocumentExpired.getExpiredDate());
            scanDocument.setStatusCode(StatusCode.CASE_NOT_FOUND);
            scanDocumentRepository.save(scanDocument);
        }
    }

    @Transactional
    @Handles("stagingbulkscan.events.document-attached-with-follow-up")
    public void handleDocumentAttachedWithFollowUp(final JsonEnvelope event) {
        final ScanDocumentAttachedAndFollowedUp scanDocumentAttachedAndFollowedUp = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanDocumentAttachedAndFollowedUp.class);
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(scanDocumentAttachedAndFollowedUp.getScanDocumentId(), scanDocumentAttachedAndFollowedUp.getScanEnvelopeId());
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        if (nonNull(scanDocument)) {
            scanDocument.setStatus(FOLLOW_UP);
            scanDocument.setStatusUpdatedDate(scanDocumentAttachedAndFollowedUp.getStatusUpdatedDate());
            scanDocument.setStatusCode(StatusCode.DOCUMENT_ATTACHED_FOLLOWUP_REQUIRED);
            scanDocumentRepository.save(scanDocument);
            }
    }
}
