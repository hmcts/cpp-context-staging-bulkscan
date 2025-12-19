package uk.gov.moj.cpp.stagingbulkscan.event.service;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SystemIdMapperService {

    private static final String TARGET_TYPE = "DOCUMENT_SUBMISSION_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public String getScanDocumentReferenceFor(final UUID submissionId) {

        final UUID contextSystemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException("Context user is not available in JNDI configurations"));

        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperClient.findBy(submissionId, TARGET_TYPE, contextSystemUserId);

        return systemIdMapping.map(SystemIdMapping::getSourceId).orElse(null);

    }
}
