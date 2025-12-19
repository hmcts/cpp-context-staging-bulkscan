package uk.gov.moj.cpp.bulkscan.azure.rest;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.json.Json;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class StagingBulkScanCommandHelperTest {

    @Mock
    private static Client client;

    private static InputStream providerJsonInputStream;

    private static StagingBulkScanCommandHelper stagingBulkScanCommandHelper;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response mockResponse;

    @Captor
    private ArgumentCaptor<Entity<ProviderPayload>> captor;

    private static ExecutionContext context;

    private static ReferenceDataQueryHelper referenceDataQueryHelper;

    private static Logger logger;

    @BeforeAll
    public static void onceBeforeClass() {
        logger = mock(Logger.class);
        context = mock(ExecutionContext.class);
        referenceDataQueryHelper = mock(ReferenceDataQueryHelper.class);
        when(referenceDataQueryHelper.getProsecutorsByOuCode(any(String.class))).thenReturn(Json.createObjectBuilder().add("shortName", "TVL").build());
        when(context.getLogger()).thenReturn(logger);
        stagingBulkScanCommandHelper = new CustomStagingBulkScanCommandHelper(context, referenceDataQueryHelper);
    }

    @BeforeEach
    public void onceBeforeEachTest() {
        initMocks(this);
    }

    @Test
    public void registerScanEnvelopeCorrectly() {
        providerJsonInputStream = StagingBulkScanCommandHelperTest.class.getResourceAsStream("/scanProviderPayload.json");
        givenAllMocksAreInitialised();
        stagingBulkScanCommandHelper.registerEnvelope(Json.createReader(providerJsonInputStream).readObject());
        verify(builder).post(captor.capture());
        final ProviderPayload providerPayload = captor.getValue().getEntity();
        assertThat(providerPayload.getVendorPOBox(), Is.<String>is("1959"));
        assertThat(providerPayload.getJurisdiction(), Is.<String>is("Crime"));
        assertThat(providerPayload.getAssociatedScanDocuments().size(), Is.is(2));

        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getCaseUrn(), Is.is("TVL12295672"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getDocumentControlNumber(), Is.is("AC12"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getDocumentName(), Is.is("Single Justice Procedure Notice - Plea (Multiple)"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getFileName(), Is.is("18361040100010001.pdf"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(1).getDocumentName(), Is.is("SJPMC100"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(1).getFileName(), Is.is("18361040100010002.pdf"));
    }

    @Test
    public void registerScanEnvelopeCorrectlyWithStatusSetToFollowup() {
        providerJsonInputStream = StagingBulkScanCommandHelperTest.class.getResourceAsStream("/scanProviderPayloadWithNoCaseUrn.json");
        givenAllMocksAreInitialised();
        stagingBulkScanCommandHelper.registerEnvelope(Json.createReader(providerJsonInputStream).readObject());
        verify(builder).post(captor.capture());
        final ProviderPayload captorValue = captor.getValue().getEntity();
        assertThat(captorValue.getAssociatedScanDocuments().size(), Is.is(1));
        assertThat(captorValue.getAssociatedScanDocuments().get(0).getStatus(), is("FOLLOW_UP"));
    }

    @Test
    public void registerScanEnvelopeCorrectlyWithNullAndBlankValues() {
        providerJsonInputStream = StagingBulkScanCommandHelperTest.class.getResourceAsStream("/scanProviderPayloadWithEmptyValues.json");
        givenAllMocksAreInitialised();
        stagingBulkScanCommandHelper.registerEnvelope(Json.createReader(providerJsonInputStream).readObject());
        verify(builder).post(captor.capture());
        final ProviderPayload providerPayload = captor.getValue().getEntity();
        assertThat(providerPayload.getVendorPOBox(), Is.<String>is("1959"));
        assertThat(providerPayload.getJurisdiction(), Is.<String>is("Crime"));
        assertThat(providerPayload.getAssociatedScanDocuments().size(), Is.is(3));

        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getCaseUrn(), Is.is("TVL12295672"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getDocumentControlNumber(), Is.is("AC12"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getDocumentName(), Is.is("Single Justice Procedure Notice - Plea (Multiple)"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getFileName(), Is.is("18361040100010001.pdf"));
        assertNull(providerPayload.getAssociatedScanDocuments().get(0).getProsecutorAuthorityId());
        assertThat(providerPayload.getAssociatedScanDocuments().get(1).getFileName(), Is.is("18361040100010002.pdf"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(1).getDocumentName(), Is.is("SJPMC100"));
        assertNull(providerPayload.getAssociatedScanDocuments().get(1).getProsecutorAuthorityId());
        assertThat(providerPayload.getAssociatedScanDocuments().get(2).getFileName(), Is.is("18361040100010003.pdf"));
        assertNull(providerPayload.getAssociatedScanDocuments().get(2).getProsecutorAuthorityId());
    }

    @Test
    public void registerScanEnvelopeWithNullAndBlankValuesForDocumentName() {
        providerJsonInputStream = StagingBulkScanCommandHelperTest.class.getResourceAsStream("/scanProviderDocumentPayloadWithEmptyValues.json");
        givenAllMocksAreInitialised();
        stagingBulkScanCommandHelper.registerEnvelope(Json.createReader(providerJsonInputStream).readObject());
        verify(builder).post(captor.capture());
        final ProviderPayload providerPayload = captor.getValue().getEntity();
        assertThat(providerPayload.getVendorPOBox(), Is.<String>is("1959"));
        assertThat(providerPayload.getJurisdiction(), Is.<String>is("Crime"));
        assertThat(providerPayload.getAssociatedScanDocuments().size(), Is.is(4));

        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getCaseUrn(), Is.is("TVL12295672"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(0).getDocumentName(), Is.is("Single Justice Procedure Notice - Plea (Multiple)"));
        assertThat(providerPayload.getAssociatedScanDocuments().get(1).getDocumentName(), Is.is(""));
        assertThat(providerPayload.getAssociatedScanDocuments().get(2).getDocumentName(), Is.is(""));
        assertThat(providerPayload.getAssociatedScanDocuments().get(3).getDocumentName(), Is.is(""));
    }

    private void givenAllMocksAreInitialised() {
        when(mockResponse.getStatus()).thenReturn(HttpStatus.ACCEPTED.value());
        when(builder.post(any())).thenReturn(mockResponse);
        when(builder.headers(any())).thenReturn(builder);
        when(webTarget.request()).thenReturn(builder);
        when(client.target(stagingBulkScanCommandHelper.getBulkScanAPIUrl())).thenReturn(webTarget);
    }

    private static class CustomStagingBulkScanCommandHelper extends StagingBulkScanCommandHelper {

        public CustomStagingBulkScanCommandHelper(final ExecutionContext context, final ReferenceDataQueryHelper referenceDataQueryHelper) {
            super(context, referenceDataQueryHelper);
        }

        @Override
        public Client getClient() {
            return client;
        }

        @Override
        public String getBulkScanAPIUrl() {
            return "http://stagingbulkscanapi-url.com";
        }

        @Override
        public String getBulkScanAPIContentType() {
            return "application/vnd.stagingbulkscan.register-envelope+json";
        }

        @Override
        public String getCPPUID() {
            return randomUUID().toString();
        }
    }
}
