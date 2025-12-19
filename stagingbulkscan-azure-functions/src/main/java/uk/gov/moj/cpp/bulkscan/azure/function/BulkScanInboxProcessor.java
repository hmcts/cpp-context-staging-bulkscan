package uk.gov.moj.cpp.bulkscan.azure.function;

import static java.lang.System.getenv;

import uk.gov.moj.cpp.bulkscan.azure.event.EventGridSchema;
import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanProcessorException;
import uk.gov.moj.cpp.bulkscan.azure.exception.InvalidZipPayloadException;
import uk.gov.moj.cpp.bulkscan.azure.rest.ReferenceDataQueryHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.StagingBulkScanCommandHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.StagingProsecutorCommandHelper;
import uk.gov.moj.cpp.bulkscan.azure.storage.BlobCloudStorage;
import uk.gov.moj.cpp.bulkscan.azure.zip.BlobZipStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class BulkScanInboxProcessor {

    private static final String ENVIRONMENT = "environment";

    private ExecutionContext context;

    @SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S2139"})
    @FunctionName("bulkScanInboxProcessor")
    public void processZip(final @EventGridTrigger(name = "event") EventGridSchema eventGridSchema,
                           final ExecutionContext context,
                           @BlobInput(
                                   name = "file",
                                   dataType = "binary",
                                   path = "{data.url}",
                                   connection = "storage-inbox") final byte[] content) {

        this.context = context;
        final Logger logger = context.getLogger();

        logger.info("Event content:");
        logger.info("Time Received: " + eventGridSchema.getEventTime());
        logger.info("Zip file url: " + eventGridSchema.getData().get("url"));

        if (Objects.isNull(content)) {
            logger.severe("received null content.");
            return;
        }

        try (BlobZipStream zipInputStream = new BlobZipStream(content)) {
            final BlobCloudStorage containerReference = getScanManagerActiveBlobContainer();
            final String zipFileName = getZipFileName(eventGridSchema);
            processTheProviderZipFile(zipInputStream, containerReference, zipFileName);
            deleteTheProviderZipFile(zipFileName);
            logger.info("Original input zip file is deleted after processing " + zipFileName);
        } catch (IOException e) {
            logger.severe("Exception occurred while processing the zip file " + e.getMessage());
            throw new BulkScanProcessorException("Exception occured while processing the zip file", e);
        }
        logger.info("Zip file is processed successfully");
    }

    @SuppressWarnings({"squid:S134", "squid:S1166", "squid:S3655"})
    private void processTheProviderZipFile(BlobZipStream blobZipStream, BlobCloudStorage containerReference,
                                           String zipFileName) throws IOException {
        try {
            validateZipPayload(blobZipStream);
            final Optional<JsonObject> payloadJsonStream = blobZipStream.getMetadataJson();
            final ReferenceDataQueryHelper referenceDataQueryHelper = new ReferenceDataQueryHelper();
            final StagingBulkScanCommandHelper stagingBulkScanCommandHelper = newStagingBulkScanCommandHelper(referenceDataQueryHelper);
            final StagingProsecutorCommandHelper stagingProsecutorCommandHelper = newStagingProsecutorCommandHelper();
            final JsonObject jsonInput = payloadJsonStream.get();

            final JsonObject registerEnvelopeResponse = stagingBulkScanCommandHelper.registerEnvelope(jsonInput);
            final int responseCode = registerEnvelopeResponse.getInt("responseCode");

            if (responseCode == HttpStatus.ACCEPTED.value()) {
                final ZipInputStream zipFileInputStream = blobZipStream.getZipFileInputStream();
                ZipEntry nextEntry = zipFileInputStream.getNextEntry();
                while (nextEntry != null) {
                    if (nextEntry.getName().endsWith("pdf")) {
                        containerReference.uploadToStorage(zipFileInputStream, nextEntry.getSize(), zipFileName + "/" + nextEntry.getName());
                    }
                    nextEntry = zipFileInputStream.getNextEntry();
                }
            } else {
                throw new BulkScanProcessorException("Register scan envelope failed -->>" + responseCode);
            }

            attachDocumentToCase(blobZipStream, stagingProsecutorCommandHelper, registerEnvelopeResponse);

            try (BlobZipStream zipStream = new BlobZipStream(blobZipStream.getOriginalZipFileContent())) {
                if (zipStream.pdfDocumentNames().size() > 1) {
                    processImageForPdf(zipStream, containerReference, zipFileName);
                }
            }
        } catch (InvalidZipPayloadException e) {
            context.getLogger().severe("Invalid zip payload exception " + e.getMessage());
            moveProviderZipIntoFailedContainer(blobZipStream, zipFileName);
        }
    }

    @SuppressWarnings({"squid:S134", "squid:S3655"})
    private void attachDocumentToCase(BlobZipStream blobZipStream, StagingProsecutorCommandHelper stagingProsecutorCommandHelper, JsonObject response) throws IOException {
        try (BlobZipStream zipStreamForStagingProsecutor = new BlobZipStream(blobZipStream.getOriginalZipFileContent())) {
            final ZipInputStream zipFileInputStream = zipStreamForStagingProsecutor.getZipFileInputStream();
            ZipEntry nextEntry = zipFileInputStream.getNextEntry();
            final List<JsonObject> documentsToBeAttached = response.getJsonArray("scannable_items").getValuesAs(JsonObject.class);
            while (nextEntry != null) {
                final String fileName = nextEntry.getName();
                if (nextEntry.getName().endsWith("pdf")) {
                    final InputStream inputStream = IOUtils.toBufferedInputStream(zipFileInputStream);
                    final JsonObject documentToBeAttached = documentsToBeAttached.stream().filter(i -> i.getString("file_name").equals(fileName)).findFirst().get();
                    final String pending = "PENDING";
                    if(pending.equalsIgnoreCase(documentToBeAttached.getString("status"))) {
                        final String scanEnvelopeId = response.getString("scanEnvelopeId");
                        stagingProsecutorCommandHelper.addMaterial(documentToBeAttached, scanEnvelopeId, inputStream);
                    }
                }
                nextEntry = zipFileInputStream.getNextEntry();
            }
        }
    }

    private void processImageForPdf(BlobZipStream blobZipStream, BlobCloudStorage containerReference,
                                    String zipFileName) throws IOException {
        final ZipInputStream zipFileInputStream = blobZipStream.getZipFileInputStream();
        ZipEntry nextEntry = zipFileInputStream.getNextEntry();
        while (nextEntry != null) {
            if (nextEntry.getName().endsWith("pdf")) {
                final byte[] imageBytes = generateImage(nextEntry.getName(), zipFileInputStream);
                final String fileName = nextEntry.getName().substring(0, nextEntry.getName().lastIndexOf('.'));
                containerReference.uploadToStorage(new ByteArrayInputStream(imageBytes), (long) imageBytes.length, zipFileName + "/" + fileName + ".png");
            }
            nextEntry = zipFileInputStream.getNextEntry();
        }
    }

    @SuppressWarnings({"squid:S1166"})
    private byte[] generateImage(String fileName, final InputStream inputStream) {
        try {
            final PDDocument document = PDDocument.load(inputStream);
            final PDFRenderer pdfRenderer = new PDFRenderer(document);
            final BufferedImage image = pdfRenderer.renderImageWithDPI(0, 30, ImageType.RGB);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            context.getLogger().warning("Image generation failed, possibly corrupted pdf -->>" + fileName);
        }
        return new byte[]{};
    }

    private void validateZipPayload(BlobZipStream blobZipStream) {
        final Optional<JsonObject> metadataJson = blobZipStream.getMetadataJson();
        if (metadataJson.isPresent()) {
            final JsonObject jsonObject = metadataJson.get();
            final List<JsonObject> scannableItems = jsonObject.getJsonArray("scannable_items").getValuesAs(JsonObject.class);

            if(scannableItems.isEmpty()) {
                throw new InvalidZipPayloadException("No scannable items found in the Json payload");
            }

            final List<String> pdfDocumentNames = blobZipStream.pdfDocumentNames();
            final int countOfDocument = pdfDocumentNames.size();
            if (scannableItems.size() != countOfDocument) {
                throw new InvalidZipPayloadException("Json payload has " + scannableItems.size() + " but actual document count was " + countOfDocument);
            }
            final List<String> fileNames = scannableItems.stream().map(file -> file.getJsonString("file_name").getString()).collect(Collectors.toList());
            if (fileNames.stream().distinct().count() != fileNames.size()) {
                throw new InvalidZipPayloadException("Payload doesn't have unique files");
            }
            if (!fileNames.containsAll(pdfDocumentNames)) {
                throw new InvalidZipPayloadException("Payload and pdf files doesn't match");
            }
        } else {
            throw new InvalidZipPayloadException("Payload doesn't have proper metadata json");
        }
    }

    private void moveProviderZipIntoFailedContainer(final BlobZipStream blobZipStream, final String zipFileName) {
        final BlobCloudStorage failedCloudStorage = getScanProviderFailedBlobContainer();
        try (final ByteArrayInputStream documentContent = new ByteArrayInputStream(blobZipStream.getOriginalZipFileContent())) {
            failedCloudStorage.uploadToStorage(documentContent, (long) blobZipStream.getOriginalZipFileLength(), zipFileName + ".zip");
        } catch (IOException e) {
            throw new BulkScanProcessorException("Unable to move files to failed container because of exception ", e);
        }
    }

    private void deleteTheProviderZipFile(final String originalZipFile) {
        final BlobCloudStorage scanProviderCloudStorage = getScanProviderInboxBlobContainer();
        scanProviderCloudStorage.deleteFromStorage(originalZipFile + ".zip");
    }

    private String getZipFileName(EventGridSchema eventGridSchema) {
        final String zipFileUrl = eventGridSchema.getData().get("url").toString();
        final String zipFileNameWithExtension = zipFileUrl.substring(zipFileUrl.lastIndexOf('/') + 1);
        return zipFileNameWithExtension.substring(0, zipFileNameWithExtension.lastIndexOf('.'));
    }

    public BlobCloudStorage getScanManagerActiveBlobContainer() {
        return new BlobCloudStorage(getenv("storage-scanmgr"), String.format("bs-%s-active-scans", getenv(ENVIRONMENT)));
    }

    public BlobCloudStorage getScanProviderInboxBlobContainer() {
        return new BlobCloudStorage(getenv("storage-inbox"), String.format("bs-%s-scans-received", getenv(ENVIRONMENT)));
    }

    public BlobCloudStorage getScanProviderFailedBlobContainer() {
        return new BlobCloudStorage(getenv("storage-failed"), String.format("bs-%s-scans-failed", getenv(ENVIRONMENT)));
    }

    public StagingBulkScanCommandHelper newStagingBulkScanCommandHelper(final ReferenceDataQueryHelper referenceDataQueryHelper) {
        return new StagingBulkScanCommandHelper(this.context, referenceDataQueryHelper);
    }

    public StagingProsecutorCommandHelper newStagingProsecutorCommandHelper() {
        return new StagingProsecutorCommandHelper();
    }
}

