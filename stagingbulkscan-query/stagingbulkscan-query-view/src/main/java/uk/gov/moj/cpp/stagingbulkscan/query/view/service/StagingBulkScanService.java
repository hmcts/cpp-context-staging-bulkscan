package uk.gov.moj.cpp.stagingbulkscan.query.view.service;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelopeDocument;
import uk.gov.moj.cpp.stagingbulkscan.azure.core.service.BlobClientProvider;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanSnapshotKey;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetDocumentResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetThumbnailResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.Thumbnail;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanEnvelopeRepository;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class StagingBulkScanService {

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    @Inject
    private ScanEnvelopeRepository scanEnvelopeRepository;

    @Inject
    private BlobClientProvider blobClientProvider;

    public ScanDocumentsResponse getScanDocumentsResponseByStatus(final List<DocumentStatus> statuses) {
        List<ScanDocument> documentsByStatus;

        if(statuses.isEmpty()) {
            throw new IllegalArgumentException("Document status list is empty.");
        }

        if (statuses.size() == 1 && statuses.get(0).equals(MANUALLY_ACTIONED)) {
            documentsByStatus = scanDocumentRepository.findAllDocumentsByStatusByDesc(MANUALLY_ACTIONED);
        } else {
            documentsByStatus = scanDocumentRepository.findAllDocumentsByStatusByAsc(statuses);
        }

        final ScanDocumentsResponse documents = new ScanDocumentsResponse();

        documents.setScanDocuments(documentsByStatus.stream().map(populateDocument()).collect(Collectors.toList()));

        return documents;
    }

    public GetDocumentResponse getDocumentResponse(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        final ScanEnvelope scanEnvelope = scanEnvelopeRepository.findBy(scanEnvelopeId);
        final Set<ScanDocument> scanDocuments = scanEnvelope.getAssociatedScanDocuments();

        final List<Thumbnail> thumbnailList = scanDocuments.stream()
                .filter(document -> !document.getId().getId().equals(scanDocumentId))
                .filter(document -> !document.isDeleted())
                .map(document -> new Thumbnail(document.getId().getId(), document.getDocumentFileName()))
                .collect(Collectors.toList());

        return scanDocuments.stream()
                .filter(document -> document.getId().getId().equals(scanDocumentId))
                .map(document -> {
                    final GetDocumentResponse documentResponse = new GetDocumentResponse();
                    documentResponse.setScanEnvelopeId(scanEnvelopeId);
                    documentResponse.setId(scanDocumentId);
                    documentResponse.setContent(getPdfEncodedContent(scanEnvelope, document));
                    documentResponse.setStatus(document.getStatus());
                    documentResponse.setDocumentFileName(document.getDocumentFileName());
                    if (!thumbnailList.isEmpty()) {
                        documentResponse.setThumbnails(thumbnailList);
                    }
                    return documentResponse;
                })
                .findFirst().orElse(new GetDocumentResponse());
    }

    public uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument getScanDocumentById(final UUID scanEnvelopeId, final UUID scanDocumentId) {

        final ScanSnapshotKey snapshotKey = new ScanSnapshotKey(scanDocumentId, scanEnvelopeId);

        final ScanDocument scanDocument = scanDocumentRepository.findBy(snapshotKey);

        return buildScanDocument(scanDocument);

    }

    public GetThumbnailResponse getThumbnailResponse(final UUID scanEnvelopeId, final UUID scanDocumentId) {
        final ScanEnvelope scanEnvelope = scanEnvelopeRepository.findBy(scanEnvelopeId);

        final Optional<ScanDocument> scanDocument = scanEnvelope.getAssociatedScanDocuments()
                .stream()
                .filter(document -> document.getId().getId().equals(scanDocumentId))
                .findFirst();

        return scanDocument.map(document -> {
                    final GetThumbnailResponse thumbnailResponse = new GetThumbnailResponse();
                    thumbnailResponse.setId(scanDocumentId);
                    thumbnailResponse.setContent(getImageEncodedContent(scanEnvelope, document));
                    return thumbnailResponse;
                }
        ).orElse(new GetThumbnailResponse());
    }

    private String getImageEncodedContent(final ScanEnvelope scanEnvelope, final ScanDocument scanDocument) {

        final char dot = '.';

        final String zipFileName = scanEnvelope.getZipFileName();

        final int envelopeIndex = zipFileName.contains(Character.toString(dot)) ? zipFileName.indexOf(dot) : zipFileName.length();

        final String fileName = zipFileName.substring(0, envelopeIndex);

        final String pdfDocumentFileName = scanDocument.getDocumentFileName();

        final int pdfIndex = pdfDocumentFileName.contains(Character.toString(dot)) ? pdfDocumentFileName.indexOf(dot) : pdfDocumentFileName.length();

        final String pdfFileName = pdfDocumentFileName.substring(0, pdfIndex);

        final byte[] content = blobClientProvider.getBlobContent(fileName, pdfFileName + ".png");

        return new String(Base64.getEncoder().encode(content));

    }

    private String getPdfEncodedContent(final ScanEnvelope scanEnvelope, final ScanDocument scanDocument) {

        final char dot = '.';

        final String zipFileName = scanEnvelope.getZipFileName();

        final int index = zipFileName.contains(Character.toString(dot)) ? zipFileName.indexOf(dot) : zipFileName.length();

        final String fileName = zipFileName.substring(0, index);

        final byte[] content = blobClientProvider.getBlobContent(fileName, scanDocument.getDocumentFileName());

        return new String(Base64.getEncoder().encode(content));

    }

    private Function<ScanDocument, uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument> populateDocument() {
        return (this::buildScanDocument);
    }

    private uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument buildScanDocument(final ScanDocument doc) {
        final uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument document = new uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument();
        document.setId(doc.getId().getId());
        document.setScanEnvelopeId(doc.getId().getScanEnvelopeId());
        document.setDocumentFileName(doc.getDocumentFileName());
        document.setVendorReceivedDate(doc.getVendorReceivedDate());
        document.setCaseUrn(doc.getCaseUrn());
        document.setCasePTIUrn(doc.getCasePTIUrn());
        document.setProsecutorAuthorityId(doc.getProsecutorAuthorityId());
        document.setProsecutorAuthorityCode(doc.getProsecutorAuthorityCode());
        document.setDocumentName(doc.getDocumentName());
        document.setStatus(doc.getStatus());
        document.setActionedBy(doc.getActionedBy());
        document.setStatusUpdatedDate(doc.getStatusUpdatedDate());
        document.setDeleted(doc.isDeleted());
        document.setStatusCode(doc.getStatusCode());

        return document;
    }

    public ScanEnvelopeDocument getScanEnvelopeDocumentById(final UUID documentId, final UUID envelopeId) {
        final ScanSnapshotKey scanSnapshotKey = new ScanSnapshotKey(documentId, envelopeId);
        final ScanDocument scanDocument = scanDocumentRepository.findBy(scanSnapshotKey);
        final ScanEnvelope scanEnvelope = scanEnvelopeRepository.findBy(envelopeId);

        final ScanEnvelopeDocument.Builder builder = new ScanEnvelopeDocument.Builder();

        ofNullable(scanDocument).ifPresent(document -> {
            builder.withCasePTIUrn(document.getCasePTIUrn());
            builder.withActionedBy(document.getActionedBy());
            builder.withDeleted(document.isDeleted());
            builder.withActionedBy(document.getActionedBy());
            builder.withCaseUrn(document.getCaseUrn());
            builder.withDeletedDate(document.getDeletedDate());
            builder.withDocumentFileName(document.getDocumentFileName());
            builder.withDocumentName(document.getDocumentName());
            builder.withNotes(document.getNotes());
            builder.withProsecutorAuthorityCode(document.getProsecutorAuthorityCode());
            builder.withProsecutorAuthorityId(document.getProsecutorAuthorityId());
            builder.withStatus(document.getStatus());
            builder.withStatusCode(document.getStatusCode());
            builder.withStatusUpdatedDate(document.getStatusUpdatedDate());
            builder.withVendorReceivedDate(document.getVendorReceivedDate());
        });

        ofNullable(scanEnvelope).ifPresent(envelope -> {
            builder.withExtractedDate(envelope.getExtractedDate());
            builder.withZipFileName(envelope.getZipFileName());
        });

        return builder
                .withId(documentId)
                .withScanEnvelopeId(envelopeId)
                .build();
    }
}
