package uk.gov.moj.cpp.stagingbulkscan.azure.core.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the storage blob client
 */
@ApplicationScoped
public class BlobClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobClientProvider.class);

    private static final byte[] DEFAULT = new byte[0];

    @Inject
    private ApplicationParameters applicationParameters;


    public void deleteIfExists(final String fileName) {
        deleteIfExists(fileName, applicationParameters.getStorageConnectionString(), applicationParameters.getAzureScanManagerContainerName());
        deleteIfExists(fileName, applicationParameters.getScanManagerStorageConnectionString(), applicationParameters.getScanManagerContainerName());
    }

    private void deleteIfExists(final String fileName, final String connectionString, final String containerName) {
        final CloudBlobClient blobClient = getBlobClientReference(connectionString);
        try {
            final CloudBlobContainer container = blobClient.getContainerReference(containerName);
            
            final CloudBlockBlob blockBlob = container.getBlockBlobReference(fileName);

            blockBlob.deleteIfExists();

        } catch (URISyntaxException | StorageException e) {
            LOGGER.error("Error occurred while getting file content from Azure storage.", e);
        }
    }

    public byte[] getBlobContent(final String zipFileName, final String documentFileName) {
        // first try in the new scan manager storage
        final byte[] blobContent = getBlobContent(zipFileName, documentFileName, applicationParameters.getScanManagerStorageConnectionString(), applicationParameters.getScanManagerContainerName());
        if (blobContent == DEFAULT) {
            return getBlobContent(zipFileName, documentFileName, applicationParameters.getStorageConnectionString(), applicationParameters.getAzureScanManagerContainerName());
        }

        return blobContent;
    }

    private byte[] getBlobContent(final String zipFileName, final String documentFileName, final String connectionString, final String containerName) {
        try {

            final CloudBlobClient blobClient = getBlobClientReference(connectionString);

            final CloudBlobContainer container = blobClient.getContainerReference(containerName);

            final CloudBlockBlob blockBlob = container.getBlockBlobReference(zipFileName + "/" + documentFileName);

            return readFully(blockBlob.openInputStream());

        } catch (URISyntaxException | StorageException | IOException e) {
            LOGGER.error("Error occurred while getting file content from Azure storage.", e);
        }

        return DEFAULT;
    }

    /**
     * Validates the connection string and returns the storage blob client. The connection string
     * must be in the Azure connection string format.
     *
     * @return The newly created CloudBlobClient object
     */
    private CloudBlobClient getBlobClientReference(final String storageConnectionString) {

        CloudStorageAccount storageAccount;
        try {
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
        } catch (URISyntaxException | InvalidKeyException e) {
            LOGGER.error("\nConnection string specifies an invalid URI.");
            LOGGER.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException("Invalid connection string.", e);
        }

        return storageAccount.createCloudBlobClient();
    }

    private static byte[] readFully(InputStream stream) throws IOException {

        final byte[] buffer = new byte[8192];

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int bytesRead;

        while ((bytesRead = stream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        return byteArrayOutputStream.toByteArray();
    }
}