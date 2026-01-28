package uk.gov.moj.cpp.bulkscan.azure.zip;

import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanProcessorException;
import uk.gov.moj.cpp.bulkscan.azure.exception.InvalidZipPayloadException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class BlobZipStream implements AutoCloseable {

    private byte[] originalZipFileContent;
    private ZipInputStream zipFileInputStream;

    public BlobZipStream(final byte[] originalZipFileContent) {
        this.zipFileInputStream = new ZipInputStream(new ByteArrayInputStream(originalZipFileContent));
        this.originalZipFileContent = originalZipFileContent;
    }

    public int getOriginalZipFileLength() {
        return originalZipFileContent.length;
    }

    public byte[] getOriginalZipFileContent() {
        return originalZipFileContent;
    }

    public ZipInputStream getZipFileInputStream() {
        return zipFileInputStream;
    }

    public Optional<JsonObject> getMetadataJson() {
        JsonReader reader = null;
        try {
            ZipEntry nextEntry = this.zipFileInputStream.getNextEntry();
            while (nextEntry != null) {
                if (nextEntry.getName().endsWith("json")) {
                    reader = JsonObjects.createReader(this.zipFileInputStream);
                    final JsonObject jsonObject = reader.readObject();
                    return Optional.of(jsonObject);
                }
                nextEntry = this.zipFileInputStream.getNextEntry();
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new InvalidZipPayloadException("Error reading metadata json document from the zip ", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            resetInputStream();
        }
    }

    public List<String> pdfDocumentNames() {
        try {
            final List<String> fileNames = new ArrayList<>();
            ZipEntry nextEntry = this.zipFileInputStream.getNextEntry();
            while (nextEntry != null) {
                if (nextEntry.getName().endsWith("pdf")) {
                    fileNames.add(nextEntry.getName());
                }
                nextEntry = this.zipFileInputStream.getNextEntry();
            }
            return fileNames;
        } catch (IOException e) {
            throw new InvalidZipPayloadException("Error reading pdf document from the zip ", e);
        } finally {
            resetInputStream();
        }
    }

    private void resetInputStream() {
        this.close();
        this.zipFileInputStream = new ZipInputStream(new ByteArrayInputStream(originalZipFileContent));
    }

    @Override
    public void close() {
        try {
            this.getZipFileInputStream().close();
        } catch (IOException e) {
            throw new BulkScanProcessorException("Unable to close the original input stream", e);
        }
    }
}
