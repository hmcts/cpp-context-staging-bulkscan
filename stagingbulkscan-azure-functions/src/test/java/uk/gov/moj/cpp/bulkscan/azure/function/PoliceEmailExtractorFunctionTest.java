package uk.gov.moj.cpp.bulkscan.azure.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.APPLICATION_PDF;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.moj.cpp.bulkscan.azure.rest.Attachment;
import uk.gov.moj.cpp.bulkscan.azure.rest.AttachmentMetadata;
import uk.gov.moj.cpp.bulkscan.azure.rest.EmailDetails;
import uk.gov.moj.cpp.bulkscan.azure.rest.NotificationEmailHelper;
import uk.gov.moj.cpp.bulkscan.azure.rest.ProcessResults;
import uk.gov.moj.cpp.bulkscan.azure.rest.Prosecutor;
import uk.gov.moj.cpp.bulkscan.azure.rest.ReferenceDataQueryHelper;
import uk.gov.moj.cpp.bulkscan.azure.storage.BlobCloudStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


public class PoliceEmailExtractorFunctionTest {

    @Mock
    private HttpRequestMessage<EmailDetails> request;

    @Mock
    private ExecutionContext context;
    @Mock
    private Logger logger;
    @Mock
    private Builder responseBuilder;
    @Mock
    private HttpResponseMessage httpResponseMessage;

    @Mock
    private ReferenceDataQueryHelper referenceDataQueryHelper;

    @Mock
    private NotificationEmailHelper notificationEmailHelper;

    @Mock
    private BlobCloudStorage blobCloudStorage;

    private PoliceEmailExtractorFunction functionToTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        functionToTest = spy(new PoliceEmailExtractorFunction());
        when(context.getLogger()).thenReturn(logger);
        when(responseBuilder.header("Content-Type", "application/json")).thenReturn(responseBuilder);
        when(responseBuilder.body(anyString())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(httpResponseMessage);
    }


    // Prosecutor Not found
    @Test
    public void givenProsecutorNotfoundForEmailDomain_processEmail_shouldLogProsecutorNotFound() {

        final EmailDetails emailDetails = createEmailDetails("subjectWithoutUrn", "test@test.com",
                createSingleAttachmentList("value1", new Date(), "test", "testc", "hjkk"));

        when(request.getBody()).thenReturn(emailDetails);
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);

        JsonObject jsonObject = createObjectBuilder().add("emailDomain", "").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());

        functionToTest.processEmail(request, context);

        verify(logger).severe(contains("ErrorCode: 4002 : No Prosecutor found for Sender's email domain: Email Subject: " + emailDetails.getSubject()));
    }

    // URN in subject - pdf not matching OUCODE
    @Test
    public void givenValidUrnInSubjectPdfNotMatchingOucode_processEmail_shouldReturnOK() {

        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                createSingleAttachmentList("value1.pdf", new Date(), "test", "application/pdf", "hjkk"));

        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        when(request.getBody()).thenReturn(emailDetails);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        JsonObject jsonObject = createObjectBuilder().add("oucode", "OUCODE").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.ok().build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(logger).severe(contains("ErrorCode:4004 - Prosecutor found but is unrelated to case URN for " +
                "one/more attachments: " + "test"));
        verify(httpResponseMessage, times(1)).getStatus();
        verifyNoMoreInteractions(request, httpResponseMessage);
    }


    @Test
    public void givenValidUrnInSubject_processEmail_shouldReturnOK() {

        //URN in subject - PDF matching prosecutor OUCODE

        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                createSingleAttachmentList("value1.pdf", new Date(), "test", "application/pdf", "hjkk"));

        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        when(request.getBody()).thenReturn(emailDetails);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verifyNoMoreInteractions(request, httpResponseMessage);
        verify(blobCloudStorage).uploadToStorage(any(), any(), any());
        verify(notificationEmailHelper, never()).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenUrnNotFoundInAttachmentAndSubject_processEmail_shouldReturnOK() {

        //URN in subject - PDF matching prosecutor OUCODE

        final EmailDetails emailDetails = createEmailDetails("Subject with No URN", "test@test.com",
                createSingleAttachmentList("value1.pdf", new Date(), "Attachement With No URN", "application/pdf", "hjkk"));

        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        when(request.getBody()).thenReturn(emailDetails);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.ok().build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verifyNoMoreInteractions(request, httpResponseMessage);
        verify(logger).severe(contains("ErrorCode:4003 - Case URN not found in subject or one/more attachments: "  + "Attachement With No URN"));

        verify(notificationEmailHelper).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenUrnNotInSubject_processEmail_shouldSendNotificationEmail() {

        // URN not in subject - Not PDF matching Prosecutor Oucode
        final EmailDetails emailDetails = createEmailDetails("subjectWithoutUrn", "test@test.com",
                createSingleAttachmentList("value1", new Date(), "test", "testc", "hjkk"));
        when(request.getBody()).thenReturn(emailDetails);
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.ok().build());
        JsonObject jsonObject = createObjectBuilder().add("oucode", "OUCODE").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verify(logger).severe(contains("ErrorCode:4001 - Unsupported File types for files: " + "test"));
        verify(notificationEmailHelper, times(1)).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(request);
        verifyNoMoreInteractions(httpResponseMessage);
    }
    @Test
    public void givenUrnNotInSubjectAndProsecutorNotFound_processEmail_shouldSendNotificationEmail() {

        // URN not in subject - Not PDF matching Prosecutor Oucode
        final EmailDetails emailDetails = createEmailDetails("subjectWithoutUrn", "test@test.com",
                createSingleAttachmentList("value1", new Date(), "attachment-1-44TK2317123", APPLICATION_PDF, "hjkk"));
        when(request.getBody()).thenReturn(emailDetails);
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.ok().build());
        JsonObject jsonObject = createObjectBuilder().add("oucode", "OUCODE").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();

        verify(logger).severe(contains("ErrorCode:4004 - Prosecutor found but is unrelated to case URN for " +
                "one/more attachments: " + "attachment-1-44TK2317123"));
        verify(notificationEmailHelper, times(1)).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(request);
        verifyNoMoreInteractions(httpResponseMessage);
    }

    // URN in subject - Multiple attachments with Non pdf as well
    @Test
    public void givenAnyAttachmentNotPdf_processEmail_shouldSendNotificationEmail() {


        final Attachment attachment1 = createAttachment("value1", new Date(), "test", "application/pdf", "hjkk", false);
        final Attachment attachment2 = createAttachment("value1", new Date(), "test", "application/notpdf", "hjkk", false);
        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                Lists.newArrayList(attachment1, attachment2));
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(request.getBody()).thenReturn(emailDetails);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.accepted().build());
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verify(notificationEmailHelper, times(1)).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
        verify(logger).severe(contains("ErrorCode:4003 - Case URN not found in subject or one/more attachments: test,test"));
        verifyNoMoreInteractions(request);
        verifyNoMoreInteractions(httpResponseMessage);
    }


    @Test
    public void givenAnyAttachmentIsPartOfASignatureTheyShouldBeIgnoredAndShouldBeProcessedSuccessfully() {

        final Attachment attachment1 = createAttachment("value1", new Date(), "test", "application/pdf", "hjkk", false);
        final Attachment attachment2 = createAttachment("value1", new Date(), "test", "image/notpdf", "hjkk", true);
        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                Lists.newArrayList(attachment1, attachment2));
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(request.getBody()).thenReturn(emailDetails);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.accepted().build());
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verifyNoMoreInteractions(notificationEmailHelper);
    }

    @Test
    public void givenThereAreNoValidPdfAttachmentItShouldProcessAndEmail() {

        final Attachment attachment1 = createAttachment("value1", new Date(), "test", "image/pdf", "hjkk", true);
        final Attachment attachment2 = createAttachment("value1", new Date(), "test", "image/notpdf", "hjkk", true);
        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                Lists.newArrayList(attachment1, attachment2));
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(request.getBody()).thenReturn(emailDetails);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.accepted().build());
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(notificationEmailHelper, times(1)).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenOuCodeMismatchWithURN_processEmail_shouldSendNotificationEmail() {

        // URN not in subject - multiple attachments one match with oucode, one failed to match.
        final Attachment attachment1 = createAttachment("value1", new Date(), "test", "application/pdf", "hjkk", false);
        final Attachment attachment2 = createAttachment("value1", new Date(), "testNonPdf", "application/notpdf", "hjkk", false);
        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                Lists.newArrayList(attachment1, attachment2));
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(request.getBody()).thenReturn(emailDetails);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setBlobCloudStorage(blobCloudStorage);
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.accepted().build());

        JsonObject jsonObject = createObjectBuilder().add("oucode", "0420000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        HttpResponseMessage response = functionToTest.processEmail(request, context);
        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verify(notificationEmailHelper, times(1)).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(request);
        verifyNoMoreInteractions(httpResponseMessage);
        verify(blobCloudStorage, never()).uploadToStorage(any(), any(), any());
        verify(logger).severe(contains("ErrorCode:4003 - Case URN not found in subject or one/more attachments: test,testNonPdf"));
    }

    @Test
    public void givenValidUrnInSubjectWithMultipleAttachments_processEmail_shouldReturnOK() {

        final EmailDetails emailDetails = createEmailDetails("21 AZ 12345 21", "test@test.com",
                createAttachmentList("value1.pdf", new Date(), "test", "application/pdf", "hjkk"));

        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(httpResponseMessage.getStatus()).thenReturn(HttpStatus.OK);
        when(request.getBody()).thenReturn(emailDetails);
        functionToTest.setReferenceDataQueryHelper(referenceDataQueryHelper);
        functionToTest.setNotificationEmailHelper(notificationEmailHelper);
        JsonObject jsonObject = createObjectBuilder().add("oucode", "0210000").build();
        when(referenceDataQueryHelper.getProsecutorByEmailDomain("test@test.com"))
                .thenReturn(createArrayBuilder().add(jsonObject).build());
        when(notificationEmailHelper.sendNotificationEmail(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(Response.accepted().build());
        functionToTest.setBlobCloudStorage(blobCloudStorage);

        HttpResponseMessage response = functionToTest.processEmail(request, context);

        assertEquals(HttpStatus.OK, response.getStatus());

        verify(request, times(1)).createResponseBuilder(HttpStatus.OK);
        verify(request, times(1)).getBody();
        verify(httpResponseMessage, times(1)).getStatus();
        verify(notificationEmailHelper).sendNotificationEmail(anyString(),anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(request, httpResponseMessage);
        verify(blobCloudStorage, never()).uploadToStorage(any(), any(), any());
    }

    @Test
    public void givenValidSubjectWithURN_getUrnEmail_shouldReturnUrnFromEmailSubject() {
        String expectedUrn = "99JP1234503";
        String subject = "Subject with URN: " + expectedUrn;
        EmailDetails emailDetails = createEmailDetails(subject, "from",
                createSingleAttachmentList("value1", new Date(),
                        "Attachment without URN", APPLICATION_PDF, "hjkk"));

        when(request.getBody()).thenReturn(emailDetails);

        Set<String> authCodes = new TreeSet<>();
        authCodes.add("0990000");
        Prosecutor prosecutor = new Prosecutor( "test@test.com", authCodes);
        ProcessResults response = functionToTest.processEmailDetails(emailDetails, prosecutor);

        assertEquals(expectedUrn, response.getAttachmentMetadataList().get(0).getUrn());

        verifyNoMoreInteractions(notificationEmailHelper);

    }

    @Test
    public void givenValidAttachmentWithURN_getUrnFromEmail_shouldReturnUrnFromAttachment() {
        String expectedUrn = "99JP1234503";
        String filename = "Attachment with URN: " + expectedUrn;
        String subject = "Subject without URN";
        final EmailDetails emailDetails = createEmailDetails(subject, "from",
                createSingleAttachmentList("value1", new Date(), filename, APPLICATION_PDF, "hjkk"));

        when(request.getBody()).thenReturn(emailDetails);

        Set<String> authCodes = new TreeSet<>();
        authCodes.add("0990000");
        Prosecutor prosecutor = new Prosecutor( "test@test.com", authCodes);
        ProcessResults response = functionToTest.processEmailDetails(emailDetails, prosecutor);

        assertEquals(expectedUrn, response.getAttachmentMetadataList().get(0).getUrn());
    }

    @Test
    public void givenSubjectWithNoURN_getUrnFromEmail_shouldReturnNullWhenNoUrnPresent() {
        String subject = "Subject without URN";
        final EmailDetails emailDetails = createEmailDetails(subject, "from",
                createSingleAttachmentList("value1", new Date(), "Attachment without URN", APPLICATION_PDF, "hjkk"));

        when(request.getBody()).thenReturn(emailDetails);

        ProcessResults response = functionToTest.processEmailDetails(emailDetails, createProsecutor());
        assertEquals(1, response.getUrnNotFoundAttachments().size());
    }

    @Test
    public void givenSubjectWithNoURN_MultipleAttachmentsWithUrn() {
        String subject = "Subject without URN";

        List<Attachment> attachments = Arrays.asList(
                createAttachment("attachment-1", new Date(), "attachment-1-44TK2317123", APPLICATION_PDF, "hjkk", false),
                createAttachment("attachment-2", new Date(), "attachment-2-42TK2317423", APPLICATION_PDF, "hjkk", false),
                createAttachment("attachment-3", new Date(), "attachment-3-42TK2317423", APPLICATION_PDF, "hjkk", false)
        );

        final EmailDetails emailDetails = createEmailDetails(subject, "from", attachments);

        when(request.getBody()).thenReturn(emailDetails);
        Set<String> authCodes = new TreeSet<>();
        authCodes.add("0420000");
        authCodes.add("0440000");
        Prosecutor prosecutor = new Prosecutor( "test@test.com", authCodes);

        ProcessResults response = functionToTest.processEmailDetails(emailDetails, prosecutor);
        assertEquals(0, response.getUrnNotFoundAttachments().size());

        assertEquals(0, response.getNonPdfAttachments().size());

        assertEquals(3, response.getAttachmentMetadataList().size());
    }

    @Test
    public void givenSubjectWithNoURN_MultipleAttachmentsWithUrnAndNonUrn() {
        String subject = "Subject without URN";

        List<Attachment> attachments = Arrays.asList(
                createAttachment("attachment-1", new Date(), "Attachment-without-URN", APPLICATION_PDF, "hjkk", false),
                createAttachment("attachment-2", new Date(), "Attachment-Non-PDF", "nonPDF", "hjkk", false),
                createAttachment("attachment-3", new Date(), "Attachment-with-URN-42TK2317423", APPLICATION_PDF, "hjkk", false)
        );

        final EmailDetails emailDetails = createEmailDetails(subject, "from", attachments);

        when(request.getBody()).thenReturn(emailDetails);

        Set<String> authCodes = new TreeSet<>();
        authCodes.add("0420000");
        Prosecutor prosecutor = new Prosecutor( "test@test.com", authCodes);
        ProcessResults response = functionToTest.processEmailDetails(emailDetails, prosecutor);
        assertEquals(1, response.getUrnNotFoundAttachments().size());
        assertEquals("Attachment-without-URN", response.getUrnNotFoundAttachments().get(0));

        assertEquals(1, response.getNonPdfAttachments().size());
        assertEquals("Attachment-Non-PDF", response.getNonPdfAttachments().get(0));

        assertEquals(1, response.getAttachmentMetadataList().size());
        assertEquals("42TK2317423", response.getAttachmentMetadataList().get(0).getUrn());
        assertEquals("Attachment-with-URN-42TK2317423", response.getAttachmentMetadataList().get(0).getFileName());
    }

    @Test
    public void givenValidURNInput_extractURN_shouldExtractValidURN() {
        String input = "44BB0339623 XXX XXX";
        Optional<String> urn = functionToTest.extractURN(input);
        assertTrue(urn.isPresent());
        assertEquals("44BB0339623", urn.get());

        assertEquals("42TK2317423", functionToTest.extractURN("42TK2317423 hjjh hjhjh").get());
        assertEquals("42TK2317423", functionToTest.extractURN("xxx xxx xxx 42TK2317423").get());
        assertEquals("44FZ0419623", functionToTest.extractURN("XXX XXX 44FZ0419623 (1 OF 2)").get());
        assertEquals("44FZ0419723", functionToTest.extractURN("XXX XXX 44FZ0419723 (2 OF 2)").get());
        assertEquals("44FZ0419723", functionToTest.extractURN("XXX XXX 44 FZ 04197 23 (2 OF 2)").get());
        assertEquals("44OY0432723", functionToTest.extractURN("XXX XXX 44/OY/04327/23").get());
        assertEquals("41CT1026924", functionToTest.extractURN("41/CT/10269/24 14/02/24 DIRIYE Bashir Ali").get());

        assertEquals("42TK2351723", functionToTest.extractURN("@£$£$%$$%^$ %$^%$^ 42TK2351723").get());
        assertEquals("44FZ0419723", functionToTest.extractURN("___ ___454 %%$ 44FZ0419723 (1 OF 2)").get());
        assertEquals("44FZ0419723", functionToTest.extractURN("000 2333 88FF 4343 ___ 44FZ0419723 (2 OF 2)").get());
        assertEquals("44FZ0419723", functionToTest.extractURN("XXX XXX 44FZ0419723 44FZ0419723 (2 OF 2)").get());
        assertEquals("42TK2317423", functionToTest.extractURN("XXX XXX 42TK2317423 44FZ0419723 (2 OF 2)").get());
        assertEquals(Optional.empty(), functionToTest.extractURN("ESSEX POLICE SJP CASE BUNDLE"));
        assertEquals(Optional.empty(), functionToTest.extractURN("Binder4.pdf"));
        assertEquals(Optional.empty(), functionToTest.extractURN("Binder9.pdf"));
    }

    @Test
    public void givenInvalidURNInput_extractURN_shouldReportInvalidURN() {
        String input = "invalid";
        Optional<String> urn = functionToTest.extractURN(input);
        assertFalse(urn.isPresent());
    }

    @Test
    public void givenEmptyURN_extractURN_shouldReturnFalse() {
        String input = "";
        Optional<String> urn = functionToTest.extractURN(input);
        assertFalse(urn.isPresent());
    }

    @Test
    public void givenAllTheRequiredData_writeMetadataToFile_shouldGenerateFile() throws IOException {
        // Initialize
        String prosecutorAuthorityCode = "ProsecutorAuthorityCode";
        Date vendorReceivedDate = new Date();
        String documentControlNumber = UUID.randomUUID().toString();
        String zipFileName = "test.zip";
        List<AttachmentMetadata> attachmentMetadataList = new ArrayList<>();
        AttachmentMetadata attachmentMetadata = AttachmentMetadata.builder().oucode(prosecutorAuthorityCode).build();
        attachmentMetadataList.add(attachmentMetadata);
        Logger logger = Mockito.mock(Logger.class);
        ProcessResults processResults = new ProcessResults();
        processResults.getAttachmentMetadataList().addAll(attachmentMetadataList);

        // Act
        File result = functionToTest.writeMetadataToFile(processResults, vendorReceivedDate, documentControlNumber, zipFileName, logger);

        //Assert
        assertTrue(result.exists());
        assertNotNull(result);

        Files.deleteIfExists(result.toPath());
    }

    @Test
    public void testExtractForwardEmailAddressAsFromAddress() {
        final EmailDetails emailDetails = EmailDetails.builder()
                .body("<b>From:</b> Test Prosecutor &lt;test.prosecutor@hmcts.net&gt;<br><b>Sent:</b> 02 February 2024 12:46<br><b>To:")
                .from("ams@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertTrue(fromAddress.isPresent());
        assertEquals("test.prosecutor@hmcts.net", fromAddress.get());
    }

    @Test
    public void testExtractLastForwardEmailAddressAsFromAddress() {
        final EmailDetails emailDetails = EmailDetails.builder()
                .body("<b>From:</b> Test Prosecutor &lt;test.prosecutor-1@hmcts.net&gt;<br><b>Sent:</b> 02 February 2024 12:46<br><b>To:" +
                        "<b>From:</b> Test Prosecutor &lt;test.prosecutor-2@hmcts.net&gt;<br><b>Sent:</b> 02 February 2024 12:46<br><b>To:" +
                        "<b>From:</b> Test Prosecutor &lt;test.prosecutor-3@hmcts.net&gt;<br><b>Sent:</b> 02 February 2024 12:46<br><b>To:")
                .from("ams@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertTrue(fromAddress.isPresent());
        assertEquals("test.prosecutor-3@hmcts.net", fromAddress.get());
    }

    @Test
    public void testExtractFromEmailAddress() {
        final EmailDetails emailDetails = EmailDetails.builder()
                .from("test.prosecutor@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertTrue(fromAddress.isPresent());
        assertEquals("test.prosecutor@test.com", fromAddress.get());
    }

    @Test
    public void logTheErrorWhenBodyIsMissing() {
        final EmailDetails emailDetails = EmailDetails.builder()
                .subject("test")
                .body("")
                .from("ams@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertFalse(fromAddress.isPresent());
        verify(logger).severe(eq("Can't find from email address from  forwarded email " +
                "with subject: " + emailDetails.getSubject()));
    }

    @Test
    public void logTheErrorWhenBodyContentMissingFromAndSent() {
        final EmailDetails emailDetails = EmailDetails.builder()
                .body("<b></b> Test Prosecutor &lt;test.prosecutor@hmcts.net&gt;<br>")
                .from("ams@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertFalse(fromAddress.isPresent());
        verify(logger).severe(eq("Can't find from email address from  forwarded email " +
                "with subject: " + emailDetails.getSubject()));
    }

    @Test
    public void logErrorWhenBodyContentMissingForwardEmailAddress() {

        final EmailDetails emailDetails = EmailDetails.builder()
                .body("<b>From:</b> Test Prosecutor &lt;t&gt;<br><b>Sent:</b> 02 February 2024 12:46<br><b>To:")
                .from("ams@test.com").build();
        when(functionToTest.getAMSEmailAddress()).thenReturn("ams@test.com");
        Optional<String> fromAddress = functionToTest.extractFromAddress(emailDetails, logger);
        assertFalse(fromAddress.isPresent());
        verify(logger).severe(eq("Can't find from email address from  forwarded email " +
                "with subject: " + emailDetails.getSubject()));
    }

    public EmailDetails createEmailDetails(String subject, String from, java.util.List<Attachment> attachments) {
        return EmailDetails.builder()
                .subject(subject)
                .from(from)
                .attachments(attachments)
                .build();
    }

    private static Attachment createAttachment(String attachmentId, Date lastModifiedDate, String fileName, String contentType, String contentBytes, boolean isInline) {
        return new Attachment(attachmentId, lastModifiedDate, fileName, contentType, contentBytes,  isInline);
    }

    private List<Attachment> createSingleAttachmentList(String attachmentId, Date lastModifiedDate, String fileName, String contentType, String contentBytes) {
        final List<Attachment> attachments = new ArrayList<>();
        attachments.add(createAttachment(attachmentId, lastModifiedDate, fileName, contentType, contentBytes, false));
        return attachments;
    }

    private List<Attachment> createAttachmentList(String attachmentId, Date lastModifiedDate, String fileName, String contentType, String contentBytes) {

        final List<Attachment> attachments = new ArrayList<>();
        Attachment attachment1 = createAttachment(attachmentId, lastModifiedDate, fileName, contentType, contentBytes, false);
        Attachment attachment2 = createAttachment(attachmentId, lastModifiedDate, fileName, contentType, contentBytes, false);
        attachments.add(attachment1);
        attachments.add(attachment2);
        return attachments;
    }

    private Prosecutor createProsecutor() {
        Set<String> authCodes = new TreeSet<>();
        authCodes.add("OUCODE");
        return new Prosecutor("test@test.com", authCodes);
    }


}

