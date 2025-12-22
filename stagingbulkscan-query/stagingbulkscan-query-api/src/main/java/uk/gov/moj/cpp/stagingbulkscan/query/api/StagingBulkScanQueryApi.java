package uk.gov.moj.cpp.stagingbulkscan.query.api;

import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.query.view.StagingBulkScanQueryView;

import java.util.Optional;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class StagingBulkScanQueryApi {

    @Inject
    private StagingBulkScanQueryView stagingBulkScanQueryView;

    @Handles("stagingbulkscan.get-all-documents-by-status")
    public JsonEnvelope findAllDocumentsByStatus(final JsonEnvelope query) {
        return this.stagingBulkScanQueryView.findAllDocumentsByStatus(query);
    }

    @Handles("stagingbulkscan.get-scan-document")
    public JsonEnvelope findScanDocument(final JsonEnvelope query) {
        return this.stagingBulkScanQueryView.getScanDocument(query);
    }

    @Handles("stagingbulkscan.get-scan-document-by-id")
    public JsonEnvelope getScanDocumentById(final JsonEnvelope query) {
        return this.stagingBulkScanQueryView.getScanDocumentById(query);
    }

    @Handles("stagingbulkscan.get-scan-envelope-document-by-ids")
    public JsonEnvelope getScanEnvelopeDocumentByIds(final JsonEnvelope query) {
        return this.stagingBulkScanQueryView.getScanEnvelopeDocumentByIds(query);
    }

    @Handles("stagingbulkscan.get-thumbnail-content")
    public JsonEnvelope getThumbnailContent(final JsonEnvelope query) {
        return this.stagingBulkScanQueryView.getThumbnailContent(query);
    }

    @Handles("stagingbulkscan.get-scan-document-status")
    public Envelope getScanDocumentStatusByCaseUrn(final JsonEnvelope query) {
        validateParams(query);
        return this.stagingBulkScanQueryView.getScanDocumentStatusByCaseUrn(query);
    }

    private void validateParams(final JsonEnvelope query) {
        final Optional<String> caseUrn = getString(query.payloadAsJsonObject(), "caseUrn");
        final Optional<String> casePtiUrn = getString(query.payloadAsJsonObject(), "casePtiUrn");
        if (!(caseUrn.isPresent() || casePtiUrn.isPresent())) {
            throw new BadRequestException("CaseUrn or CasePtiUrn must be provided as a parameter");
        }
    }
}
