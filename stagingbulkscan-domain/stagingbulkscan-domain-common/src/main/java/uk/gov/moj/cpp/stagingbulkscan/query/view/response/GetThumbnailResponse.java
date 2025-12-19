package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import java.util.UUID;

public class GetThumbnailResponse {

    private UUID id;
    private String content;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
