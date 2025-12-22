package uk.gov.moj.cpp.bulkscan.azure.function;

import static org.apache.commons.io.IOUtils.toByteArray;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.bulkscan.azure.event.EventGridSchema;
import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanProcessorException;
import uk.gov.moj.cpp.bulkscan.azure.exception.InvalidZipPayloadException;
import uk.gov.moj.cpp.bulkscan.azure.rest.ReferenceDataQueryHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.StagingBulkScanCommandHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.StagingProsecutorCommandHelper;
import uk.gov.moj.cpp.bulkscan.azure.storage.BlobCloudStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class BulkScanInboxProcessorTest {

    @Mock
    private BlobCloudStorage blobCloudStorage;

    @Mock
    private BlobCloudStorage scanProviderCloudStorage;

    @Mock
    private BlobCloudStorage scanProviderFailedBlobStorage;

    @Mock
    private StagingBulkScanCommandHelper stagingBulkScanHandler;

    @Mock
    private StagingProsecutorCommandHelper stagingProsecutorCommandHelper;

    @Mock
    private Response mockResponse;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Logger logger;

    private static InputStream providerJsonInputStream;

    @BeforeEach
    public void onceBeforeEachTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void processValidZipCorrectly() throws IOException, BulkScanProcessorException {
        providerJsonInputStream = BulkScanInboxProcessorTest.class.getResourceAsStream("/scanProviderPayloadWithResponse.json");
        givenTheEnvironmentIsSetCorrectly();
        when(stagingBulkScanHandler.registerEnvelope(any(JsonObject.class))).thenReturn(Json.createReader(providerJsonInputStream).readObject());
        Response materialResponse = Mockito.mock(Response.class);
        when(stagingProsecutorCommandHelper.addMaterial(any(JsonObject.class), anyString(), any(InputStream.class))).thenReturn(materialResponse);
        whenBulkScanProcessorIsInvokedWithPayload("valid_scan_documents");
        verify(stagingBulkScanHandler).registerEnvelope(any(JsonObject.class));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_documents/CrownCourtExtract.pdf"));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_documents/sample_file.pdf"));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_documents/CrownCourtExtract.png"));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_documents/sample_file.png"));
    }

    @Test
    public void documentCountNoMatchInvalidZip() throws IOException {
        givenTheEnvironmentIsSetCorrectly();
        whenBulkScanProcessorIsInvokedWithPayload("document_count_no_match");
        verifyTheOriginalZipIsUploadedToFailedContainer("document_count_no_match",
                "Invalid zip payload exception Json payload has 3 but actual document count was 2");
    }

    @Test
    public void payloadDoesntHaveUniqueFileNames() throws IOException {
        givenTheEnvironmentIsSetCorrectly();
        whenBulkScanProcessorIsInvokedWithPayload("non_unique_file_names");
        verifyTheOriginalZipIsUploadedToFailedContainer("non_unique_file_names",
                "Invalid zip payload exception Payload doesn't have unique files");
    }

    @Test
    public void payloadAndPdfDoesntMatch() throws IOException {
        givenTheEnvironmentIsSetCorrectly();
        whenBulkScanProcessorIsInvokedWithPayload("payload_and_files_no_match");
        verifyTheOriginalZipIsUploadedToFailedContainer("payload_and_files_no_match",
                "Invalid zip payload exception Payload and pdf files doesn't match");
    }

    @Test
    public void stagingBulkHandlerReturnsError() throws IOException {
        providerJsonInputStream = BulkScanInboxProcessorTest.class.getResourceAsStream("/scanProviderPayloadWithErrorResponse.json");
        givenTheEnvironmentIsSetCorrectly();
        when(stagingBulkScanHandler.registerEnvelope(any(JsonObject.class))).thenReturn(Json.createReader(providerJsonInputStream).readObject());
        when(executionContext.getLogger()).thenReturn(logger);

        assertThrows(BulkScanProcessorException.class, () -> whenBulkScanProcessorIsInvokedWithPayload("valid_scan_documents"));
    }

    @Test
    public void zipDoesntHaveJsonPayload() throws IOException {
        givenTheEnvironmentIsSetCorrectly();
        whenBulkScanProcessorIsInvokedWithPayload("no_json_payload");
        verifyTheOriginalZipIsUploadedToFailedContainer("no_json_payload",
                "Invalid zip payload exception Payload doesn't have proper metadata json");
    }

    @Test
    public void shouldNotGenerateImageIfZipHasOnePdfDocument() throws IOException, BulkScanProcessorException {
        providerJsonInputStream = BulkScanInboxProcessorTest.class.getResourceAsStream("/scanProviderPayloadWithResponse_one_pdf.json");
        givenTheEnvironmentIsSetCorrectly();
        when(stagingBulkScanHandler.registerEnvelope(any(JsonObject.class))).thenReturn(Json.createReader(providerJsonInputStream).readObject());
        whenBulkScanProcessorIsInvokedWithPayload("valid_scan_documents_with_one_pdf");
        verify(stagingBulkScanHandler).registerEnvelope(any(JsonObject.class));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_documents_with_one_pdf/file-sample_150kB.pdf"));
    }

    @Test
    public void shouldNotInvokeAddMaterialIfCaseUrnDoesntExists() throws IOException {
        providerJsonInputStream = BulkScanInboxProcessorTest.class.getResourceAsStream("/scanProviderPayloadWithResponse.json");
        givenTheEnvironmentIsSetCorrectly();
        Response materialResponse = Mockito.mock(Response.class);
        when(stagingProsecutorCommandHelper.addMaterial(any(JsonObject.class), anyString(), any(InputStream.class))).thenReturn(materialResponse);
        when(stagingBulkScanHandler.registerEnvelope(any(JsonObject.class))).thenReturn(Json.createReader(providerJsonInputStream).readObject());
        whenBulkScanProcessorIsInvokedWithPayload("valid_scan_document_with_no_case_urn");
        verify(stagingBulkScanHandler).registerEnvelope(any(JsonObject.class));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_document_with_no_case_urn/CrownCourtExtract.pdf"));
        verify(blobCloudStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("valid_scan_document_with_no_case_urn/sample_file.pdf"));
    }

    @Test
    public void shouldThrowInvalidZipExceptionThenMoveZipToFailedContainer() throws IOException {
        providerJsonInputStream = BulkScanInboxProcessorTest.class.getResourceAsStream("/scanProviderPayloadWithNoScannableItems.json");
        givenTheEnvironmentIsSetCorrectly();
        when(stagingBulkScanHandler.registerEnvelope(any(JsonObject.class))).thenReturn(Json.createReader(providerJsonInputStream).readObject());
        when(executionContext.getLogger()).thenReturn(logger);
        whenBulkScanProcessorIsInvokedWithPayload("no_scannable_items");
        verify(stagingBulkScanHandler, never()).registerEnvelope(any(JsonObject.class));
        verify(logger).severe("Invalid zip payload exception No scannable items found in the Json payload");
        verify(scanProviderFailedBlobStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq("no_scannable_items.zip"));
    }

    private void givenTheEnvironmentIsSetCorrectly() {
        when(executionContext.getLogger()).thenReturn(logger);
        when(mockResponse.getStatus()).thenReturn(HttpStatus.ACCEPTED.value());
    }

    private void whenBulkScanProcessorIsInvokedWithPayload(final String fileName) throws IOException {
        InputStream fileInputStream = getClass().getResourceAsStream("/" + fileName + ".zip");
        BulkScanInboxProcessor bulkScanInboxProcessor = new CustomBulkScanInboxProcessor();
        final HashMap<String, Object> data = new HashMap<>();
        data.put("url", "http://someurl/bs-ste-scans-received/" + fileName + ".zip");
        bulkScanInboxProcessor.processZip(new EventGridSchema(new Date(), data), executionContext, toByteArray(fileInputStream));
    }

    private void verifyTheOriginalZipIsUploadedToFailedContainer(final String fileName, String errorMessage) {
        verify(stagingBulkScanHandler, never()).registerEnvelope(any(JsonObject.class));
        verify(blobCloudStorage, never()).uploadToStorage(any(InputStream.class), any(Long.class), eq(fileName + "/CrownCourtExtract.pdf"));
        verify(blobCloudStorage, never()).uploadToStorage(any(InputStream.class), any(Long.class), eq(fileName + "/sample_file.pdf"));
        verify(scanProviderFailedBlobStorage).uploadToStorage(any(InputStream.class), any(Long.class), eq(fileName + ".zip"));
        verify(logger).severe(errorMessage);
    }

    private class CustomBulkScanInboxProcessor extends BulkScanInboxProcessor {
        @Override
        public BlobCloudStorage getScanManagerActiveBlobContainer() throws BulkScanProcessorException {
            return blobCloudStorage;
        }

        @Override
        public BlobCloudStorage getScanProviderInboxBlobContainer() {
            return scanProviderCloudStorage;
        }

        @Override
        public BlobCloudStorage getScanProviderFailedBlobContainer() {
            return scanProviderFailedBlobStorage;
        }

        @Override
        public StagingBulkScanCommandHelper newStagingBulkScanCommandHelper(final ReferenceDataQueryHelper referenceDataQueryHelper) {
            return stagingBulkScanHandler;
        }

        @Override
        public StagingProsecutorCommandHelper newStagingProsecutorCommandHelper() {
            return stagingProsecutorCommandHelper;
        }
    }
}
