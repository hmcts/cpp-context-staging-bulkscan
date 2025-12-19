package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("stagingbulkscan.events.document-next-step-paused")
public class DocumentNextStepPaused implements Serializable {
    private static final long serialVersionUID = 10L;

    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;
    private final Boolean isSjp;

    @JsonCreator
    public DocumentNextStepPaused(
            @JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
            @JsonProperty("scanDocumentId") final UUID scanDocumentId,
            @JsonProperty("isSjp") final Boolean isSjp) {

        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.isSjp = isSjp;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public Boolean getIsSjp() {
        return isSjp;
    }

}
