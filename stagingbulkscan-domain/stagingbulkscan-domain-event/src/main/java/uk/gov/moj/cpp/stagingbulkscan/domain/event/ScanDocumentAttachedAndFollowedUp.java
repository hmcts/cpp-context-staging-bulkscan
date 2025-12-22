package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("stagingbulkscan.events.document-attached-with-follow-up")
public class ScanDocumentAttachedAndFollowedUp implements Serializable {
    private static final long serialVersionUID = 10L;

    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;
    private final ZonedDateTime statusUpdatedDate;

    @JsonCreator
    public ScanDocumentAttachedAndFollowedUp(
            @JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
            @JsonProperty("scanDocumentId") final UUID scanDocumentId,
            @JsonProperty("statusUpdatedDate") final ZonedDateTime statusUpdatedDate) {

        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.statusUpdatedDate = statusUpdatedDate;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public ZonedDateTime getStatusUpdatedDate() {
        return statusUpdatedDate;
    }
}
