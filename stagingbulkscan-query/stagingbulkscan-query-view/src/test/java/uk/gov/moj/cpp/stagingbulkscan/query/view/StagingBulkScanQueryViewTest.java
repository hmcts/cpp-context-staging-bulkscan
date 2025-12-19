package uk.gov.moj.cpp.stagingbulkscan.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.AUTO_ACTIONED;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.FOLLOW_UP;
import static uk.gov.justice.stagingbulkscan.domain.DocumentStatus.MANUALLY_ACTIONED;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.stagingbulkscan.domain.DocumentStatus;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelopeDocument;
import uk.gov.justice.stagingbulkscan.domain.StatusCode;
import uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetDocumentResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.GetThumbnailResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.ScanDocumentsResponse;
import uk.gov.moj.cpp.stagingbulkscan.query.view.response.Thumbnail;
import uk.gov.moj.cpp.stagingbulkscan.query.view.service.StagingBulkScanService;
import uk.gov.moj.cpp.stagingbulkscan.repository.ScanDocumentRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanQueryViewTest {
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID ENVELOPE_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final String DOCUMENT_FILE_NAME = STRING.next() + ".pdf";
    private static final ZonedDateTime VENDOR_RECEIVED_DATE = ZonedDateTime.now(UTC);
    private static final ZonedDateTime STATUS_UPDATED_DATE = ZonedDateTime.now(UTC);
    private static final String CASE_URN = "12295672";
    private static final String PROSECUTOR_ID = "AAAJJ11";
    private static final String PROSECUTOR_NAME = "TFL";
    private static final String DOCUMENT_NAME = "Form SJPN";

    private static final String RESPONSE_NAME_GET_SCAN_DOCUMENT = "stagingbulkscan.get-scan-document";
    private static final String RESPONSE_NAME_GENERATE_THUMBNAIL_CONTENT = "stagingbulkscan.get-thumbnail-content";
    private static final String RESPONSE_NAME_GET_DOCUMENTS_BY_STATUS = "stagingbulkscan.get-all-documents-by-status";
    private static final String RESPONSE_NAME_GET_DOCUMENTS_BY_ID = "stagingbulkscan.get-scan-document-by-id";
    private static final String GET_SCAN_ENVELOPE_DOCUMENTS_BY_ID = "stagingbulkscan.get-scan-envelope-document-by-ids";
    private static final String FIELD_SCAN_DOCUMENTS = "scanDocuments";
    private static final String FIELD_ID = "id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SCAN_ENVELOPE_ID = "scanEnvelopeId";
    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String FIELD_DOCUMENT_FILE_NAME = "documentFileName";
    private static final String FIELD_VENDOR_RECEIVED_DATE = "vendorReceivedDate";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_STATUS_UPDATED_DATE = "statusUpdatedDate";
    private static final String FIELD_THUMBNAILS = "thumbnails";
    public static final String BASE64_CONTENT = "ChQ=";

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private StagingBulkScanService stagingBulkScanService;

    @InjectMocks
    private StagingBulkScanQueryView stagingBulkScanQueryView;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ScanDocumentRepository scanDocumentRepository;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGetScanDocuments() {
        final UUID envelopeId = randomUUID();
        final UUID thumbnailId = randomUUID();
        final UUID documentId = randomUUID();
        final String fileName = STRING.next() + ".pdf";
        final String content = STRING.next();

        final GetDocumentResponse document =
                getDocumentResponse(envelopeId, thumbnailId, documentId, fileName, content);

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add(FIELD_SCAN_ENVELOPE_ID, envelopeId.toString())
                        .add(FIELD_SCAN_DOCUMENT_ID, documentId.toString())
                        .build());

        when(stagingBulkScanService.getDocumentResponse(envelopeId, documentId)).thenReturn(document);

        final JsonEnvelope getDocumentEnvelope = stagingBulkScanQueryView.getScanDocument(query);

        assertThat(getDocumentEnvelope, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_GET_SCAN_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_ID), equalTo(documentId.toString())),
                        withJsonPath(format("$.%s", FIELD_SCAN_ENVELOPE_ID), equalTo(envelopeId.toString())),
                        withJsonPath(format("$.%s", FIELD_DOCUMENT_FILE_NAME), equalTo(fileName)),
                        withJsonPath(format("$.%s", FIELD_STATUS), equalTo(FOLLOW_UP.toString())),
                        withJsonPath(format("$.%s", FIELD_CONTENT), equalTo(content)),
                        withJsonPath(format("$.%s[0].%s", FIELD_THUMBNAILS, FIELD_ID, thumbnailId)),
                        withJsonPath(format("$.%s[0].%s", FIELD_THUMBNAILS, FIELD_DOCUMENT_FILE_NAME, fileName))
                )))
        ));
    }

    @Test
    public void shouldGetScanDocumentByCaseUrnOrCasePtiUrn() {

        final uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument scanDocument = new uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument();

        scanDocument.setDocumentFileName("file.pdf");
        scanDocument.setStatus(MANUALLY_ACTIONED);
        scanDocument.setStatusCode(StatusCode.CASE_NOT_FOUND);

        final List<uk.gov.moj.cpp.stagingbulkscan.persist.entity.ScanDocument> scanDocumentList = Collections.singletonList(scanDocument);

        when(scanDocumentRepository.findScanDocumentStatus(anyString(),anyString())).thenReturn(scanDocumentList);


        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("DocumentFileName", scanDocument.getDocumentFileName())
                .add("status", scanDocument.getStatus().toString())
                .add("statusCode", scanDocument.getStatusCode().toString())
                .build();

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("caseUrn","48C35075720")
                        .add("casePtiUrn", "48C35075720")
                        .build());


        final Envelope envelope = stagingBulkScanQueryView.getScanDocumentStatusByCaseUrn(query);

        assertThat(JsonEnvelope.envelopeFrom(envelope.metadata(), (JsonValue) envelope.payload()),
                CoreMatchers.is(jsonEnvelope(
                        withMetadataEnvelopedFrom(query).withName("stagingbulkscan.get-scan-document-status"),
                        JsonEnvelopePayloadMatcher.payload()
                                .isJson(allOf(
                                        withJsonPath("$.scanDocumentStatuses", hasSize(1)),
                                        withJsonPath("$.scanDocumentStatuses[0].statusCode", CoreMatchers.is(StatusCode.CASE_NOT_FOUND.toString())),
                                        withJsonPath("$.scanDocumentStatuses[0].status", CoreMatchers.is(MANUALLY_ACTIONED.toString())),
                                        withJsonPath("$.scanDocumentStatuses[0].documentFileName", CoreMatchers.is("file.pdf")))
                                ))));
    }

    @Test
    public void shouldGenerateThumbnailContent() {
        final GetThumbnailResponse response = new GetThumbnailResponse();

        response.setId(DOCUMENT_ID);
        response.setContent(BASE64_CONTENT);

        when(stagingBulkScanService.getThumbnailResponse(Mockito.any(UUID.class), Mockito.any(UUID.class))).thenReturn(response);

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add(FIELD_SCAN_ENVELOPE_ID, ENVELOPE_ID.toString())
                        .add(FIELD_SCAN_DOCUMENT_ID, DOCUMENT_ID.toString())
                        .build());

        final JsonEnvelope thumbnailContent = stagingBulkScanQueryView.getThumbnailContent(query);

        assertThat(thumbnailContent, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_GENERATE_THUMBNAIL_CONTENT),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_ID), equalTo(DOCUMENT_ID.toString())),
                        withJsonPath(format("$.%s", FIELD_CONTENT), equalTo(BASE64_CONTENT))
                )))
        ));
    }

    @Test
    public void shouldGetDocumentsByStatuses() {
        final UUID scanDocumentId1 = UUID.randomUUID();
        final UUID scanDocumentId2 = UUID.randomUUID();

        final ScanDocumentsResponse documents = new ScanDocumentsResponse();

        documents.setScanDocuments(getDocuments(scanDocumentId1, scanDocumentId2));

        when(stagingBulkScanService.getScanDocumentsResponseByStatus(Arrays.asList(MANUALLY_ACTIONED, AUTO_ACTIONED))).thenReturn(documents);

        final String statusString = MANUALLY_ACTIONED + "," + AUTO_ACTIONED;
        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("status", statusString).build()
        );

        final JsonEnvelope scanDocumentResponseEnvelope = stagingBulkScanQueryView.findAllDocumentsByStatus(query);

        assertThat(scanDocumentResponseEnvelope, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_GET_DOCUMENTS_BY_STATUS),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_ID), equalTo(scanDocumentId1.toString())),
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_STATUS), equalTo(MANUALLY_ACTIONED.toString())),
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_DOCUMENT_FILE_NAME), equalTo(DOCUMENT_FILE_NAME)),
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_VENDOR_RECEIVED_DATE), equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_STATUS_UPDATED_DATE), equalTo(ZonedDateTimes.toString(STATUS_UPDATED_DATE))),
                        withJsonPath(format("$.%s[0].%s", FIELD_SCAN_DOCUMENTS, FIELD_SCAN_ENVELOPE_ID), equalTo(ENVELOPE_ID.toString())),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_ID), equalTo(scanDocumentId2.toString())),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_STATUS), equalTo(AUTO_ACTIONED.toString())),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_DOCUMENT_FILE_NAME), equalTo(DOCUMENT_FILE_NAME)),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_VENDOR_RECEIVED_DATE), equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_STATUS_UPDATED_DATE), equalTo(ZonedDateTimes.toString(STATUS_UPDATED_DATE))),
                        withJsonPath(format("$.%s[1].%s", FIELD_SCAN_DOCUMENTS, FIELD_SCAN_ENVELOPE_ID), equalTo(ENVELOPE_ID.toString()))
                )))
        ));
    }

    @Test
    public void shouldGetScanEnvelopeDocumentByIds() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID scanEnvelopeId = UUID.randomUUID();

        final ScanEnvelopeDocument envelopeDocument = new ScanEnvelopeDocument.Builder()
                .withId(scanDocumentId)
                .withScanEnvelopeId(scanEnvelopeId)
                .withDocumentFileName("fileName")
                .withStatus(FOLLOW_UP)
                .build();

        when(stagingBulkScanService.getScanEnvelopeDocumentById(scanDocumentId, scanEnvelopeId)).thenReturn(envelopeDocument);

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUID(GET_SCAN_ENVELOPE_DOCUMENTS_BY_ID),
                createObjectBuilder()
                        .add("scanEnvelopeId", scanEnvelopeId.toString())
                        .add("scanDocumentId", scanDocumentId.toString())
                        .build()
        );

        final JsonEnvelope scanDocumentResponseEnvelope = stagingBulkScanQueryView.getScanEnvelopeDocumentByIds(query);

        assertThat(scanDocumentResponseEnvelope, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(GET_SCAN_ENVELOPE_DOCUMENTS_BY_ID),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_ID), equalTo(scanDocumentId.toString())),
                        withJsonPath(format("$.%s", FIELD_SCAN_ENVELOPE_ID), equalTo(scanEnvelopeId.toString())),
                        withJsonPath(format("$.%s", FIELD_STATUS), equalTo(FOLLOW_UP.toString())),
                        withJsonPath(format("$.%s", FIELD_DOCUMENT_FILE_NAME), equalTo("fileName"))
                )))
        ));
    }

    @Test
    public void shouldGetDocumentById() {
        final UUID scanDocumentId = UUID.randomUUID();
        final UUID scanEnvelopeId = randomUUID();

        final ScanDocument scanDocumentResponse = new ScanDocument();
        scanDocumentResponse.setId(scanDocumentId);
        scanDocumentResponse.setScanEnvelopeId(scanEnvelopeId);
        scanDocumentResponse.setStatus(MANUALLY_ACTIONED);
        scanDocumentResponse.setDocumentFileName(DOCUMENT_FILE_NAME);
        scanDocumentResponse.setVendorReceivedDate(VENDOR_RECEIVED_DATE);
        scanDocumentResponse.setStatusUpdatedDate(STATUS_UPDATED_DATE);

        when(stagingBulkScanService.getScanDocumentById(scanEnvelopeId, scanDocumentId)).thenReturn(scanDocumentResponse);

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder().add(FIELD_SCAN_ENVELOPE_ID, scanEnvelopeId.toString())
                        .add(FIELD_SCAN_DOCUMENT_ID, scanDocumentId.toString())
                        .build());

        final JsonEnvelope scanDocumentResponseEnvelope = stagingBulkScanQueryView.getScanDocumentById(query);

        assertThat(scanDocumentResponseEnvelope, is(jsonEnvelope(
                withMetadataEnvelopedFrom(query)
                        .withName(RESPONSE_NAME_GET_DOCUMENTS_BY_ID),
                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_ID), equalTo(scanDocumentId.toString())),
                        withJsonPath(format("$.%s", FIELD_STATUS), equalTo(MANUALLY_ACTIONED.toString())),
                        withJsonPath(format("$.%s", FIELD_DOCUMENT_FILE_NAME), equalTo(DOCUMENT_FILE_NAME)),
                        withJsonPath(format("$.%s", FIELD_VENDOR_RECEIVED_DATE), equalTo(ZonedDateTimes.toString(VENDOR_RECEIVED_DATE))),
                        withJsonPath(format("$.%s", FIELD_STATUS_UPDATED_DATE), equalTo(ZonedDateTimes.toString(STATUS_UPDATED_DATE)))
                )))
        ));
    }

    private List<ScanDocument> getScanDocuments() {
        List<ScanDocument> scanDocumentList = new ArrayList<>();
        final ScanDocument document = new ScanDocument();
        document.setId(DOCUMENT_ID);
        document.setScanEnvelopeId(ENVELOPE_ID);
        document.setDocumentFileName(DOCUMENT_FILE_NAME);
        document.setVendorReceivedDate(VENDOR_RECEIVED_DATE);
        document.setCaseUrn(CASE_URN);
        document.setProsecutorAuthorityId(PROSECUTOR_ID);
        document.setProsecutorAuthorityCode(PROSECUTOR_NAME);
        document.setDocumentName(DOCUMENT_NAME);
        document.setStatus(FOLLOW_UP);
        document.setStatusUpdatedDate(STATUS_UPDATED_DATE);
        scanDocumentList.add(document);
        return scanDocumentList;
    }

    private List<ScanDocument> getActionedScanDocuments() {
        List<ScanDocument> scanDocumentList = new ArrayList<>();
        final ScanDocument document = new ScanDocument();
        document.setId(DOCUMENT_ID);
        document.setScanEnvelopeId(ENVELOPE_ID);
        document.setDocumentFileName(DOCUMENT_FILE_NAME);
        document.setVendorReceivedDate(VENDOR_RECEIVED_DATE);
        document.setCaseUrn(CASE_URN);
        document.setProsecutorAuthorityId(PROSECUTOR_ID);
        document.setProsecutorAuthorityCode(PROSECUTOR_NAME);
        document.setDocumentName(DOCUMENT_NAME);
        document.setStatus(MANUALLY_ACTIONED);
        document.setActionedBy(ACTIONED_BY);
        document.setStatusUpdatedDate(STATUS_UPDATED_DATE);
        document.setDeleted(TRUE);
        scanDocumentList.add(document);
        return scanDocumentList;
    }

    private GetDocumentResponse getDocumentResponse(final UUID envelopeId,
                                                    final UUID thumbnailId,
                                                    final UUID documentId,
                                                    final String fileName,
                                                    final String content) {
        final GetDocumentResponse document = new GetDocumentResponse();
        List<Thumbnail> thumbnailList = new ArrayList<>();

        Thumbnail thumbnail = new Thumbnail(thumbnailId, fileName);
        thumbnailList.add(thumbnail);

        document.setScanEnvelopeId(envelopeId);
        document.setId(documentId);
        document.setDocumentFileName(fileName);
        document.setContent(content);
        document.setStatus(FOLLOW_UP);
        document.setThumbnails(thumbnailList);

        return document;
    }

    private List<ScanDocument> getDocuments(final UUID scanDocumentId1, final UUID scanDocumentId2) {
        List<ScanDocument> scanDocumentList = new ArrayList<>();

        final ScanDocument document1 = new ScanDocument();
        document1.setId(scanDocumentId1);
        document1.setScanEnvelopeId(ENVELOPE_ID);
        document1.setStatus(MANUALLY_ACTIONED);
        document1.setDocumentFileName(DOCUMENT_FILE_NAME);
        document1.setVendorReceivedDate(VENDOR_RECEIVED_DATE);
        document1.setStatusUpdatedDate(STATUS_UPDATED_DATE);
        scanDocumentList.add(document1);

        final ScanDocument document2 = new ScanDocument();
        document2.setId(scanDocumentId2);
        document2.setScanEnvelopeId(ENVELOPE_ID);
        document2.setStatus(AUTO_ACTIONED);
        document2.setDocumentFileName(DOCUMENT_FILE_NAME);
        document2.setVendorReceivedDate(VENDOR_RECEIVED_DATE);
        document2.setStatusUpdatedDate(STATUS_UPDATED_DATE);
        scanDocumentList.add(document2);

        return scanDocumentList;
    }
}
