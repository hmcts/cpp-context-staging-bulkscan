package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssociatedScanDocument {

    private final String caseUrn;
    private final String casePTIUrn;
    private final String prosecutorAuthorityId;
    private final String prosecutorAuthorityCode;
    private final String documentControlNumber;
    private final String documentName;
    private final String scanningDate;
    private final String manualIntervention;
    private final String nextAction;
    private final String nextActionDate;
    private final String fileName;
    private final String notes;
    private final String vendorReceivedDate;
    private final String scanDocumentId;
    private final String status;
    private final String asn;
    private final Plea plea;
    private final Mc100s mc100s;

    @SuppressWarnings("squid:S00107")
    private AssociatedScanDocument(final String caseUrn, final String casePTIUrn, final String prosecutorAuthorityId,
                                  final String prosecutorAuthorityCode, final String documentControlNumber, final String documentName,
                                  final String scanningDate, final String manualIntervention,
                                  final String nextAction, final String nextActionDate, final String fileName,
                                  final String notes, final String vendorReceivedDate, final String scanDocumentId, final String status, final String asn, final Plea plea,
                                   @JsonProperty("mc100s") final Mc100s mc100s) {
        this.caseUrn = caseUrn;
        this.casePTIUrn = casePTIUrn;
        this.prosecutorAuthorityId = prosecutorAuthorityId;
        this.prosecutorAuthorityCode = prosecutorAuthorityCode;
        this.documentControlNumber = documentControlNumber;
        this.documentName = documentName;        
        this.scanningDate = scanningDate;
        this.manualIntervention = manualIntervention;
        this.nextAction = nextAction;
        this.nextActionDate = nextActionDate;
        this.fileName = fileName;
        this.notes = notes;
        this.vendorReceivedDate = vendorReceivedDate;
        this.scanDocumentId = scanDocumentId;
        this.status = status;
        this.asn = asn;
        this.plea = plea;
        this.mc100s = mc100s;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getCasePTIUrn() {
        return casePTIUrn;
    }

    public String getProsecutorAuthorityId() {
        return prosecutorAuthorityId;
    }

    public String getProsecutorAuthorityCode() {
        return prosecutorAuthorityCode;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getScanningDate() {
        return scanningDate;
    }

    public String getManualIntervention() {
        return manualIntervention;
    }

    public String getNextAction() {
        return nextAction;
    }

    public String getNextActionDate() {
        return nextActionDate;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNotes() {
        return notes;
    }

    public String getVendorReceivedDate() {
        return vendorReceivedDate;
    }

    public String getScanDocumentId() {
        return scanDocumentId;
    }

    public String getStatus() {
        return status;
    }

    public String getAsn() {
        return asn;
    }

    public Plea getPlea() {
        return plea;
    }

    public Mc100s getMc100s() {
        return mc100s;
    }

    public static class AssociatedScanDocumentBuilder {
        private String caseUrn;
        private String casePTIUrn;
        private String prosecutorAuthorityId;
        private String prosecutorName;
        private String documentControlNumber;
        private String documentName;       
        private String scanningDate;
        private String manualIntervention;
        private String nextAction;
        private String nextActionDate;
        private String fileName;
        private String notes;
        private String vendorReceivedDate;
        private String scanDocumentId;
        private String status;
        private String asn;
        private Plea plea;
        private Mc100s mc100s;

        public AssociatedScanDocumentBuilder withCaseUrn(String caseUrn) {
            this.caseUrn = caseUrn;
            return this;
        }

        public AssociatedScanDocumentBuilder withCasePTIUrn(String casePTIUrn) {
            this.casePTIUrn = casePTIUrn;
            return this;
        }

        public AssociatedScanDocumentBuilder withProsecutorAuthorityId(String prosecutorAuthorityId) {
            this.prosecutorAuthorityId = prosecutorAuthorityId;
            return this;
        }

        public AssociatedScanDocumentBuilder withProsecutorName(String prosecutorName) {
            this.prosecutorName = prosecutorName;
            return this;
        }

        public AssociatedScanDocumentBuilder withDocumentControlNumber(String documentControlNumber) {
            this.documentControlNumber = documentControlNumber;
            return this;
        }

        public AssociatedScanDocumentBuilder withDocumentName(String documentName) {
            this.documentName = documentName;
            return this;
        }

        public AssociatedScanDocumentBuilder withScanningDate(String scanningDate) {
            this.scanningDate = scanningDate;
            return this;
        }

        public AssociatedScanDocumentBuilder withManualIntervention(String manualIntervention) {
            this.manualIntervention = manualIntervention;
            return this;
        }

        public AssociatedScanDocumentBuilder withNextAction(String nextAction) {
            this.nextAction = nextAction;
            return this;
        }

        public AssociatedScanDocumentBuilder withNextActionDate(String nextActionDate) {
            this.nextActionDate = nextActionDate;
            return this;
        }

        public AssociatedScanDocumentBuilder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public AssociatedScanDocumentBuilder withNotes(String notes) {
            this.notes = notes;
            return this;
        }

        public AssociatedScanDocumentBuilder withVendorReceivedDate(String vendorReceivedDate){
            this.vendorReceivedDate = vendorReceivedDate;
            return this;
        }

        public AssociatedScanDocumentBuilder withScanDocumentId(String scanDocumentId){
            this.scanDocumentId = scanDocumentId;
            return this;
        }

        public AssociatedScanDocumentBuilder withStatus(String status){
            this.status = status;
            return this;
        }

        public AssociatedScanDocumentBuilder withAsn(final String asn) {
            this.asn = asn;
            return this;
        }

        public AssociatedScanDocument createDocuments() {
            return new AssociatedScanDocument(caseUrn, casePTIUrn, prosecutorAuthorityId, prosecutorName, documentControlNumber, documentName, scanningDate, manualIntervention, nextAction, nextActionDate, fileName, notes, vendorReceivedDate, scanDocumentId, status, asn, plea, mc100s);
        }

        public AssociatedScanDocumentBuilder withPlea(final Plea plea) {
            this.plea = plea;
            return this;
        }

        public AssociatedScanDocumentBuilder withMc100s(final Mc100s mc100s) {
            this.mc100s = mc100s;
            return this;
        }
    }

}
