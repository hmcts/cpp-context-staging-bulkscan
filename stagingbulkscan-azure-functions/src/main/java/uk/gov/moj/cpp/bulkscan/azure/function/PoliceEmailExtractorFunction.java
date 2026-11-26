package uk.gov.moj.cpp.bulkscan.azure.function;

import static java.lang.System.getenv;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.APPLICATION_PDF;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.ENVIRONMENT;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.JSON_EXTENSION;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.OUCODE;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.PDF_EXTENSION;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.POLICE_EMAIL_EXTRACTOR_FUNCTION;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.URN_REGEX;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.ZIP_EXTENSION;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.ZIP_FILE_NAME_DATE_FORMAT;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.emailPattern;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.formatTimestamp;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.fromSentPattern;

import uk.gov.moj.cpp.bulkscan.azure.ErrorCode;
import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanConfigurationMissingException;
import uk.gov.moj.cpp.bulkscan.azure.exception.BulkScanProcessorException;
import uk.gov.moj.cpp.bulkscan.azure.rest.Attachment;
import uk.gov.moj.cpp.bulkscan.azure.rest.AttachmentMetadata;
import uk.gov.moj.cpp.bulkscan.azure.rest.EmailDetails;
import uk.gov.moj.cpp.bulkscan.azure.rest.Metadata;
import uk.gov.moj.cpp.bulkscan.azure.rest.NotificationEmailHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.ProcessResults;
import uk.gov.moj.cpp.bulkscan.azure.rest.Prosecutor;
import uk.gov.moj.cpp.bulkscan.azure.rest.ReferenceDataQueryHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.ScannableItem;
import uk.gov.moj.cpp.bulkscan.azure.storage.BlobCloudStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;


/**
 * Azure Functions with HTTP Trigger.
 */

@SuppressWarnings({"squid:S2139", "squid:S2629", "squid:S4042", "squid:S899"})
public class PoliceEmailExtractorFunction {

    private ReferenceDataQueryHelper referenceDataQueryHelper = new ReferenceDataQueryHelper();
    private NotificationEmailHelper notificationEmailHelper = new NotificationEmailHelper();
    private BlobCloudStorage blobCloudStorage;

    @SuppressWarnings({"squid:S1312"})
    @FunctionName(POLICE_EMAIL_EXTRACTOR_FUNCTION)
    public HttpResponseMessage processEmail(@HttpTrigger(name = "req", methods = {HttpMethod.POST},
            authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<EmailDetails> request,
                                            final ExecutionContext context) {

        final String correlationId = UUID.randomUUID().toString();

        final Logger logger = context.getLogger();

        final EmailDetails emailDetails = request.getBody();
        logger.info(String.format("PoliceEmailExtractorFunction invoked for email subject: " +
                "%s correlationId: %s", emailDetails.getSubject(), correlationId));

        final Optional<Prosecutor> prosecutor = getProsecutorDetails(emailDetails, logger);

        if (prosecutor.isPresent()) {
            final Prosecutor prosecutorData = prosecutor.get();
            logger.info(() -> String.format("%s: Prosecutor details found." +
                    "oucodes: %s", correlationId, prosecutorData.getAuthorityCodes()));
            final String senderEmailAddress = prosecutorData.getEmailAddress();
            final ProcessResults processResults = processEmailDetails(emailDetails, prosecutorData);
            handleInvalidAttachments(senderEmailAddress, processResults, emailDetails.getSubject(), logger, correlationId);
            handleValidAttachments(processResults, logger, correlationId);
        } else {
            logger.severe(String.format("%s: ErrorCode: %s : No Prosecutor found for Sender's " +
                    "email domain: Email Subject: %s", correlationId,ErrorCode.NO_PROSECUTOR_FOUND_FOR_SENDER_DOMAIN.getCode(), emailDetails.getSubject()));
        }
        return createResponse(request);
    }

    private void handleInvalidAttachments(String senderEmailAddress, ProcessResults processResults, String subject,
                                          Logger logger, String correlationId) {
        if (processResults.hasInvalidAttachments()) {
            if (!processResults.getNonPdfAttachments().isEmpty()) {
                logger.severe(String.format("%s : ErrorCode:%s - Unsupported File types for files: %s",
                        correlationId,
                        ErrorCode.UNSUPPORTED_FILE_TYPE.getCode(),
                        String.join(",", processResults.getNonPdfAttachments())));
            }

            if (!processResults.getUrnNotFoundAttachments().isEmpty()) {
                logger.severe(String.format("%s : ErrorCode:%s - Case URN not found in subject or one/more attachments: %s",
                        correlationId,
                        ErrorCode.CASE_URN_NOT_FOUND.getCode(),
                        String.join(",", processResults.getUrnNotFoundAttachments())));
            }

            if (!processResults.getProsecutorUrnNotMatchWithAttachments().isEmpty()) {
                logger.severe(String.format("%s : ErrorCode:%s - Prosecutor found but is unrelated to case URN for one/more attachments: %s",
                        correlationId,
                        ErrorCode.PROSECUTOR_FOUND_BUT_IS_UNRELATED_TO_CASE_URN.getCode(),
                        String.join(",", processResults.getProsecutorUrnNotMatchWithAttachments())));
            }

            if(processResults.isEmailWithNoAttachments()) {
                logger.severe(String.format("%s : ErrorCode:%s - No SJPN file attached to the email",
                        correlationId,
                        ErrorCode.NO_SJPN_FILE_ATTACHED_TO_THE_EMAIL.getCode()));
            }

            // Prepare and send email with invalid files
            prepareAndSendEmail(senderEmailAddress, String.join(",", processResults.getAllInvalidAttachments()), subject, logger, correlationId);
        }
    }

    private void handleValidAttachments(ProcessResults processResults, Logger logger, String correlationId) {

        final Date vendorReceivedDate = new Date();

        final List<AttachmentMetadata> attachmentMetadataList = processResults.getAttachmentMetadataList();
        if (!attachmentMetadataList.isEmpty()) {
            final String documentControlNumber = FunctionConstants.createDocumentControlNumber();

            final String zipFileName = documentControlNumber + "_" + formatTimestamp(ZIP_FILE_NAME_DATE_FORMAT);
            final String zipFileNameWithExtension = zipFileName + ZIP_EXTENSION;
            final File metadataFile = writeMetadataToFile(processResults, vendorReceivedDate, documentControlNumber, zipFileNameWithExtension, logger);
            File zipFile= null;
            try {
                zipFile = prepareZipFile(logger, attachmentMetadataList, documentControlNumber, zipFileName, metadataFile);
                uploadZipFile(zipFile, zipFileNameWithExtension);
                logger.info(String.format("%s%s%s", correlationId, zipFileNameWithExtension, ": is uploaded successfully "));
            } catch (IOException e) {
                logger.severe(correlationId + ":Error occurred while creating zip file or uploading to blob storage:" + e.getMessage());
                throw new BulkScanProcessorException("Error occurred while reading the file contents.", e);
            }finally {
                if (zipFile != null && zipFile.exists()) {
                    zipFile.delete();
                }
            }
        }
    }

    private File prepareZipFile(final Logger logger, final List<AttachmentMetadata> attachmentMetadataList,
                                final String documentControlNumber, final String zipFileName,
                                final File metadataFile) throws IOException {
        final File zipFile = createFile(logger, zipFileName,ZIP_EXTENSION);
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        addMetaDataToZipFile(metadataFile, out);
        addAttachmentToZipFile(attachmentMetadataList, documentControlNumber, out);
        out.closeEntry();
        out.close();
        return zipFile;
    }


    private void addMetaDataToZipFile(final File metadataFile, final ZipOutputStream out) throws IOException {
        final ZipEntry e = new ZipEntry("metadata.json");
        final byte[] data = Files.readAllBytes(metadataFile.toPath());
        out.putNextEntry(e);
        out.write(data, 0, data.length);
    }

    private void addAttachmentToZipFile(List<AttachmentMetadata> attachmentMetadataList,
                                        String documentControlNumber, ZipOutputStream out) throws IOException {
        for (int index = 0; index < attachmentMetadataList.size(); index++) {
            final AttachmentMetadata attachmentMetadata = attachmentMetadataList.get(index);
            final String fileName = documentControlNumber + String.format("%04d", index + 1) + ".pdf";
            final ZipEntry entry = new ZipEntry(fileName);
            final byte[] data = Base64.getDecoder().decode(attachmentMetadata.getAttachment().getContentBytes());
            out.putNextEntry(entry);
            out.write(data);
        }
    }

    private void uploadZipFile(File zipFile, String zipFileName) throws IOException {
        try (FileInputStream documentContent = new FileInputStream(zipFile)) {
            getScanProviderInboxBlobContainer().uploadToStorage(
                    documentContent, Files.size(zipFile.toPath()), zipFileName);
        }
    }

    private BlobCloudStorage getScanProviderInboxBlobContainer() {
        if (blobCloudStorage == null) {
            blobCloudStorage = new BlobCloudStorage(getenv("storage-inbox"), String.format("bs-%s-scans-received", getenv(ENVIRONMENT)));
        }
        return blobCloudStorage;
    }

    protected void setBlobCloudStorage(final BlobCloudStorage blobCloudStorage) {
        this.blobCloudStorage = blobCloudStorage;
    }


    protected File writeMetadataToFile(ProcessResults processResults, Date vendorReceivedDate, String documentControlNumber, String zipFileName, Logger logger) {

        final Metadata metaData = constructMetadata(processResults, vendorReceivedDate, documentControlNumber, zipFileName);

        final File tempFile = createFile(logger, "metadata",JSON_EXTENSION);

        final ObjectMapper mapper = new ObjectMapper();

        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            mapper.writeValue(fileWriter, metaData);
        } catch (IOException e) {
            logger.severe("Error creating metadata json");
            throw new BulkScanConfigurationMissingException("Error creating metadata json", e);
        }

        return tempFile;
    }

    private File createFile(Logger logger, String fileName, String extension) {
        try {
            return File.createTempFile(fileName, extension);
        } catch (IOException e) {
            logger.severe("Error creating file" + fileName + "."+ extension);
            throw new BulkScanConfigurationMissingException("Error creating temp file", e);
        }
    }
    private Metadata constructMetadata(ProcessResults processResults, Date vendorReceivedDate, String documentControlNumber, String zipFileName) {
        final List<ScannableItem> scannableItems = IntStream.range(1, processResults.getAttachmentMetadataList().size() + 1)
                .mapToObj(i -> constructScannableItem(processResults, vendorReceivedDate, documentControlNumber, i)).collect(Collectors.toList());

        return buildMetaData(scannableItems, vendorReceivedDate, zipFileName);
    }

    private ScannableItem constructScannableItem(ProcessResults processResults, Date vendorReceivedDate, String documentControlNumber, int i) {
        final AttachmentMetadata attachmentMetadata = processResults.getAttachmentMetadataList().get(i - 1);
        final String formattedDocumentControlNumber = documentControlNumber + String.format("%04d", i);
        return buildScannableItem(attachmentMetadata, vendorReceivedDate, formattedDocumentControlNumber);
    }

    private Metadata buildMetaData(List<ScannableItem> scannableItems, Date vendorReceivedDate, String zipFileName) {
        return new Metadata(scannableItems, "CRIME", vendorReceivedDate, "12888", vendorReceivedDate, zipFileName, vendorReceivedDate);
    }

    private ScannableItem buildScannableItem(AttachmentMetadata attachmentMetadata, Date vendorReceivedDate, String fileName) {
        return new ScannableItem(attachmentMetadata.getUrn(), attachmentMetadata.getUrn(), fileName, "SJPN", fileName + PDF_EXTENSION,
                "FORWARD", vendorReceivedDate, null, vendorReceivedDate);

    }

    private Optional<Prosecutor> getProsecutorDetails(final EmailDetails emailDetails, Logger logger) {
        final Optional<String> fromAddress = extractFromAddress(emailDetails, logger);
        if (fromAddress.isPresent()) {
            final String fromEmailAddress = fromAddress.get();
            final JsonArray prosecutorsByEmailDomain = referenceDataQueryHelper.getProsecutorByEmailDomain(fromEmailAddress);
            final Set<String> authCodes = new HashSet<>();

            for (final JsonValue jsonValue : prosecutorsByEmailDomain) {
                final JsonObject prosecutorByEmailDomain = (JsonObject)jsonValue;
                if (prosecutorByEmailDomain.containsKey(OUCODE)) {
                    final String oucode = prosecutorByEmailDomain.getString(OUCODE);
                    authCodes.add(oucode);
                }
            }

            if(!authCodes.isEmpty()) {
                return Optional.of(new Prosecutor(fromEmailAddress, authCodes));
            }
        }
        return Optional.empty();
    }

    protected Optional<String> extractLastForwardEmailDetails(String emailBody) {
        final Matcher fromSentMatcher = fromSentPattern.matcher(emailBody);
        Optional<String> results = Optional.empty();
        while (fromSentMatcher.find()) {
            results = Optional.of(fromSentMatcher.group());
        }
        return results;
    }

    protected Optional<String> extractFromAddress(final EmailDetails emailDetails, Logger logger) {
        final String amsEmailAddress = getAMSEmailAddress();
        if (emailDetails.getFrom().equalsIgnoreCase(amsEmailAddress)) {
            final Optional<String> lastForwardEmailDetails = extractLastForwardEmailDetails(emailDetails.getBody());
            if (lastForwardEmailDetails.isPresent()) {
                final Matcher emailMatcher = emailPattern.matcher(lastForwardEmailDetails.get());
                if (emailMatcher.find()) {
                    return Optional.of(emailMatcher.group());
                }
            }
            logger.severe("Can't find from email address from  forwarded email " +
                    "with subject: " + emailDetails.getSubject());
            return Optional.empty();
        } else {
            return Optional.of(emailDetails.getFrom());
        }
    }

    protected Optional<String> extractURN(final String input) {
        final Matcher matcher = Pattern.compile(URN_REGEX).matcher(input);
        return matcher.find() ? Optional.of(matcher.group().replaceAll("[ /]", "")) : Optional.empty();
    }

    private HttpResponseMessage createResponse(HttpRequestMessage<EmailDetails> request) {
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", APPLICATION_JSON)
                .body("{}")
                .build();
    }

    protected ProcessResults processEmailDetails(EmailDetails emailDetails, Prosecutor prosecutor) {
        final ProcessResults processResults = new ProcessResults();
        final Optional<String> urnFromSubject = extractURN(emailDetails.getSubject());

        if (urnFromSubject.isPresent()) {
            processAttachmentsWithSubjectUrn(emailDetails.getAttachments(), urnFromSubject.get(), processResults, prosecutor);
        } else {
            processAttachments(emailDetails.getAttachments(), processResults, prosecutor);
        }

        return processResults;
    }

    private void processAttachmentsWithSubjectUrn(List<Attachment> attachments, String subjectUrn, ProcessResults processResults, Prosecutor prosecutor) {
        if (attachments.stream().filter(Attachment::isNotSignatureAttachment).count() > 1) {
            markAttachmentAsUrnNotFound(attachments, processResults);
        } else {
            final Optional<Attachment> attachmentOptional = attachments
                    .stream()
                    .filter(Attachment::isNotSignatureAttachment)
                    .findFirst();

            if (attachmentOptional.isPresent()) {
                Attachment attachment = attachmentOptional.get();
                if (!isPdfAttachment(attachment)) {
                    processResults.addNonPdfAttachment(attachment.getName());
                } else {
                    processSingleValidAttachment(attachment, subjectUrn, processResults, prosecutor);
                }
            } else {
                processResults.setEmailWithNoAttachments(true);
            }
        }
    }

    private void processSingleValidAttachment(Attachment attachment, String subjectUrn, ProcessResults processResults, Prosecutor prosecutor) {
        final String urnPrefix = subjectUrn.substring(0, 2);
        final Optional<String> oucode = findOucodeForUrnPrefix(prosecutor, urnPrefix);
        if (oucode.isPresent()) {
            //extract URN from attachment
            processAttachmentWithOucode(attachment, subjectUrn, processResults, oucode.get());
        } else {
            processResults.addProsecutorUrnNotMatchWithAttachments(attachment.getName());
        }
    }

    private void processAttachmentWithOucode(final Attachment attachment, final String subjectUrn, final ProcessResults processResults, final String oucode) {
        final Optional<String> attachmentUrn = extractURN(attachment.getName());
        if (attachmentUrn.isPresent()) {
            if (subjectUrn.equalsIgnoreCase(attachmentUrn.get())) {
                processResults.addAttachmentMetadata(populateAttachmentMetaData(subjectUrn, attachment, oucode));
            } else {
                processResults.addUrnNotFoundAttachment(attachment.getName());
            }
        } else {
            processResults.addAttachmentMetadata(populateAttachmentMetaData(subjectUrn, attachment, oucode));
        }
    }

    private Optional<String> findOucodeForUrnPrefix(Prosecutor prosecutor, String urnPrefix) {
        return prosecutor.getAuthorityCodes().stream()
                .filter(s -> s.substring(1, 3).equalsIgnoreCase(urnPrefix))
                .findFirst();
    }

    private boolean isPdfAttachment(Attachment attachment) {
        return APPLICATION_PDF.equalsIgnoreCase(attachment.getContentType());
    }

    private void markAttachmentAsUrnNotFound(List<Attachment> attachments, ProcessResults processResults) {
        attachments.stream()
                .map(Attachment::getName)
                .forEach(processResults::addUrnNotFoundAttachment);
    }

    private AttachmentMetadata populateAttachmentMetaData(final String urnFromSubject, final Attachment a, final String oucode) {
        return AttachmentMetadata.builder()
                .urn(urnFromSubject)
                .fileName(a.getName())
                .oucode(oucode)
                .attachment(a)
                .build();
    }

    private void processAttachments(List<Attachment> attachments, ProcessResults processResults, Prosecutor prosecutor) {
        attachments.forEach(attachment -> {
            if (attachmentIsPdf(attachment)) {
                processPdfAttachmentForUrn(attachment, processResults,prosecutor);
            } else {
                if (attachment.isNotSignatureAttachment()) {
                    processNonPdfAttachment(attachment, processResults);
                }
            }
        });
    }

    private boolean attachmentIsPdf(Attachment attachment) {
        return APPLICATION_PDF.equalsIgnoreCase(attachment.getContentType());
    }

    private void processPdfAttachmentForUrn(Attachment attachment, ProcessResults processResults, Prosecutor prosecutor) {
        final Optional<String> urnFromAttachment = extractURN(attachment.getName());
        if (urnFromAttachment.isPresent()) {
            final String urn = urnFromAttachment.get();
            final String urnPrefix = urn.substring(0, 2);
            final Optional<String> oucode = prosecutor.getAuthorityCodes().stream()
                    .filter(s -> s.substring(1, 3).equalsIgnoreCase(urnPrefix)).findFirst();
            if (oucode.isPresent()) {
                final AttachmentMetadata metadata = populateAttachmentMetaData(urn, attachment, oucode.get());
                processResults.addAttachmentMetadata(metadata);
            } else {
                processResults.addProsecutorUrnNotMatchWithAttachments(attachment.getName());
            }
        } else {
            processResults.addUrnNotFoundAttachment(attachment.getName());
        }
    }

    private void processNonPdfAttachment(Attachment attachment, ProcessResults processResults) {
        processResults.addNonPdfAttachment(attachment.getName());
    }

    protected void setReferenceDataQueryHelper(final ReferenceDataQueryHelper referenceDataQueryHelper) {
        this.referenceDataQueryHelper = referenceDataQueryHelper;
    }

    protected void setNotificationEmailHelper(final NotificationEmailHelper notificationEmailHelper) {
        this.notificationEmailHelper = notificationEmailHelper;
    }

    private void prepareAndSendEmail(String senderEmailAddress, String invalidFileNames, String subject, Logger logger, String correlationId) {
        final String notificationId = UUID.randomUUID().toString();
        logger.info(() -> correlationId + ": Sending notification email with notification id: " + notificationId);
        final Response response = notificationEmailHelper.sendNotificationEmail(notificationId, senderEmailAddress, invalidFileNames, subject, logger);
        if (response.getStatus() == 202) {
            logger.info(() -> correlationId + ": Notification Email Sent successfully: response.getStatus() " + response.getStatus());
        } else {
            logger.severe(() -> correlationId + ": Notification API call failed with reason: " + response.readEntity(String.class));
        }
    }

    protected String getAMSEmailAddress() {
        return getenv("ams_email_address");
    }

}

