package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S1700"})
public class ScanDocumentsResponse {
    @JsonProperty("scanDocuments")
    private List<ScanDocument> scanDocuments;

    public List<ScanDocument> getScanDocuments() {
        return scanDocuments;
    }

    public void setScanDocuments(List<ScanDocument> scanDocuments) {
        this.scanDocuments = scanDocuments;
    }
}
