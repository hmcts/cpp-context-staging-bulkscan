package uk.gov.moj.cpp.stagingbulkscan.query.view.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.stagingbulkscan.domain.ScanEnvelopeDocument;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.BlobClientProvider;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetDocumentResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetThumbnailResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanEnvelopeRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanServiceTest {

    @Mock
    private ScanEnvelopeRepository scanEnvelopeRepository;

    @Mock
    private ScanDocumentRepository scanDocumentRepository;

    @Mock
    private BlobClientProvider blobClientProvider;

    @InjectMocks
    private StagingBulkScanService stagingBulkScanService;

    @Test
    public void getDocumentResponse() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithOneDocument(scanEnvelopeId, scanDocumentId1);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        when(blobClientProvider.getBlobContent(Mockito.anyString(), Mockito.anyString())).thenReturn(new byte[]{10, 20});

        final GetDocumentResponse getDocumentResponse = stagingBulkScanService.getDocumentResponse(scanEnvelopeId, scanDocumentId1);
        assertNotNull(getDocumentResponse.getContent());
        assertEquals(MANUALLY_ACTIONED, getDocumentResponse.getStatus());
        assertEquals(scanEnvelopeId, getDocumentResponse.getScanEnvelopeId());
        assertEquals(scanDocumentId1, getDocumentResponse.getId());
    }

    @Test
    public void getDocumentResponseWithTwoDocuments() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithTwoDocuments(scanEnvelopeId, scanDocumentId1, scanDocumentId2);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        when(blobClientProvider.getBlobContent(Mockito.anyString(), Mockito.anyString())).thenReturn(new byte[]{10, 20});

        final GetDocumentResponse getDocumentResponse = stagingBulkScanService.getDocumentResponse(scanEnvelopeId, scanDocumentId1);
        assertNotNull(getDocumentResponse.getContent());
        assertEquals(MANUALLY_ACTIONED, getDocumentResponse.getStatus());
        assertEquals(scanEnvelopeId, getDocumentResponse.getScanEnvelopeId());
        assertEquals(scanDocumentId1, getDocumentResponse.getId());
    }

    @Test
    public void getDocumentResponseWithThreeDocumentsWithOneDeleted() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();
        final UUID scanDocumentId3 = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithThreeDocumentsAndOneDeleted(scanEnvelopeId,
                scanDocumentId1,
                scanDocumentId2,
                scanDocumentId3);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        when(blobClientProvider.getBlobContent(Mockito.anyString(), Mockito.anyString())).thenReturn(new byte[]{10, 20});

        final GetDocumentResponse getDocumentResponse = stagingBulkScanService.getDocumentResponse(scanEnvelopeId, scanDocumentId1);
        assertNotNull(getDocumentResponse.getContent());

        assertNotNull(getDocumentResponse.getThumbnails());
        assertEquals(1, getDocumentResponse.getThumbnails().size());
        assertEquals(scanDocumentId3, getDocumentResponse.getThumbnails().get(0).getId());

        assertEquals(MANUALLY_ACTIONED, getDocumentResponse.getStatus());
        assertEquals(scanEnvelopeId, getDocumentResponse.getScanEnvelopeId());
        assertEquals(scanDocumentId1, getDocumentResponse.getId());
    }

    @Test
    public void getDocumentResponseForNotMatchingId() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithTwoDocuments(scanEnvelopeId, scanDocumentId1, scanDocumentId2);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        when(blobClientProvider.getBlobContent(Mockito.anyString(), Mockito.anyString())).thenReturn(new byte[]{10, 20});

        final GetDocumentResponse getDocumentResponse = stagingBulkScanService.getDocumentResponse(scanEnvelopeId, scanDocumentId2);

        assertNotNull(getDocumentResponse.getContent());
        assertEquals(AUTO_ACTIONED, getDocumentResponse.getStatus());
        assertEquals(scanEnvelopeId, getDocumentResponse.getScanEnvelopeId());
        assertEquals(scanDocumentId2, getDocumentResponse.getId());
    }

    @Test
    public void getThumbnailResponse() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithOneDocument(scanEnvelopeId, scanDocumentId);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);
        when(blobClientProvider.getBlobContent(Mockito.anyString(), Mockito.anyString())).thenReturn(new byte[]{10, 20});

        final GetThumbnailResponse getThumbnailResponse =
                stagingBulkScanService.getThumbnailResponse(scanEnvelopeId, scanDocumentId);
        assertNotNull(getThumbnailResponse.getContent());
        assertEquals(scanDocumentId, getThumbnailResponse.getId());
    }

    @Test
    public void getThumbnailResponseWithZeroDocuments() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId = UUID.randomUUID();

        final ScanEnvelope scanEnvelope = getScanEnvelopeWithZeroDocument(scanEnvelopeId);

        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        final GetThumbnailResponse getThumbnailResponse =
                stagingBulkScanService.getThumbnailResponse(scanEnvelopeId, scanDocumentId);
        assertNull(getThumbnailResponse.getId());
    }

    @Test
    public void getDocumentsByStatuses() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();

        final List<ScanDocument> scanDocuments = buildScanDocuments(scanEnvelopeId, scanDocumentId1, scanDocumentId2);

        when(scanDocumentRepository.findAllDocumentsByStatusByDesc(MANUALLY_ACTIONED)).thenReturn(scanDocuments);

        final ScanDocumentsResponse scanDocumentsResponse = stagingBulkScanService.getScanDocumentsResponseByStatus(singletonList(MANUALLY_ACTIONED));
        assertNotNull(scanDocumentsResponse.getScanDocuments());
        assertEquals(MANUALLY_ACTIONED, scanDocumentsResponse.getScanDocuments().get(0).getStatus());
    }

    @Test
    public void getDocumentsByStatusesForFollowup() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();

        final List<ScanDocument> scanDocuments = buildScanDocumentsForFollowUp(scanEnvelopeId, scanDocumentId1, scanDocumentId2);

        when(scanDocumentRepository.findAllDocumentsByStatusByAsc(singletonList(FOLLOW_UP))).thenReturn(scanDocuments);

        final ScanDocumentsResponse scanDocumentsResponse = stagingBulkScanService.getScanDocumentsResponseByStatus(singletonList(FOLLOW_UP));
        assertNotNull(scanDocumentsResponse.getScanDocuments());
        assertEquals(FOLLOW_UP, scanDocumentsResponse.getScanDocuments().get(0).getStatus());
        assertEquals(FOLLOW_UP, scanDocumentsResponse.getScanDocuments().get(1).getStatus());
    }

    @Test
    public void getDocumentByIdResponse() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId = UUID.randomUUID();
        final ScanSnapshotKey snapshotKey = new ScanSnapshotKey(scanDocumentId, scanEnvelopeId);

        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(snapshotKey);
        scanDocument.setDocumentFileName("test.pdf");
        scanDocument.setStatus(MANUALLY_ACTIONED);

        when(scanDocumentRepository.findBy(snapshotKey)).thenReturn(scanDocument);

        final uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument scanDocumentResponse
                = stagingBulkScanService.getScanDocumentById(scanEnvelopeId, scanDocumentId);

        assertEquals(scanDocumentId, scanDocumentResponse.getId());
        assertEquals(scanEnvelopeId, scanDocumentResponse.getScanEnvelopeId());
        assertEquals(MANUALLY_ACTIONED, scanDocumentResponse.getStatus());
        assertEquals("test.pdf", scanDocumentResponse.getDocumentFileName());
    }

    @Test
    public void getScanEnvelopeDocumentByIds() {
        final UUID scanEnvelopeId = UUID.randomUUID();
        final UUID scanDocumentId = UUID.randomUUID();
        final ScanSnapshotKey snapshotKey = new ScanSnapshotKey(scanDocumentId, scanEnvelopeId);

        final ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(snapshotKey);
        scanDocument.setDocumentFileName("test.pdf");
        scanDocument.setStatus(FOLLOW_UP);

        final ScanEnvelope scanEnvelope = new ScanEnvelope();
        scanEnvelope.setId(scanEnvelopeId);
        scanEnvelope.setZipFileName("zippedName");

        when(scanDocumentRepository.findBy(snapshotKey)).thenReturn(scanDocument);
        when(scanEnvelopeRepository.findBy(scanEnvelopeId)).thenReturn(scanEnvelope);

        final ScanEnvelopeDocument envelopeDocument = stagingBulkScanService.getScanEnvelopeDocumentById(scanDocumentId, scanEnvelopeId);

        assertThat(envelopeDocument, notNullValue());
        assertThat(envelopeDocument.getId(), is(scanDocumentId));
        assertThat(envelopeDocument.getScanEnvelopeId(), is(scanEnvelopeId));
        assertThat(envelopeDocument.getStatus(), is(FOLLOW_UP));
        assertThat(envelopeDocument.getDocumentFileName(), is("test.pdf"));
        assertThat(envelopeDocument.getZipFileName(), is("zippedName"));
    }

    private ScanEnvelope getScanEnvelopeWithOneDocument(final UUID scanEnvelopeId,
                                                        final UUID scanDocumentId1) {
        final Set<ScanDocument> documents = new HashSet<>();
        ScanEnvelope scanEnvelope = new ScanEnvelope();
        scanEnvelope.setId(scanEnvelopeId);
        scanEnvelope.setZipFileName("test.zip");

        final ScanDocument scanDocument1 = new ScanDocument();
        scanDocument1.setId(new ScanSnapshotKey(scanDocumentId1, scanEnvelopeId));
        scanDocument1.setDocumentFileName("test.pdf");
        scanDocument1.setStatus(MANUALLY_ACTIONED);

        documents.add(scanDocument1);
        scanEnvelope.setAssociatedScanDocuments(documents);

        return scanEnvelope;
    }

    private ScanEnvelope getScanEnvelopeWithZeroDocument(final UUID scanEnvelopeId) {
        final Set<ScanDocument> documents = new HashSet<>();
        ScanEnvelope scanEnvelope = new ScanEnvelope();
        scanEnvelope.setId(scanEnvelopeId);
        scanEnvelope.setZipFileName("test.zip");

        scanEnvelope.setAssociatedScanDocuments(documents);

        return scanEnvelope;
    }

    private ScanEnvelope getScanEnvelopeWithTwoDocuments(final UUID scanEnvelopeId,
                                                         final UUID scanDocumentId1,
                                                         final UUID scanDocumentId2) {
        final ScanEnvelope scanEnvelope = getScanEnvelopeWithOneDocument(scanEnvelopeId, scanDocumentId1);

        ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(new ScanSnapshotKey(scanDocumentId2, scanEnvelopeId));
        scanDocument.setDocumentFileName("test2.pdf");
        scanDocument.setStatus(AUTO_ACTIONED);

        final Set<ScanDocument> documents = scanEnvelope.getAssociatedScanDocuments();
        documents.add(scanDocument);
        scanEnvelope.setAssociatedScanDocuments(documents);

        return scanEnvelope;
    }

    private ScanEnvelope getScanEnvelopeWithThreeDocumentsAndOneDeleted(final UUID scanEnvelopeId,
                                                                        final UUID scanDocumentId1,
                                                                        final UUID scanDocumentId2,
                                                                        final UUID scanDocumentId3) {
        final ScanEnvelope scanEnvelope = getScanEnvelopeWithOneDocument(scanEnvelopeId, scanDocumentId1);

        ScanDocument scanDocument = new ScanDocument();
        scanDocument.setId(new ScanSnapshotKey(scanDocumentId2, scanEnvelopeId));
        scanDocument.setDocumentFileName("test2.pdf");
        scanDocument.setStatus(AUTO_ACTIONED);
        scanDocument.setDeleted(Boolean.TRUE);

        ScanDocument scanDocument1 = new ScanDocument();
        scanDocument1.setId(new ScanSnapshotKey(scanDocumentId3, scanEnvelopeId));
        scanDocument1.setDocumentFileName("test3.pdf");
        scanDocument1.setStatus(AUTO_ACTIONED);

        final Set<ScanDocument> documents = scanEnvelope.getAssociatedScanDocuments();
        documents.add(scanDocument);
        documents.add(scanDocument1);
        scanEnvelope.setAssociatedScanDocuments(documents);

        return scanEnvelope;
    }

    private List<ScanDocument> buildScanDocuments(final UUID scanEnvelopeId,
                                                  final UUID scanDocumentId1,
                                                  final UUID scanDocumentId2) {
        final ScanDocument scanDocument1 = new ScanDocument();
        scanDocument1.setId(new ScanSnapshotKey(scanDocumentId1, scanEnvelopeId));
        scanDocument1.setDocumentFileName("test2.pdf");
        scanDocument1.setStatus(MANUALLY_ACTIONED);

        final ScanDocument scanDocument2 = new ScanDocument();
        scanDocument2.setId(new ScanSnapshotKey(scanDocumentId2, scanEnvelopeId));
        scanDocument2.setDocumentFileName("test2.pdf");
        scanDocument2.setStatus(AUTO_ACTIONED);

        final List<ScanDocument> documents = new ArrayList<>();
        documents.add(scanDocument1);
        documents.add(scanDocument2);

        return documents;
    }

    private List<ScanDocument> buildScanDocumentsForFollowUp(final UUID scanEnvelopeId,
                                                             final UUID scanDocumentId1,
                                                             final UUID scanDocumentId2) {
        final ScanDocument scanDocument1 = new ScanDocument();
        scanDocument1.setId(new ScanSnapshotKey(scanDocumentId1, scanEnvelopeId));
        scanDocument1.setDocumentFileName("test2.pdf");
        scanDocument1.setStatus(FOLLOW_UP);

        final ScanDocument scanDocument2 = new ScanDocument();
        scanDocument2.setId(new ScanSnapshotKey(scanDocumentId2, scanEnvelopeId));
        scanDocument2.setDocumentFileName("test2.pdf");
        scanDocument2.setStatus(FOLLOW_UP);

        final List<ScanDocument> documents = new ArrayList<>();
        documents.add(scanDocument1);
        documents.add(scanDocument2);

        return documents;
    }
}