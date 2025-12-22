package uk.gov.moj.cpp.stagingbulkscan.persist.entity;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "scan_document")
public class ScanDocument {

    @EmbeddedId
    private ScanSnapshotKey id;

    @ManyToOne
    @JoinColumn(name = "scan_envelope_id", insertable = false, updatable = false)
    private ScanEnvelope scanEnvelope;

    @Column(name = "document_file_name", nullable = false)
    private String documentFileName;

    @Column(name = "vendor_received_date", nullable = false)
    private ZonedDateTime vendorReceivedDate;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "case_pti_urn")
    private String casePTIUrn;

    @Column(name = "prosecutor_authority_id")
    private String prosecutorAuthorityId;

    @Column(name = "prosecutor_authority_code")
    private String prosecutorAuthorityCode;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DocumentStatus status;

    @Column(name = "actioned_by")
    private UUID actionedBy;

    @Column(name = "status_updated_date")
    private ZonedDateTime statusUpdatedDate;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "deleted_date")
    private ZonedDateTime deletedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code")
    private StatusCode statusCode;

    public ScanDocument() {
        //For JPA
    }

    public ScanSnapshotKey getId() {
        return id;
    }

    public void setId(final ScanSnapshotKey id) {
        this.id = id;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public ZonedDateTime getVendorReceivedDate() {
        return vendorReceivedDate;
    }

    public void setVendorReceivedDate(ZonedDateTime vendorReceivedDate) {
        this.vendorReceivedDate = vendorReceivedDate;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public String getCasePTIUrn() {
        return casePTIUrn;
    }

    public void setCasePTIUrn(String casePTIUrn) {
        this.casePTIUrn = casePTIUrn;
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

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public UUID getActionedBy() {
        return actionedBy;
    }

    public void setActionedBy(UUID actionedBy) {
        this.actionedBy = actionedBy;
    }

    public ZonedDateTime getStatusUpdatedDate() {
        return statusUpdatedDate;
    }

    public void setStatusUpdatedDate(ZonedDateTime statusUpdatedDate) {
        this.statusUpdatedDate = statusUpdatedDate;
    }

    public ScanEnvelope getScanEnvelope() {
        return scanEnvelope;
    }

    public void setScanEnvelope(final ScanEnvelope scanEnvelope) {
        this.scanEnvelope = scanEnvelope;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public ZonedDateTime getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(final ZonedDateTime deletedDate) {
        this.deletedDate = deletedDate;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
