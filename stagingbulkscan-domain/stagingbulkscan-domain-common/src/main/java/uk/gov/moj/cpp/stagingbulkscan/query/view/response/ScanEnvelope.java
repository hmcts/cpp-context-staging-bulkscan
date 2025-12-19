package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanEnvelope {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("zip_file_name")
    private String zipFileName;

    @JsonProperty("extracted_date")
    private ZonedDateTime extractedDate;

    @JsonProperty("notes")
    private String notes;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(final String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public ZonedDateTime getExtractedDate() {
        return extractedDate;
    }

    public void setExtractedDate(final ZonedDateTime extractedDate) {
        this.extractedDate = extractedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "ScanEnvelope{" +
                "id=" + id +
                ", zipFileName='" + zipFileName + '\'' +
                ", ZonedDateTime=" + extractedDate +
                '}';
    }
}
