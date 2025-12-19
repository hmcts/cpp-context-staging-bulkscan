package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S00107"})
@Event("stagingbulkscan.events.actioned-document-deleted")
public class ActionedDocumentDeleted implements Serializable {

    private static final long serialVersionUID = 1L;
    private final UUID scanEnvelopeId;
    private final UUID scanDocumentId;
    private final String zipFileName;
    private final String documentFileName;
    private final ZonedDateTime deletedDate;

    @JsonCreator
    public ActionedDocumentDeleted(@JsonProperty("scanEnvelopeId") final UUID scanEnvelopeId,
                                   @JsonProperty("scanDocumentId") final UUID scanDocumentId,
                                   @JsonProperty("zipFileName") final String zipFileName,
                                   @JsonProperty("documentFileName") final String documentFileName,
                                   @JsonProperty("deletedDate") final ZonedDateTime deletedDate) {
        this.scanEnvelopeId = scanEnvelopeId;
        this.scanDocumentId = scanDocumentId;
        this.zipFileName = zipFileName;
        this.documentFileName = documentFileName;
        this.deletedDate = deletedDate;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public UUID getScanDocumentId() {
        return scanDocumentId;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public ZonedDateTime getDeletedDate() {
        return deletedDate;
    }
}