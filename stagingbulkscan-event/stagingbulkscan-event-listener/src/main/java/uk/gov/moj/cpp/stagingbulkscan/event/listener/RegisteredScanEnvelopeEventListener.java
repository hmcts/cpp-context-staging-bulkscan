package uk.gov.moj.cpp.stagingbulkscan.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanEnvelopeRegistered;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanEnvelopeRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2201"})
@ServiceComponent(EVENT_LISTENER)
public class RegisteredScanEnvelopeEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisteredScanEnvelopeEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ScanEnvelopeRepository scanEnvelopeRepository;

    @Transactional
    @Handles("stagingbulkscan.events.scan-envelope-registered")
    public void saveScanEnvelope(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingbulkscan.events.scan-envelope-registered {}", event.toObfuscatedDebugString());
        }

        final ScanEnvelopeRegistered scanEnvelopeRegistered = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ScanEnvelopeRegistered.class);

        final ScanEnvelope scanEnvelope = buildScanEnvelope(scanEnvelopeRegistered);

        scanEnvelopeRepository.save(scanEnvelope);
    }

    private ScanEnvelope buildScanEnvelope(final ScanEnvelopeRegistered envelope) {
        final ScanEnvelope scanEnvelopeEntity = new ScanEnvelope();
        final uk.gov.justice.stagingbulkscan.domain.ScanEnvelope scanEnvelope = envelope.getScanEnvelope();
        scanEnvelopeEntity.setId(scanEnvelope.getScanEnvelopeId());
        scanEnvelopeEntity.setExtractedDate(scanEnvelope.getExtractedDate());
        scanEnvelopeEntity.setZipFileName(scanEnvelope.getZipFileName());
        scanEnvelopeEntity.setNotes(scanEnvelope.getNotes());
        scanEnvelopeEntity.setAssociatedScanDocuments(buildScanDocuments(scanEnvelope.getScanEnvelopeId(), scanEnvelope.getAssociatedScanDocuments()));
        return scanEnvelopeEntity;
    }

    private Set<ScanDocument> buildScanDocuments(final UUID scanEnvelopeId, final List<uk.gov.justice.stagingbulkscan.domain.ScanDocument> associatedDocumentAttributes) {
        return associatedDocumentAttributes.stream().map(document -> {
            final ScanDocument scanDocument = new ScanDocument();
            scanDocument.setId(new ScanSnapshotKey(document.getScanDocumentId(), scanEnvelopeId));
            scanDocument.setCaseUrn(document.getCaseUrn());
            scanDocument.setCasePTIUrn(document.getCasePTIUrn());
            scanDocument.setDocumentFileName(document.getFileName());
            scanDocument.setDocumentName(document.getDocumentName());
            scanDocument.setProsecutorAuthorityId(document.getProsecutorAuthorityId());
            scanDocument.setProsecutorAuthorityCode(document.getProsecutorAuthorityCode());
            scanDocument.setVendorReceivedDate(document.getVendorReceivedDate());
            scanDocument.setNotes(document.getNotes());
            scanDocument.setStatus(document.getStatus());
            scanDocument.setStatusUpdatedDate(document.getStatusUpdatedDate());
            return scanDocument;
        }).collect(Collectors.toSet());
    }
}
