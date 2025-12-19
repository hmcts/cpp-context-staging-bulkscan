package uk.gov.moj.cpp.stagingbulkscan.repository;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;

import java.util.List;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Repository
public abstract class ScanDocumentRepository extends AbstractEntityRepository<ScanDocument, ScanSnapshotKey> {

    @Query(value = "from ScanDocument doc where doc.status = :status and deleted is false order by doc.vendorReceivedDate asc")
    public abstract List<ScanDocument> findAllDocumentsByStatusByAsc(@QueryParam("status") final DocumentStatus status);

    @Query(value = "from ScanDocument doc where doc.status = :statuses and deleted is false order by doc.vendorReceivedDate desc")
    public abstract List<ScanDocument> findAllDocumentsByStatusByDesc(@QueryParam("statuses") final DocumentStatus status);

    @Query(value = "from ScanDocument doc where doc.status in :statuses and deleted is false order by doc.vendorReceivedDate asc")
    public abstract List<ScanDocument> findAllDocumentsByStatusByAsc(@QueryParam("statuses") final List<DocumentStatus> statuses);

    @Query(value = "from ScanDocument doc where doc.status in :statuses and deleted is false order by doc.vendorReceivedDate desc")
    public abstract List<ScanDocument> findAllDocumentsByStatusByDesc(@QueryParam("statuses") final List<DocumentStatus> statuses);

    @Query(value = "from ScanDocument doc where doc.caseUrn is not null and doc.caseUrn =:caseUrn or" +
            " doc.casePTIUrn is not null and doc.casePTIUrn=:casePtiUrn ")
    public abstract List<ScanDocument> findScanDocumentStatus(@QueryParam("caseUrn") final String
                                                                          caseUrn,@QueryParam("casePtiUrn") final String casePtiUrn);
}