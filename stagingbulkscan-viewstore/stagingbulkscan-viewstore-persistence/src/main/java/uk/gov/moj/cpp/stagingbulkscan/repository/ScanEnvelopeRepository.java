package uk.gov.moj.cpp.stagingbulkscan.repository;

import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class ScanEnvelopeRepository {

    @PersistenceContext(unitName = "stagingbulkscan")
    EntityManager entityManager;

    public ScanEnvelope findBy(final UUID id) {
        return entityManager.find(ScanEnvelope.class, id);
    }

    public ScanEnvelope save(final ScanEnvelope scanEnvelope) {
        return entityManager.merge(scanEnvelope);
    }
}
