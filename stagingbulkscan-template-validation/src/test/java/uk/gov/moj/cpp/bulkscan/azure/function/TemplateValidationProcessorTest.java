package uk.gov.moj.cpp.bulkscan.azure.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("unchecked")
class TemplateValidationProcessorTest {

    @Mock
    private HttpRequestMessage<Optional<byte[]>> request;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage response;

    @Mock
    private ExecutionContext context;

    private TemplateValidationProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TemplateValidationProcessor();

        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.body(any(String.class))).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when request body is empty")
    void testValidateTemplateWithEmptyBody() throws IOException {
        when(request.getBody()).thenReturn(Optional.empty());

        HttpResponseMessage result = processor.validateTemplate(request, context);

        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
        verify(responseBuilder).body("Please send document content in the request body.");
        verify(responseBuilder).build();
        assertEquals(response, result);
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when request body is null")
    void testValidateTemplateWithNullBody() throws IOException {
        when(request.getBody()).thenReturn(Optional.ofNullable(null));

        HttpResponseMessage result = processor.validateTemplate(request, context);

        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
        verify(responseBuilder).body("Please send document content in the request body.");
        verify(responseBuilder).build();
        assertEquals(response, result);
    }

    @Test
    @DisplayName("Should return OK with success message when document is valid")
    void testValidateTemplateWithValidDocument() throws IOException {
        byte[] documentContent = "valid document content".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");
        validResult.addDetail("Document validated successfully");
        validResult.addDetail("All sections found");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            HttpResponseMessage result = processor.validateTemplate(request, context);

            verify(request).createResponseBuilder(HttpStatus.OK);
            verify(responseBuilder).body(argThat(body ->
                    body.toString().contains("Document validated successfully") &&
                            body.toString().contains("Document type: ENGLISH") &&
                            body.toString().contains("Document validated successfully") &&
                            body.toString().contains("All sections found")));
            verify(responseBuilder).build();
            assertEquals(response, result);
        }
    }

    @Test
    @DisplayName("Should return BAD_REQUEST with error messages when document is invalid")
    void testValidateTemplateWithInvalidDocument() throws IOException {
        byte[] documentContent = "invalid document content".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult invalidResult = new ValidationResult();
        invalidResult.setDocumentType("INVALID");
        invalidResult.addError("Missing required header");
        invalidResult.addError("Invalid footer format");
        invalidResult.addDetail("Total pages: 5");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(invalidResult);

            HttpResponseMessage result = processor.validateTemplate(request, context);

            verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
            verify(responseBuilder).body(argThat(body ->
                    body.toString().contains("Document validation failed with errors") &&
                            body.toString().contains("Document type: INVALID") &&
                            body.toString().contains("Missing required header") &&
                            body.toString().contains("Invalid footer format") &&
                            body.toString().contains("Total pages: 5")));
            verify(responseBuilder).build();
            assertEquals(response, result);
        }
    }

    @Test
    @DisplayName("Should handle IOException from validation helper")
    void testValidateTemplateWithIOException() throws IOException {
        byte[] documentContent = "problematic document content".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenThrow(new IOException("PDF parsing error"));

            assertThrows(IOException.class, () -> {
                processor.validateTemplate(request, context);
            });
        }
    }

    @Test
    @DisplayName("Should handle large document content")
    void testValidateTemplateWithLargeDocument() throws IOException {
        byte[] largeDocumentContent = new byte[1024 * 1024];
        Arrays.fill(largeDocumentContent, (byte) 'A');
        when(request.getBody()).thenReturn(Optional.of(largeDocumentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("MULTILINGUAL");
        validResult.addDetail("Large document processed successfully");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(largeDocumentContent))
                    .thenReturn(validResult);

            HttpResponseMessage result = processor.validateTemplate(request, context);

            verify(request).createResponseBuilder(HttpStatus.OK);
            verify(responseBuilder).body(argThat(body ->
                    body.toString().contains("Document type: MULTILINGUAL")));
            assertEquals(response, result);
        }
    }

    @Test
    @DisplayName("Should format success response correctly with multiple details")
    void testSuccessResponseFormatting() throws IOException {
        byte[] documentContent = "test document".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");
        validResult.addDetail("Detail 1: Page count verified");
        validResult.addDetail("Detail 2: Headers validated");
        validResult.addDetail("Detail 3: Footers validated");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            processor.validateTemplate(request, context);

            verify(responseBuilder).body(argThat(body -> {
                String bodyStr = body.toString();
                return bodyStr.contains("Document validated successfully:\n") &&
                        bodyStr.contains("Document type: ENGLISH\n") &&
                        bodyStr.contains("  * Detail 1: Page count verified\n") &&
                        bodyStr.contains("  * Detail 2: Headers validated\n") &&
                        bodyStr.contains("  * Detail 3: Footers validated\n");
            }));
        }
    }

    @Test
    @DisplayName("Should format error response correctly with multiple errors and details")
    void testErrorResponseFormatting() throws IOException {
        byte[] documentContent = "test document".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult invalidResult = new ValidationResult();
        invalidResult.setDocumentType("INVALID");
        invalidResult.addError("Error 1: Missing header");
        invalidResult.addError("Error 2: Invalid footer");
        invalidResult.addDetail("Detail 1: Total pages found: 3");
        invalidResult.addDetail("Detail 2: Document size: 1024 bytes");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(invalidResult);

            processor.validateTemplate(request, context);

            verify(responseBuilder).body(argThat(body -> {
                String bodyStr = body.toString();
                return bodyStr.contains("Document validation failed with errors:\n") &&
                        bodyStr.contains("Document type: INVALID\n") &&
                        bodyStr.contains("- Error 1: Missing header\n") &&
                        bodyStr.contains("- Error 2: Invalid footer\n") &&
                        bodyStr.contains("  * Detail 1: Total pages found: 3\n") &&
                        bodyStr.contains("  * Detail 2: Document size: 1024 bytes\n");
            }));
        }
    }

    @Test
    @DisplayName("Should handle validation result with null document type")
    void testValidationWithNullDocumentType() throws IOException {
        byte[] documentContent = "test document".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType(null);
        validResult.addDetail("Processed successfully");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            processor.validateTemplate(request, context);

            verify(responseBuilder).body(argThat(body -> {
                String bodyStr = body.toString();
                return bodyStr.contains("Document type: null");
            }));
        }
    }

    @Test
    @DisplayName("Should handle validation result with empty lists")
    void testValidationWithEmptyLists() throws IOException {
        byte[] documentContent = "test document".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            processor.validateTemplate(request, context);

            verify(request).createResponseBuilder(HttpStatus.OK);
            verify(responseBuilder).body(argThat(body -> {
                String bodyStr = body.toString();
                return bodyStr.contains("Document validated successfully:\n") &&
                        bodyStr.contains("Document type: ENGLISH\n") &&
                        !bodyStr.contains("  * ");
            }));
        }
    }

    @Test
    @DisplayName("Should log document size information")
    void testDocumentSizeLogging() throws IOException {
        byte[] documentContent = "test document content for size logging".getBytes();
        when(request.getBody()).thenReturn(Optional.of(documentContent));

        ValidationResult validResult = new ValidationResult();
        validResult.setDocumentType("ENGLISH");

        try (MockedStatic<TemplateValidationHelper> mockedHelper = mockStatic(TemplateValidationHelper.class)) {
            mockedHelper.when(() -> TemplateValidationHelper.validateDocument(documentContent))
                    .thenReturn(validResult);

            HttpResponseMessage result = processor.validateTemplate(request, context);

            assertNotNull(result);
            verify(request).createResponseBuilder(HttpStatus.OK);
        }
    }
}