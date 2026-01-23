package uk.gov.moj.cpp.bulkscan.azure.rest;

import static java.lang.System.getenv;
import static java.time.ZonedDateTime.now;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.bulkscan.azure.rest.AssociatedScanDocument.AssociatedScanDocumentBuilder;
import static uk.gov.moj.cpp.bulkscan.azure.rest.DocumentMapper.getDocumentMapper;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.microsoft.azure.functions.ExecutionContext;

@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S2139", "squid:S3776"})
public class StagingBulkScanCommandHelper {

    private static final String SCANNABLE_ITEMS = "scannable_items";
    private static final String CASE_URN = "case_urn";
    private static final String CASE_NUMBER = "case_number";
    private static final String CASE_PTIURN = "case_ptiurn";
    private static final String PROSECUTOR_ID = "prosecutor_ID";
    private static final String FILE_NAME = "file_name";
    private static final String DOCUMENT_NAME = "document_name";
    private static final String SHORT_NAME = "shortName";
    private static final String EMPTY = "";
    private static final String DOCUMENT_CONTROL_NUMBER = "document_control_number";
    private static final String MANUAL_INTERVENTION = "manual_intervention";
    private static final String NEXT_ACTION = "next_action";
    private static final String NEXT_ACTION_DATE = "next_action_date";
    private static final String NOTES = "notes";
    private static final String SCANNING_DATE = "scanning_date";
    private static final String ASN = "asn";
    private static final String OUCODE = "oucode";
    private static final String OCR_DATA = "ocr_data";
    private static final String SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String ENVELOPE_CLASSIFICATION = "envelope_classification";
    private static final String JURISDICTION = "jurisdiction";
    private static final String OPENING_DATE = "opening_date";
    private static final String PO_BOX = "po_box";
    private static final String ZIP_FILE_CREATEDDATE = "zip_file_createddate";
    private static final String ZIP_FILE_NAME = "zip_file_name";
    private static final String DELIVERY_DATE = "delivery_date";
    private static final String SCAN_ENVELOPE_ID = "scanEnvelopeId";
    private final ReferenceDataQueryHelper referenceDataQueryHelper;
    private final ExecutionContext context;
    private final ClientWrapper clientWrapper = new ClientWrapper();

    public StagingBulkScanCommandHelper(final ExecutionContext context, final ReferenceDataQueryHelper referenceDataQueryHelper) {
        this.context = context;
        this.referenceDataQueryHelper = referenceDataQueryHelper;
    }

    public JsonObject registerEnvelope(final JsonObject jsonInput) {
        final JsonObjectBuilder enrichedEnvelopeBuilder = createObjectBuilderWithFilter(jsonInput, field -> !field.equals(SCANNABLE_ITEMS));
        final ProviderPayload commandPayload = convertPayload(jsonInput, enrichedEnvelopeBuilder);
        final Entity<ProviderPayload> entity = Entity.entity(commandPayload, getBulkScanAPIContentType());
        final Client client = getClient();
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-type", "application/json");
        headers.add("CJSCPPUID", getCPPUID());
        final Response response = client.target(getBulkScanAPIUrl()).request().headers(headers).post(entity);
        return enrichedEnvelopeBuilder.add("responseCode", response.getStatus()).build();
    }

    private ProviderPayload convertPayload(final JsonObject payload, final JsonObjectBuilder envelopeBuilder) {
        final List<AssociatedScanDocument> associatedScanDocuments = new ArrayList<>();
        final JsonArray scannedDocuments = payload.getJsonArray(SCANNABLE_ITEMS);
        final List<JsonObject> jsonObjects = scannedDocuments.getValuesAs(JsonObject.class);
        final UUID scanEnvelopeId = UUID.randomUUID();
        final String envClassification = payload.getString(ENVELOPE_CLASSIFICATION, EMPTY);
        final String jurisdiction = payload.getString(JURISDICTION, EMPTY);
        final String notes = payload.getString(NOTES, EMPTY);
        final String openingDate = payload.getString(OPENING_DATE, EMPTY);
        final String poBox = payload.getString(PO_BOX, EMPTY);
        final String createdDate = payload.getString(ZIP_FILE_CREATEDDATE, EMPTY);
        final String zipFileName = payload.getString(ZIP_FILE_NAME, EMPTY);
        final String deliveryDate = payload.getString(DELIVERY_DATE, EMPTY);

        envelopeBuilder.add(SCAN_ENVELOPE_ID, scanEnvelopeId.toString());
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        jsonObjects.forEach(json -> {
            final JsonObjectBuilder document = createObjectBuilder();
            associatedScanDocuments.add(buildAssociatedScanDocument(deliveryDate, json, document));
            arrayBuilder.add(document);
        });
        envelopeBuilder.add(SCANNABLE_ITEMS, arrayBuilder);

        ProviderPayload.ProviderPayloadBuilder payloadBuilder = new ProviderPayload.ProviderPayloadBuilder();
        payloadBuilder.withScanEnvelopeId(scanEnvelopeId.toString());
        setEnvelopeClassifcation(envClassification, payloadBuilder);
        setJurisdiction(jurisdiction, payloadBuilder);
        setNotes(notes, payloadBuilder);
        setVendorOpeningDate(openingDate, payloadBuilder);
        setPoBox(poBox, payloadBuilder);
        setZipFileCreationDate(createdDate, payloadBuilder);
        setZipFileName(zipFileName, payloadBuilder);
        payloadBuilder.withExtractedDate(now().format(DateTimeFormatter.ISO_INSTANT));
        payloadBuilder.withAssociatedScanDocuments(associatedScanDocuments);
        return payloadBuilder.createProviderPayload();
    }

    private AssociatedScanDocument buildAssociatedScanDocument(final String deliveryDate, final JsonObject json, final JsonObjectBuilder documentBuilder) {
        final String caseUrn = json.getString(CASE_NUMBER, EMPTY);
        final String prosecutorId = json.getString(PROSECUTOR_ID, EMPTY);
        final String documentName = json.getString(DOCUMENT_NAME, EMPTY);
        final String casePTIUrn = json.getString(CASE_PTIURN, EMPTY);
        final String docCtlNumber =  json.getString(DOCUMENT_CONTROL_NUMBER, EMPTY);
        final String manualIntervention = json.getString(MANUAL_INTERVENTION, EMPTY);
        final String nextAction = json.getString(NEXT_ACTION, EMPTY);
        final String nextActionDate = json.getString(NEXT_ACTION_DATE, EMPTY);
        final String notes = json.getString(NOTES, EMPTY);
        final String scanningDate = json.getString(SCANNING_DATE, EMPTY);
        final String asn = json.getString(ASN, EMPTY);
        final UUID scanDocumentId = UUID.randomUUID();

        final AssociatedScanDocumentBuilder associatedScanDocumentBuilder = new AssociatedScanDocumentBuilder();
        associatedScanDocumentBuilder.withScanDocumentId(scanDocumentId.toString());
        setCasePTIUrn(casePTIUrn, associatedScanDocumentBuilder);
        setCaseUrn(caseUrn, associatedScanDocumentBuilder);
        setDocumentControlNumber(docCtlNumber, associatedScanDocumentBuilder);
        setDocumentName(documentName, associatedScanDocumentBuilder);
        setFileName(json, associatedScanDocumentBuilder);
        setManualIntervention(manualIntervention, associatedScanDocumentBuilder);
        setNextAction(nextAction, associatedScanDocumentBuilder);
        setNextActionDate(nextActionDate, associatedScanDocumentBuilder);
        setNotes(notes, associatedScanDocumentBuilder);
        setScanningDate(scanningDate, associatedScanDocumentBuilder);
        setVendorReceivedDate(deliveryDate, associatedScanDocumentBuilder);
        setAsn(asn, associatedScanDocumentBuilder);

        if (isNoneBlank(casePTIUrn)) {
            final String ouCode = referenceDataQueryHelper.getOuCodeByPtiUrn(casePTIUrn).getString(OUCODE);
            if (!ouCode.isEmpty()) {
                final String shortName = referenceDataQueryHelper.getProsecutorsByOuCode(ouCode).getString(SHORT_NAME);
                associatedScanDocumentBuilder.withProsecutorAuthorityId(ouCode);
                associatedScanDocumentBuilder.withProsecutorName(shortName);
            }
        } else if (isNoneBlank(prosecutorId)) {
            final JsonObject jsonObject = referenceDataQueryHelper.getProsecutorsByOuCode(prosecutorId);
            if(Objects.nonNull(jsonObject) && jsonObject.containsKey(SHORT_NAME)) {
                final String shortName = jsonObject.getString(SHORT_NAME);
                associatedScanDocumentBuilder.withProsecutorAuthorityId(prosecutorId);
                associatedScanDocumentBuilder.withProsecutorName(shortName);
            }
        }
        final DocumentHelper documentHelper = new DocumentHelper(new ReferenceDataQueryHelper());
        final String status = documentHelper.determineDocumentStatus(caseUrn, casePTIUrn, prosecutorId, documentName);
        associatedScanDocumentBuilder.withStatus(status);
        documentBuilder.add("status", status);

        enrichDocumentBuilder(json, documentBuilder, scanDocumentId);

        buildAdditionalDetails(json, associatedScanDocumentBuilder);

        return associatedScanDocumentBuilder.createDocuments();
    }

    private void buildAdditionalDetails(final JsonObject json, final AssociatedScanDocumentBuilder builder) {
        final String ocrData = json.getString(OCR_DATA, null);
        if (isNoneBlank(ocrData)) {
            final String ocrContent = new String(Base64.getDecoder().decode(ocrData));
            context.getLogger().info(String.format("ocr content -->%s", ocrContent));
            final Map<String, String> metadataMap = buildMetadata(ocrContent);

            if (getDocumentMapper().getSinglePlea().equals(json.getString(DOCUMENT_NAME)) || getDocumentMapper().getMultiplePlea().equals(json.getString(DOCUMENT_NAME))) {
                final PleaBuilder pleaBuilder = new PleaBuilder();
                builder.withPlea(pleaBuilder.buildPlea(metadataMap));
            }

            if (getDocumentMapper().getMc100().equals(json.getString(DOCUMENT_NAME))) {
                final Mc100Builder mc100Builder = new Mc100Builder();
                builder.withMc100s(mc100Builder.buildMC100(metadataMap));
            }
        }
    }

    private List<MetadataDetails> buildMetadataDetails(final JsonArray jsonArray) {
        return IntStream.range(0, jsonArray.size())
                .mapToObj(jsonArray::getJsonObject)
                .map(this::buildMetadataDetail).collect(Collectors.toList());
    }

    private MetadataDetails buildMetadataDetail(final JsonObject jsonObject) {
        return MetadataDetails.metadataDetails()
                .withMetadata_field_name(jsonObject.getString("metadata_field_name"))
                .withMetadata_field_value(jsonObject.getString("metadata_field_value"))
                .build();
    }

    private Map<String, String> buildMetadata(final String ocrContent) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        final JsonObject jsonObject = stringToJsonObjectConverter.convert(ocrContent);
        final JsonArray metadataFiles = jsonObject.getJsonArray("Metadata_file");
        final OcrMetadata ocrMetadata = OcrMetadata.ocrMetadata().withMetadata_file(buildMetadataDetails(metadataFiles)).build();
        final List<MetadataDetails> metadataFile = ocrMetadata.getMetadata_file();
        return metadataFile.stream().collect(Collectors.toMap(MetadataDetails::getMetadata_field_name, MetadataDetails::getMetadata_field_value));
    }

    private void enrichDocumentBuilder(JsonObject json, JsonObjectBuilder documentBuilder, final UUID scanDocumentId) {
        documentBuilder.add(SCAN_DOCUMENT_ID, scanDocumentId.toString());
        if (json.containsKey(CASE_NUMBER)) {
            documentBuilder.add(CASE_URN, json.getString(CASE_NUMBER, EMPTY));
            documentBuilder.add(PROSECUTOR_ID, json.getString(PROSECUTOR_ID, EMPTY));
        }
        if (json.containsKey(CASE_PTIURN)) {
            documentBuilder.add(CASE_PTIURN, json.getString(CASE_PTIURN, EMPTY));
        }
        documentBuilder.add(DOCUMENT_NAME, json.getString(DOCUMENT_NAME, EMPTY));
        documentBuilder.add(FILE_NAME, json.getString(FILE_NAME, EMPTY));
        if (json.containsKey(ASN)) {
            documentBuilder.add(ASN, json.getString(ASN, EMPTY));
        }
    }

    private void setZipFileName(String zipFileName, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(zipFileName)) {
            payloadBuilder.withZipFileName(zipFileName);
        }
    }

    private void setZipFileCreationDate(String createdDate, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(createdDate)) {
            payloadBuilder.withZipFileCreationDate(createdDate);
        }
    }

    private void setPoBox(String poBox, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(poBox)) {
            payloadBuilder.withPoBox(poBox);
        }
    }

    private void setVendorOpeningDate(String openingDate, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(openingDate)) {
            payloadBuilder.withVendorOpeningDate(openingDate);
        }
    }

    private void setNotes(String notes, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(notes)) {
            payloadBuilder.withNotes(notes);
        }
    }

    private void setJurisdiction(String jurisdiction, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(jurisdiction)) {
            payloadBuilder.withJurisdiction(jurisdiction);
        }
    }

    private void setEnvelopeClassifcation(String envClassification, ProviderPayload.ProviderPayloadBuilder payloadBuilder) {
        if(isNoneBlank(envClassification)) {
            payloadBuilder.withEnvelopeClassifcation(envClassification);
        }
    }

    private void setAsn(String asn, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(asn)) {
            associatedScanDocumentBuilder.withAsn(asn);
        }
    }

    private void setVendorReceivedDate(String deliveryDate, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(deliveryDate)) {
            associatedScanDocumentBuilder.withVendorReceivedDate(deliveryDate);
        }
    }

    private void setScanningDate(String scanningDate, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(scanningDate)) {
            associatedScanDocumentBuilder.withScanningDate(scanningDate);
        }
    }

    private void setNotes(String notes, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(notes)) {
            associatedScanDocumentBuilder.withNotes(notes);
        }
    }

    private void setNextActionDate(String nextActionDate, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(nextActionDate)) {
            associatedScanDocumentBuilder.withNextActionDate(nextActionDate);
        }
    }

    private void setNextAction(String nextAction, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(nextAction)) {
            associatedScanDocumentBuilder.withNextAction(nextAction);
        }
    }

    private void setManualIntervention(String manualIntervention, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(manualIntervention)) {
            associatedScanDocumentBuilder.withManualIntervention(manualIntervention);
        }
    }

    private void setFileName(JsonObject json, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(json.getString(FILE_NAME))) {
            associatedScanDocumentBuilder.withFileName(json.getString(FILE_NAME));
        }
    }

    private void setDocumentName(String documentName, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(documentName)) {
            associatedScanDocumentBuilder.withDocumentName(documentName);
        } else {
            associatedScanDocumentBuilder.withDocumentName(EMPTY);
        }
    }

    private void setDocumentControlNumber(String docCtlNumber, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(docCtlNumber)) {
            associatedScanDocumentBuilder.withDocumentControlNumber(docCtlNumber);
        }
    }

    private void setCaseUrn(String caseUrn, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(caseUrn)) {
            associatedScanDocumentBuilder.withCaseUrn(caseUrn);
        }
    }

    private void setCasePTIUrn(String casePTIUrn, AssociatedScanDocumentBuilder associatedScanDocumentBuilder) {
        if(isNoneBlank(casePTIUrn)) {
            associatedScanDocumentBuilder.withCasePTIUrn(casePTIUrn);
        }
    }


    public Client getClient() {
        return clientWrapper.getClient();
    }

    public String getBulkScanAPIUrl() {
        return getenv("register_envelope_api_url");
    }

    public String getCPPUID() {
        return getenv("BS-CPPUID");
    }

    public String getBulkScanAPIContentType() {
        return getenv("register_envelope_content_type");
    }
}
