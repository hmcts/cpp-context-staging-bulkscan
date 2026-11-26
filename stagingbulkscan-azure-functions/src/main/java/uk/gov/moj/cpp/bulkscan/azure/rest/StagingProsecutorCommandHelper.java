package uk.gov.moj.cpp.bulkscan.azure.rest;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static uk.gov.moj.cpp.bulkscan.azure.rest.DocumentMapper.getDocumentMapper;

import java.io.InputStream;
import java.util.function.BiPredicate;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

public class StagingProsecutorCommandHelper {
    private static final String DOCUMENT_REFERENCE_PATTERN = "%s:%s";
    private final ClientWrapper clientWrapper = new ClientWrapper();

    public Response addMaterial(final JsonObject jsonInput, final String scanEnvelopeId, final InputStream inputStream) {
        final String casePtiUrnStr = "case_ptiurn";
        final String caseUrnStr = "case_urn";
        final Entity<MultipartFormDataOutput> entity = buildMultipartFormDataOutputEntity(jsonInput, scanEnvelopeId, inputStream);
        final MultivaluedMap<String, Object> headers = getHeaders(jsonInput);

        final BiPredicate<JsonObject, String> predicate = (jsonObject, parameter) -> jsonInput.containsKey(parameter);

        if(predicate.test(jsonInput, caseUrnStr)) {
            final String caseUrn = jsonInput.getString(caseUrnStr);
            final String prosecutorId = jsonInput.getString("prosecutor_ID");
            return getClient().target(getStagingProsecutorMaterialUploadUrlWithCaseUrn(caseUrn, prosecutorId)).request().headers(headers).post(entity);
        }

        if(predicate.test(jsonInput, casePtiUrnStr)) {
            final String casePtiUrn = jsonInput.getString(casePtiUrnStr);
            return getClient().target(getStagingProsecutorMaterialUploadUrlWithPtiUrn(casePtiUrn)).request().headers(headers).post(entity);
        }

        return Response.status(404, "Both case urn & case pti urn are missing.").build();
    }

    private MultivaluedMap<String, Object> getHeaders(final JsonObject jsonInput) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-type", MediaType.MULTIPART_FORM_DATA_TYPE);
        headers.add("CJSCPPUID", getCPPUID());
        headers.add("Content-Disposition", "filename=" + jsonInput.getString("file_name"));
        return headers;
    }

    private Entity<MultipartFormDataOutput> buildMultipartFormDataOutputEntity(final JsonObject jsonInput, final String scanEnvelopeId, final InputStream inputStream) {
        final String scanDocumentId = jsonInput.getString("scanDocumentId");
        final String documentName = jsonInput.getString("document_name");
        final MultipartFormDataOutput multipartFormDataOutput = new MultipartFormDataOutput();
        multipartFormDataOutput.addFormData("material", inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE, jsonInput.getString("file_name"));
        multipartFormDataOutput.addFormData("materialType", getDocumentMapper().get(documentName), MediaType.TEXT_PLAIN_TYPE);
        if(!jsonInput.containsKey("case_ptiurn")) {
            final String prosecutorId = jsonInput.getString("prosecutor_ID");
            multipartFormDataOutput.addFormData("prosecutingAuthority", prosecutorId, MediaType.TEXT_PLAIN_TYPE);
        }
        multipartFormDataOutput.addFormData("srcDocumentReference", getDocumentReference(scanEnvelopeId, scanDocumentId), MediaType.TEXT_PLAIN_TYPE);
        if (jsonInput.containsKey("asn")) {
            multipartFormDataOutput.addFormData("defendantId", jsonInput.getString("asn"), MediaType.TEXT_PLAIN_TYPE);
        }
        return Entity.entity(multipartFormDataOutput, MediaType.MULTIPART_FORM_DATA_TYPE);
    }

    public Client getClient() {
        return clientWrapper.getClient();
    }

    public String getCPPUID() {
        return getenv("staging-prosecutor-user");
    }

    protected String getStagingProsecutorMaterialUploadUrlWithCaseUrn(final String caseUrn, final String ouCode) {
        return format(getenv("attach_document_to_case_urn_url"), caseUrn, ouCode);
    }

    protected String getStagingProsecutorMaterialUploadUrlWithPtiUrn(final String casePtiUrn) {
        return format(getenv("attach_document_to_case_pti_urn_url"), casePtiUrn);
    }

    private String getDocumentReference(final String scanEnvelopeId, final String scanDocumentId) {
        return format(DOCUMENT_REFERENCE_PATTERN, scanEnvelopeId, scanDocumentId);
    }
}
