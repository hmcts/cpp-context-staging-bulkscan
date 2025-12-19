package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderPayload {

    private String scanEnvelopeId;
    private String envelopeClassification;
    private String vendorPOBox;
    private String jurisdiction;
    private String vendorOpeningDate;
    private String zipFileCreatedDate;
    private String zipFileName;
    private List<AssociatedScanDocument> associatedScanDocuments;
    private String notes;
    private String extractedDate;

    @SuppressWarnings("squid:S00107")
    private ProviderPayload(final String envelopeClassification, final String vendorPOBox, final String jurisdiction,
                           final String vendorOpeningDate, final String zipFileCreatedDate,
                           final String zipFileName, final List<AssociatedScanDocument>  associatedScanDocuments, final String notes,
                           final String extractedDate, final String scanEnvelopeId) {
        this.envelopeClassification = envelopeClassification;
        this.vendorPOBox = vendorPOBox;
        this.jurisdiction = jurisdiction;
        this.vendorOpeningDate = vendorOpeningDate;
        this.zipFileCreatedDate = zipFileCreatedDate;
        this.zipFileName = zipFileName;
        this.associatedScanDocuments = associatedScanDocuments;
        this.notes = notes;
        this.extractedDate = extractedDate;
        this.scanEnvelopeId = scanEnvelopeId;
    }

    public String getEnvelopeClassification() {
        return envelopeClassification;
    }

    public String getVendorPOBox() {
        return vendorPOBox;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getVendorOpeningDate() {
        return vendorOpeningDate;
    }

    public String getZipFileCreatedDate() {
        return zipFileCreatedDate;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public List<AssociatedScanDocument> getAssociatedScanDocuments() {
        return associatedScanDocuments;
    }

    public String getNotes() {
        return notes;
    }

    public String getExtractedDate() {
        return extractedDate;
    }

    public String getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public static class ProviderPayloadBuilder {
        private String envelopeClassification;
        private String vendorPOBox;
        private String jurisdiction;
        private String vendorOpeningDate;
        private String zipFileCreatedDate;
        private String zipFileName;
        private List<AssociatedScanDocument>  associatedScanDocuments;
        private String notes;
        private String extractedDate;
        private String scanEnvelopeId;

        public ProviderPayloadBuilder withEnvelopeClassifcation(String envelopeClassifcation) {
            this.envelopeClassification = envelopeClassifcation;
            return this;
        }

        public ProviderPayloadBuilder withPoBox(final String poBox) {
            this.vendorPOBox = poBox;
            return this;
        }

        public ProviderPayloadBuilder withJurisdiction(final String jurisdiction) {
            this.jurisdiction = jurisdiction;
            return this;
        }

        public ProviderPayloadBuilder withVendorOpeningDate(final String vendorOpeningDate) {
            this.vendorOpeningDate = vendorOpeningDate;
            return this;
        }

        public ProviderPayloadBuilder withZipFileCreationDate(final String zipFileCreationDate) {
            this.zipFileCreatedDate = zipFileCreationDate;
            return this;
        }

        public ProviderPayloadBuilder withZipFileName(final String zipFileName) {
            this.zipFileName = zipFileName;
            return this;
        }

        public ProviderPayloadBuilder withAssociatedScanDocuments(final List<AssociatedScanDocument>  associatedScanDocuments) {
            this.associatedScanDocuments = associatedScanDocuments;
            return this;
        }

        public ProviderPayloadBuilder withNotes(String notes) {
            this.notes = notes;
            return this;
        }

        public ProviderPayloadBuilder withExtractedDate(final String extractedDate) {
            this.extractedDate = extractedDate;
            return this;
        }

        public ProviderPayloadBuilder withScanEnvelopeId(final String scanEnvelopeId) {
            this.scanEnvelopeId = scanEnvelopeId;
            return this;
        }

        public ProviderPayload createProviderPayload() {
            return new ProviderPayload(envelopeClassification, vendorPOBox, jurisdiction, vendorOpeningDate, zipFileCreatedDate, zipFileName, associatedScanDocuments, notes, extractedDate, scanEnvelopeId);
        }
    }
}
