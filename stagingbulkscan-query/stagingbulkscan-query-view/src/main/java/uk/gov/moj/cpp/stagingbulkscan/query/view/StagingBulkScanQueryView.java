package uk.gov.moj.cpp.stagingbulkscan.query.view;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonString;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelopeDocument;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetDocumentResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetThumbnailResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.query.view.service.StagingBulkScanService;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;


@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class StagingBulkScanQueryView {

    private static final String QUERY_GET_ALL_DOCUMENTS_BY_STATUS = "stagingbulkscan.get-all-documents-by-status";
    private static final String QUERY_GET_DOCUMENT = "stagingbulkscan.get-scan-document";
    private static final String QUERY_GET_THUMBNAIL_CONTENT = "stagingbulkscan.get-thumbnail-content";
    private static final String QUERY_GET_DOCUMENT_BY_ID = "stagingbulkscan.get-scan-document-by-id";
    private static final String QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID = "stagingbulkscan.get-scan-envelope-document-by-ids";

    private static final String CASE_URN = "caseUrn";
    private static final String CASE_PTI_URN = "casePtiUrn";

    private static final String FIELD_SCAN_ENVELOPE_ID = "scanEnvelopeId";
    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_STATUS_CODE = "statusCode";
    private static final String DOCUMENT_FILE_NAME = "documentFileName";
    private static final String SCAN_DOCUMENT_STATUSES = "scanDocumentStatuses";

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StagingBulkScanService stagingBulkScanService;

    @Inject
    private ScanDocumentRepository scanDocumentRepository;

    public JsonEnvelope findAllDocumentsByStatus(final JsonEnvelope envelope) {
        final String statusParameter = envelope.payloadAsJsonObject().getString(FIELD_STATUS);

        final List<String> individualStatuses = Arrays.asList(statusParameter.split(","));
        final List<DocumentStatus> convertedDocumentStatuses = individualStatuses.stream().map(DocumentStatus::valueOf).collect(Collectors.toList());
        return enveloper.withMetadataFrom(envelope, QUERY_GET_ALL_DOCUMENTS_BY_STATUS)
                .apply(objectToJsonObjectConverter.convert(stagingBulkScanService.getScanDocumentsResponseByStatus(convertedDocumentStatuses)));
    }


    public JsonEnvelope getScanDocument(final JsonEnvelope envelope) {

        final Optional<UUID> scanEnvelopeIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_ENVELOPE_ID);

        final Optional<UUID> scanDocumentIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_DOCUMENT_ID);

        GetDocumentResponse documentResponse = new GetDocumentResponse();
        if (scanEnvelopeIdOptional.isPresent() && scanDocumentIdOptional.isPresent()) {
            documentResponse = stagingBulkScanService.getDocumentResponse(scanEnvelopeIdOptional.get(), scanDocumentIdOptional.get());
        }

        return enveloper.withMetadataFrom(envelope, QUERY_GET_DOCUMENT).apply(objectToJsonObjectConverter.convert(documentResponse));
    }


    public JsonEnvelope getThumbnailContent(final JsonEnvelope envelope) {
        final Optional<UUID> scanEnvelopeIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_ENVELOPE_ID);
        final Optional<UUID> scanDocumentIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_DOCUMENT_ID);

        GetThumbnailResponse thumbnailResponse = new GetThumbnailResponse();
        if (scanEnvelopeIdOptional.isPresent() && scanDocumentIdOptional.isPresent()) {
            thumbnailResponse = stagingBulkScanService.getThumbnailResponse(scanEnvelopeIdOptional.get(), scanDocumentIdOptional.get());
        }

        return enveloper.withMetadataFrom(envelope, QUERY_GET_THUMBNAIL_CONTENT).apply(objectToJsonObjectConverter.convert(thumbnailResponse));
    }

    public JsonEnvelope getScanDocumentById(final JsonEnvelope envelope) {
        final Optional<UUID> scanEnvelopeIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_ENVELOPE_ID);
        final Optional<UUID> scanDocumentIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_DOCUMENT_ID);

        ScanDocument scanDocument = new ScanDocument();
        if (scanEnvelopeIdOptional.isPresent() && scanDocumentIdOptional.isPresent()) {
            scanDocument = stagingBulkScanService.getScanDocumentById(scanEnvelopeIdOptional.get(), scanDocumentIdOptional.get());
        }

        return enveloper.withMetadataFrom(envelope, QUERY_GET_DOCUMENT_BY_ID).apply(scanDocument);
    }

    public JsonEnvelope getScanEnvelopeDocumentByIds(final JsonEnvelope envelope) {
        final Optional<UUID> scanEnvelopeIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_ENVELOPE_ID);
        final Optional<UUID> scanDocumentIdOptional = getUUID(envelope.payloadAsJsonObject(), FIELD_SCAN_DOCUMENT_ID);

        ScanEnvelopeDocument envelopeDocument = new ScanEnvelopeDocument.Builder().build();
        if (scanDocumentIdOptional.isPresent() && scanEnvelopeIdOptional.isPresent()) {
            envelopeDocument = stagingBulkScanService.getScanEnvelopeDocumentById(scanDocumentIdOptional.get(), scanEnvelopeIdOptional.get());
        }

        return enveloper.withMetadataFrom(envelope, QUERY_GET_SCAN_ENVELOPE_DOCUMENT_BY_ID).apply(envelopeDocument);
    }

    public Envelope getScanDocumentStatusByCaseUrn(final JsonEnvelope envelope) {
        final JsonObject queryPayload = envelope.payloadAsJsonObject();
        final Optional<String> caseUrn = getJsonString(queryPayload, CASE_URN).map(JsonString::getString);
        final Optional<String> casePtiUrn = getJsonString(queryPayload, CASE_PTI_URN).map(JsonString::getString);

        final String qCaseUrn = caseUrn.orElse(null);
        final String qCasePtiUrn = casePtiUrn.orElse(null);

        final List<uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument> scanDocumentStatusList =
                scanDocumentRepository.findScanDocumentStatus(qCaseUrn, qCasePtiUrn);


        final JsonObject responsePayload = buildPayload(scanDocumentStatusList);
        return enveloper.withMetadataFrom(envelope, "stagingbulkscan.get-scan-document-status").apply(responsePayload);

    }

    private JsonObject buildPayload(final List<uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument> scanDocumentStatusList) {

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        for (final uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument scanDocument : scanDocumentStatusList) {

            final JsonObjectBuilder builder = Json.createObjectBuilder();

            ofNullable(scanDocument.getId()).ifPresent(id ->
                    builder.add(FIELD_SCAN_ENVELOPE_ID, String.valueOf(id)));

            ofNullable(scanDocument.getStatus()).ifPresent(status ->
                    builder.add(FIELD_STATUS, String.valueOf(status)));

            ofNullable(scanDocument.getStatusCode()).ifPresent(statusCode ->
                    builder.add(FIELD_STATUS_CODE, String.valueOf(statusCode)));

            ofNullable(scanDocument.getDocumentFileName()).ifPresent(documentFileName ->
                    builder.add(DOCUMENT_FILE_NAME, (documentFileName)));

            arrayBuilder.add(builder);
        }

        return Json.createObjectBuilder()
                .add(SCAN_DOCUMENT_STATUSES, arrayBuilder.build())
                .build();
    }
}