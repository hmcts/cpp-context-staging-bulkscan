package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import java.util.UUID;

public class Thumbnail {
    private UUID id;
    private String documentFileName;

    public Thumbnail(final UUID id,
                     final String documentFileName) {
        this.id = id;
        this.documentFileName = documentFileName;
    }
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(final String documentFileName) {
        this.documentFileName = documentFileName;
    }
}
