package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicScanDocumentActioned {
    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;

    @JsonCreator
    public PublicScanDocumentActioned(final @JsonProperty("scanEnvelopeId") UUID scanEnvelopeId,
                                final @JsonProperty("scanDocumentId") UUID scanDocumentId) {
        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }
}
