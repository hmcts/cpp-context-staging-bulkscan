package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("stagingbulkscan.events.mark-as-actioned")
public class ScanDocumentManuallyActioned implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;
    private final UUID actionedBy;
    private final ZonedDateTime actionedDate;

    @JsonCreator
    public ScanDocumentManuallyActioned(@JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
                                        @JsonProperty("scanDocumentId") final UUID scanDocumentId,
                                        @JsonProperty("actionedBy") final UUID actionedBy,
                                        @JsonProperty("actionedDate") final ZonedDateTime actionedDate) {
        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.actionedBy = actionedBy;
        this.actionedDate = actionedDate;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public UUID getActionedBy() {
        return actionedBy;
    }

    public ZonedDateTime getActionedDate() {
        return actionedDate;
    }
}
