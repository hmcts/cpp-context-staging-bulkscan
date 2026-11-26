package uk.gov.moj.cpp.stagingbulkscan.repository;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;

import java.util.UUID;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Repository(forEntity = ScanEnvelope.class)
public abstract class ScanEnvelopeRepository extends AbstractEntityRepository<ScanEnvelope, UUID> {
}
