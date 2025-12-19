package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DefendantDetailsUpdateRequested.EVENT_NAME)
public class DefendantDetailsUpdateRequested implements Serializable {

    public static final String EVENT_NAME = "stagingbulkscan.events.defendant-details-update-requested";

    private static final long serialVersionUID = 1L;

    private final UUID scanEnvelopeId;

    private final UUID scanDocumentId;

    private final String caseUrn;

    @JsonCreator
    public DefendantDetailsUpdateRequested(@JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
                                           @JsonProperty("scanDocumentId") final UUID scanDocumentId,
                                           @JsonProperty("caseUrn") final String caseUrn) {
        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.caseUrn = caseUrn;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }
}
