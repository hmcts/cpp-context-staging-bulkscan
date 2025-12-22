package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;

import java.util.List;
import java.util.UUID;

public class GetDocumentResponse {

    private UUID scanEnvelopeId;
    private UUID id;
    private DocumentStatus status;
    private String content;
    private String documentFileName;
    private List<Thumbnail> thumbnails;


    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public void setScanEnvelopeId(final UUID scanEnvelopeId) {
        this.scanEnvelopeId = scanEnvelopeId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(final String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(final List<Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }
}
