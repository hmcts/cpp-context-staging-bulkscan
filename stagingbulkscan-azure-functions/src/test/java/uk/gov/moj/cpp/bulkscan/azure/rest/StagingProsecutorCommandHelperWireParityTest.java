package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.bulkscan.azure.rest.DocumentMapper.getDocumentMapper;

/**
 * J17/J25 parity characterisation (J17 golden master), BSC-M02.
 *
 * Captures the multipart request that StagingProsecutorCommandHelper.addMaterial actually sends, using a local HTTP
 * server. The helper code is unchanged by the J25 upgrade, but the RESTEasy multipart provider moves from 3.15 to
 * 7.0: on J17 the material part's filename is written as UTF-8, on J25 a non-ASCII character becomes '?'.
 * The existing StagingProsecutorCommandHelperTest mocks the Client, so it cannot see the wire format.
 */
class StagingProsecutorCommandHelperWireParityTest {

    private static final String NON_ASCII_FILE_NAME = "fé 1.pdf";

    private final AtomicReference<byte[]> capturedBody = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                capturedBody.set(body.readAllBytes());
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void bscM02_materialPartShouldCarryTheNonAsciiFileNameAsUtf8() {
        final JsonObject material = createObjectBuilder()
                .add("document_name", getDocumentMapper().getSinglePlea())
                .add("file_name", NON_ASCII_FILE_NAME)
                .add("asn", "defendantId1")
                .add("case_urn", "TVL1234556")
                .add("prosecutor_ID", "GA0001")
                .add("scanDocumentId", "DOCUMENT_ID")
                .build();

        final Response response = new LocalStagingProsecutorCommandHelper("http://localhost:" + server.getAddress().getPort())
                .addMaterial(material, "ENVELOPE_ID", new ByteArrayInputStream("%PDF-1.4".getBytes(UTF_8)));

        assertThat(response.getStatus(), is(200));
        final String body = new String(capturedBody.get(), UTF_8);
        assertThat(body, containsString("name=\"material\"; filename=\"" + NON_ASCII_FILE_NAME + "\""));
        assertThat(body, containsString("name=\"materialType\""));
        assertThat(body, containsString("name=\"prosecutingAuthority\""));
        assertThat(body, containsString("name=\"srcDocumentReference\""));
        assertThat(body, containsString("name=\"defendantId\""));
    }

    private static class LocalStagingProsecutorCommandHelper extends StagingProsecutorCommandHelper {

        private final String baseUrl;

        LocalStagingProsecutorCommandHelper(final String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public Client getClient() {
            return ClientBuilder.newClient();
        }

        @Override
        public String getCPPUID() {
            return "cppuid";
        }

        @Override
        protected String getStagingProsecutorMaterialUploadUrlWithCaseUrn(final String caseUrn, final String ouCode) {
            return baseUrl + "/material/" + caseUrn + "/" + ouCode;
        }
    }
}
