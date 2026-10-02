package uk.gov.moj.cpp.stagingbulkscan.repository;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class ScanDocumentRepository {

    private static final String STATUSES = "statuses";

    @PersistenceContext(unitName = "stagingbulkscan")
    EntityManager entityManager;

    public ScanDocument findBy(final ScanSnapshotKey id) {
        return entityManager.find(ScanDocument.class, id);
    }

    public ScanDocument save(final ScanDocument scanDocument) {
        return entityManager.merge(scanDocument);
    }

    public List<ScanDocument> findAllDocumentsByStatusByAsc(final DocumentStatus status) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.status = :status and deleted is false order by doc.vendorReceivedDate asc",
                        ScanDocument.class)
                .setParameter("status", status)
                .getResultList();
    }

    public List<ScanDocument> findAllDocumentsByStatusByDesc(final DocumentStatus status) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.status = :statuses and deleted is false order by doc.vendorReceivedDate desc",
                        ScanDocument.class)
                .setParameter(STATUSES, status)
                .getResultList();
    }

    public List<ScanDocument> findAllDocumentsByStatusByAsc(final List<DocumentStatus> statuses) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.status in (:statuses) and deleted is false order by doc.vendorReceivedDate asc",
                        ScanDocument.class)
                .setParameter(STATUSES, statuses)
                .getResultList();
    }

    public List<ScanDocument> findAllDocumentsByStatusByDesc(final List<DocumentStatus> statuses) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.status in (:statuses) and deleted is false order by doc.vendorReceivedDate desc",
                        ScanDocument.class)
                .setParameter(STATUSES, statuses)
                .getResultList();
    }

    public List<ScanDocument> findScanDocumentStatus(final String caseUrn, final String casePtiUrn) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.caseUrn is not null and doc.caseUrn =:caseUrn or" +
                                " doc.casePTIUrn is not null and doc.casePTIUrn=:casePtiUrn ",
                        ScanDocument.class)
                .setParameter("caseUrn", caseUrn)
                .setParameter("casePtiUrn", casePtiUrn)
                .getResultList();
    }

    public List<ScanDocument> findDocumentsEligibleForDeletion(final List<DocumentStatus> statuses,
                                                               final ZonedDateTime cutoffDate,
                                                               final int maxResults) {
        return entityManager.createQuery(
                        "select doc from ScanDocument doc where doc.status in (:statuses) " +
                                "and doc.deleted is false " +
                                "and doc.statusUpdatedDate < :cutoffDate " +
                                "order by doc.statusUpdatedDate asc",
                        ScanDocument.class)
                .setParameter(STATUSES, statuses)
                .setParameter("cutoffDate", cutoffDate)
                .setMaxResults(maxResults)
                .getResultList();
    }
}
