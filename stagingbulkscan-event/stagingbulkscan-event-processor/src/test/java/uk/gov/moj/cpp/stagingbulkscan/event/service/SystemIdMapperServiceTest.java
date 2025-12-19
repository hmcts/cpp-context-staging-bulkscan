package uk.gov.moj.cpp.stagingbulkscan.event.service;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMapperServiceTest {
    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private SystemIdMapperService systemIdMapperService;

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {

        final UUID submissionId = randomUUID();
        final UUID userId = randomUUID();
        final String mappedDocumentReference = "documentReference";

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), mappedDocumentReference, "BS_DOCUMENT_REFERENCE", submissionId, "DOCUMENT_SUBMISSION_ID", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(submissionId, "DOCUMENT_SUBMISSION_ID", userId)).thenReturn(Optional.of(systemIdMapping));

        final String documentReference = systemIdMapperService.getScanDocumentReferenceFor(submissionId);

        assertThat(documentReference, is(mappedDocumentReference));
    }

    public void shouldReturnNullWhenCaseIdNotMappingExists() {

        final UUID submissionId = randomUUID();
        final UUID userId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(submissionId, "DOCUMENT_SUBMISSION_ID", userId)).thenReturn(Optional.empty());

        final String documentReference = systemIdMapperService.getScanDocumentReferenceFor(submissionId);

        assertNull(documentReference);
    }
}
