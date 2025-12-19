package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("stagingbulkscan.events.document-next-step-decided")
public class DocumentNextStepDecided implements Serializable {
    private static final long serialVersionUID = 10L;

    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;
    private final String caseUrn;
    private final Boolean isSjp;

    @JsonCreator
    public DocumentNextStepDecided(
            @JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
            @JsonProperty("scanDocumentId") final UUID scanDocumentId,
            @JsonProperty("caseUrn") final String caseUrn,
            @JsonProperty("isSjp") final Boolean isSjp) {

        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.caseUrn = caseUrn;
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

    public String getCaseUrn() {
        return caseUrn;
    }
}
