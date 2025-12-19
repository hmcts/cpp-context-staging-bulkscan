package uk.gov.moj.cpp.stagingbulkscan.query.view.response;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanDocument {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("scanEnvelopeId")
    private UUID scanEnvelopeId;

    @JsonProperty("documentFileName")
    private String documentFileName;

    @JsonProperty("vendorReceivedDate")
    private ZonedDateTime vendorReceivedDate;

    @JsonProperty("caseUrn")
    private String caseUrn;

    @JsonProperty("casePTIUrn")
    private String casePTIUrn;

    @JsonProperty("prosecutorAuthorityId")
    private String prosecutorAuthorityId;

    @JsonProperty("prosecutorAuthorityCode")
    private String prosecutorAuthorityCode;

    @JsonProperty("documentName")
    private String documentName;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("status")
    private DocumentStatus status;

    @JsonProperty("actionedBy")
    private UUID actionedBy;

    @JsonProperty("statusUpdatedDate")
    private ZonedDateTime statusUpdatedDate;

    @JsonProperty("deleted")
    private boolean deleted;

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("drivingLicenceNumber")
    private String drivingLicenceNumber;

    @JsonProperty("statusCode")
    private StatusCode statusCode;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public String getDocumentEmail() {
        return this.emailAddress;
    }

    public void setDocumentEmail(final String email) {
        this.emailAddress = email;
    }

    public String getDrivingLicenceNumber() {
        return this.drivingLicenceNumber;
    }

    public void setScanEnvelopeId(final UUID scanEnvelopeId) {
        this.scanEnvelopeId = scanEnvelopeId;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(final String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public ZonedDateTime getVendorReceivedDate() {
        return vendorReceivedDate;
    }

    public void setVendorReceivedDate(final ZonedDateTime vendorReceivedDate) {
        this.vendorReceivedDate = vendorReceivedDate;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public String getCasePTIUrn() {
        return casePTIUrn;
    }

    public void setCasePTIUrn(final String casePTIUrn) {
        this.casePTIUrn = casePTIUrn;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(final String documentName) {
        this.documentName = documentName;
    }

    public String getProsecutorAuthorityId() {
        return prosecutorAuthorityId;
    }

    public void setProsecutorAuthorityId(final String prosecutorAuthorityId) {
        this.prosecutorAuthorityId = prosecutorAuthorityId;
    }

    public String getProsecutorAuthorityCode() {
        return prosecutorAuthorityCode;
    }

    public void setProsecutorAuthorityCode(final String prosecutorAuthorityCode) {
        this.prosecutorAuthorityCode = prosecutorAuthorityCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public UUID getActionedBy() {
        return actionedBy;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public void setActionedBy(final UUID actionedBy) {
        this.actionedBy = actionedBy;
    }

    public ZonedDateTime getStatusUpdatedDate() {
        return statusUpdatedDate;
    }

    public void setStatusUpdatedDate(ZonedDateTime statusUpdatedDate) {
        this.statusUpdatedDate = statusUpdatedDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setDrivingLicenceNumber(final String drivingLicenceNumber) {
        this.drivingLicenceNumber = drivingLicenceNumber;
    }

    @Override
    public String toString() {
        return "ScanDocument{" +
                "id=" + id +
                ", scanEnvelopeId=" + scanEnvelopeId +
                ", documentFileName='" + documentFileName + '\'' +
                ", vendorReceivedDate=" + vendorReceivedDate +
                ", caseUrn='" + caseUrn + '\'' +
                ", casePTIUrn='" + casePTIUrn + '\'' +
                ", prosecutorAuthorityId='" + prosecutorAuthorityId + '\'' +
                ", prosecutorAuthorityCode='" + prosecutorAuthorityCode + '\'' +
                ", documentName='" + documentName + '\'' +
                ", notes='" + notes + '\'' +
                ", status=" + status +
                ", actionedBy=" + actionedBy +
                ", statusUpdatedDate=" + statusUpdatedDate +
                ", deleted=" + deleted +
                ", emailAddress='" + emailAddress + '\'' +
                ", drivingLicenceNumber='" + drivingLicenceNumber + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
