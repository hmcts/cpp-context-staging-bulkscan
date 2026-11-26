package uk.gov.moj.cpp.stagingbulkscan.persist.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "scan_envelope")
public class ScanEnvelope {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "zip_file_name", nullable = false)
    private String zipFileName;

    @Column(name = "extracted_date", nullable = false)
    private ZonedDateTime extractedDate;

    @Column(name = "notes")
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "scanEnvelope", orphanRemoval = true)
    private Set<ScanDocument> associatedScanDocuments = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public ZonedDateTime getExtractedDate() {
        return extractedDate;
    }

    public void setExtractedDate(ZonedDateTime extractedDate) {
        this.extractedDate = extractedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }


    public Set<ScanDocument> getAssociatedScanDocuments() {
        return associatedScanDocuments;
    }

    public void setAssociatedScanDocuments(final Set<ScanDocument> associatedScanDocuments) {
        this.associatedScanDocuments = associatedScanDocuments;
    }
}
