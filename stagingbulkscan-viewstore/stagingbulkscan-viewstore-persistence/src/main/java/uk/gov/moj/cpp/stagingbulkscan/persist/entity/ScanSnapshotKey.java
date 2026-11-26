package uk.gov.moj.cpp.stagingbulkscan.persist.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ScanSnapshotKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scan_envelope_id", nullable = false)
    private UUID scanEnvelopeId;

    public ScanSnapshotKey() {
    }

    public ScanSnapshotKey(final UUID id, final UUID scanEnvelopeId) {
        this.id = id;
        this.scanEnvelopeId = scanEnvelopeId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScanEnvelopeId() {
        return scanEnvelopeId;
    }

    public void setScanEnvelopeId(UUID scanEnvelopeId) {
        this.scanEnvelopeId = scanEnvelopeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.scanEnvelopeId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.id, ((ScanSnapshotKey) o).id)
                && Objects.equals(this.scanEnvelopeId, ((ScanSnapshotKey) o).scanEnvelopeId);
    }

    @Override
    public String toString() {
        return "ScanSnapshotKey [id=" + id + ", scanEnvelopeId=" + scanEnvelopeId + "]";
    }
}