package uk.gov.moj.cpp.bulkscan.azure.function.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Aes256CbcServiceTest {

    private Aes256CbcService aes256CbcService;

    private TemplateValidationService templateValidationService;

    private static MockWebServer mockWebServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aes256CbcService = new Aes256CbcService();
        templateValidationService = new TemplateValidationService();
    }

    @Test
    public void shouldDecryptValidNppForm() throws Exception {
        final byte[] decryptedFile = aes256CbcService.decryptAes256Cbc(getPdfContent("src/test/resources/test-documents/encryptedValidDocument.pdf"),
                "YmB1H9DbauIBTvznD1CPa3quNpkFhtsrTN+NOzlprBg=", "hT2fgmfrS9Si9LdGGZjUFw==", true, true);

        final String result = templateValidationService.validateTemplate(Optional.of(decryptedFile));

        assertTrue(result.startsWith("Document validated successfully"));
    }

    @Test
    public void shouldDecryptInValidNppForm() throws Exception {
        final byte[] decryptedFile = aes256CbcService.decryptAes256Cbc(getPdfContent("src/test/resources/test-documents/encryptedInvalidDocument.pdf"),
                "YmB1H9DbauIBTvznD1CPa3quNpkFhtsrTN+NOzlprBg=", "hT2fgmfrS9Si9LdGGZjUFw==", true, true);

        final String result = templateValidationService.validateTemplate(Optional.of(decryptedFile));

        assertTrue(result.startsWith("Document validation failed with errors"));
    }

    @Test
    void shouldDownloadFile() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // given
        byte[] expectedData = "Hello test!".getBytes();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(new okio.Buffer().write(expectedData)));

        String fileUrl = mockWebServer.url("/test.txt").toString();

        // when
        byte[] result = aes256CbcService.downloadFile(fileUrl);

        // then
        assertArrayEquals(expectedData, result);

        mockWebServer.shutdown();
    }

    private byte[] getPdfContent(final String first) throws IOException {
        Path pdfFilePath = Paths.get(first);
        if (!Files.exists(pdfFilePath)) {
            throw new IOException("Test PDF file not found at: " + pdfFilePath.toAbsolutePath());
        }
        byte[] pdfContent = Files.readAllBytes(pdfFilePath);
        return pdfContent;
    }
}
