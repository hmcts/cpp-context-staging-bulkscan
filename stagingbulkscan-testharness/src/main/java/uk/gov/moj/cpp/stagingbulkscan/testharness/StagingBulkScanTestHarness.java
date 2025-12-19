package uk.gov.moj.cpp.stagingbulkscan.testharness;

import static javax.json.Json.createObjectBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2245", "squid:S2629"})
public class StagingBulkScanTestHarness {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingBulkScanTestHarness.class.getName());
    private static final String SRC_FOLDER = "/stagingbulkscan-testharness/src/main/resources/pdfs";
    private static final String DEST_FOLDER = "/stagingbulkscan-testharness/src/main/resources/temp";

    private static final String PDF = ".pdf";
    private static final String JSON = ".json";
    private static final String ZIP = ".zip";

    public static void main(String[] args) {
        final String config = "config.properties";
        final String containerName = "container_name";
        final String connectionString = "connection.string";
        final String zipFileName = UUID.randomUUID().toString() + ZIP;

        try (final InputStream input = StagingBulkScanTestHarness.class.getClassLoader().getResourceAsStream(config)) {

            final Properties prop = new Properties();

            if (input == null) {
                LOGGER.error("Sorry, unable to find config.properties");
                return;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            final File source = new File(new File(".").getCanonicalPath() + SRC_FOLDER);
            final File destination = new File(new File(".").getCanonicalPath() + DEST_FOLDER);
            deleteFiles(destination);
            copyFolder(source, destination);
            renameFiles(destination);
            createJson(zipFileName, destination);
            zipFolder(destination, new File(zipFileName));
            uploadZipFile(zipFileName, prop.getProperty(connectionString), prop.getProperty(containerName));
            deleteFile(new File(zipFileName));
        } catch (Exception e) {
            LOGGER.error("Failed to zip and upload", e);
        }
    }

    private static void uploadZipFile(final String zipFileName, final String connectionString, final String containerName) throws URISyntaxException, InvalidKeyException, StorageException, IOException {
        LOGGER.info(String.format("Uploading %s", zipFileName));
        final CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(connectionString);
        final CloudBlobClient cloudBlobClient = cloudStorageAccount.createCloudBlobClient();
        cloudBlobClient.getContainerReference(containerName).getBlockBlobReference(zipFileName).uploadFromFile(new File(".").getCanonicalPath() + "/" + zipFileName);
    }


    private static void createJson(String zipFileName, File files) throws IOException {
        final File[] listFiles = files.listFiles();
        final String deliveryDate = ZonedDateTime.now().minusDays(5).format(DateTimeFormatter.ISO_INSTANT);
        final int max = 99999999;
        final int min = 11111111;

        final String caseNumber = Integer.toString((int) (Math.random() * ((max - min) + 1)) + min);
        final String caseUrn= "TVL"+caseNumber;
        final JsonObjectBuilder scanEnvelope = createObjectBuilder()
                .add("delivery_date", deliveryDate)
                .add("zip_file_name", zipFileName);

        final JsonArrayBuilder builder = Json.createArrayBuilder();

        for (final File file : Objects.requireNonNull(listFiles)) {
            builder.add(createObjectBuilder()
                    .add("case_number", caseUrn)
                    .add("prosecutor_ID", "GAEAA01")
                    .add("document_name", "Single Justice Procedure Notice - Plea (Single)")
                    .add("file_name", file.getName()));
        }
        scanEnvelope.add("scannable_items", builder);

        final JsonObject payload = scanEnvelope.build();
        final Path path = Paths.get(files + File.separator + UUID.randomUUID().toString() + JSON);
        Files.write(path, payload.toString().getBytes());
    }

    private static void deleteFiles(File dir) {
        for (final File file : Objects.requireNonNull(dir.listFiles())) {
            if (!file.isDirectory()) {
                deleteFile(file);
            }
        }
    }

    private static void deleteFile(final File file) {
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            LOGGER.error("Failed to deleted {}", file.getName(), e);
        }
    }

    private static void renameFiles(File sourceFolder) {
        final File[] listFiles = sourceFolder.listFiles();

        int max = 5000;
        int min = 1000;
        String envId = Integer.toString((int) (Math.random() * ((max - min) + 1)) + min);
        int fileNumber=1;
        for (final File file : Objects.requireNonNull(listFiles)) {
            final boolean renamed = file.renameTo(new File(sourceFolder + File.separator + "E-"+envId +"-"+ fileNumber +"-"+UUID.randomUUID().toString().replaceAll("-", "") + PDF));
            fileNumber=fileNumber+1;
            if (renamed) {
                LOGGER.info("{} is renamed.", file.getName());

            }
        }
    }

    private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        //Check if sourceFolder is a directory or file
        //If sourceFolder is file; then copy the file directly to new location
        if (sourceFolder.isDirectory()) {
            //Verify if destinationFolder is already present; If not then create it
            if (!destinationFolder.exists()) {
                final boolean mkdir = destinationFolder.mkdir();
                if (mkdir) {
                    LOGGER.info("{} folder created.", destinationFolder.getName());
                }
            }

            //Get all files from source directory
            final String[] files = sourceFolder.list();

            for (final String file : Objects.requireNonNull(files)) {
                final File srcFile = new File(sourceFolder, file);
                final File destFile = new File(destinationFolder, file);
                //Recursive function call
                copyFolder(srcFile, destFile);
            }
        } else {
            //Copy the file content from one place to another
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void addFileToZip(File rootPath, File srcFile, ZipOutputStream zip) throws Exception {
        if (srcFile.isDirectory()) {
            addFolderToZip(rootPath, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                String name = srcFile.getName();
                zip.putNextEntry(new ZipEntry(name));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }
    private static void addFolderToZip(File rootPath, File srcFolder, ZipOutputStream zip) throws Exception {
        for (final File fileName : Objects.requireNonNull(srcFolder.listFiles())) {
            addFileToZip(rootPath, fileName, zip);
        }
    }

    private static void zipFolder(File srcFolder, File destZipFile) throws Exception {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {

            addFolderToZip(srcFolder, srcFolder, zip);
        }
    }

}
