package uk.gov.moj.cpp.bulkscan.azure.rest;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.moj.cpp.bulkscan.azure.rest.DocumentMapper.getDocumentMapper;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class StagingProsecutorCommandHelperTest {

    @Mock
    private static Client client;

    @Mock
    private InputStream inputStream;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response mockResponse;

    @Captor
    private ArgumentCaptor<Entity<MultipartFormDataOutput>> documentCaptor;

    @BeforeEach
    public void onceBeforeEachTest() {
        initMocks(this);
    }


    @Test
    public void addMaterialWithCaseUrn() {
        when(builder.post(any())).thenReturn(mockResponse);
        when(builder.headers(any())).thenReturn(builder);
        when(webTarget.request()).thenReturn(builder);
        final StagingProsecutorCommandHelper stagingProsecutorCommandHelper = new CustomStagingProsecutorCommandHelper();
        when(client.target(stagingProsecutorCommandHelper.getStagingProsecutorMaterialUploadUrlWithCaseUrn("TVL1234556", "GA0001"))).thenReturn(webTarget);
        final JsonObject jsonObject = Json.createObjectBuilder().add("document_name", getDocumentMapper().getSinglePlea())
                .add("file_name", "abcd.pdf")
                .add("asn", "defendantId1")
                .add("case_urn", "TVL1234556")
                .add("prosecutor_ID", "GA0001")
                .add("scanDocumentId", "DOCUMENT_ID")
                .build();
        stagingProsecutorCommandHelper.addMaterial(jsonObject, "ENVELOPE_ID", inputStream);
        verify(builder).post(documentCaptor.capture());
        final MultipartFormDataOutput captorValue = documentCaptor.getValue().getEntity();
        assertThat(captorValue.getFormData().get("material").getMediaType(), is(MediaType.APPLICATION_OCTET_STREAM_TYPE));
        assertThat(captorValue.getFormData().get("material").getEntity(), is(inputStream));
        assertThat(captorValue.getFormData().get("materialType").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("materialType").getEntity(), is("PLEA"));
        assertThat(captorValue.getFormData().get("prosecutingAuthority").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("prosecutingAuthority").getEntity(), is("GA0001"));
        assertThat(captorValue.getFormData().get("defendantId").getEntity(), is("defendantId1"));
        assertThat(captorValue.getFormData().get("defendantId").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("srcDocumentReference").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("srcDocumentReference").getEntity(), is("ENVELOPE_ID:DOCUMENT_ID"));
    }

    @Test
    public void addMaterialWithPtiUrn() {
        when(builder.post(any())).thenReturn(mockResponse);
        when(builder.headers(any())).thenReturn(builder);
        when(webTarget.request()).thenReturn(builder);
        final StagingProsecutorCommandHelper stagingProsecutorCommandHelper = new CustomStagingProsecutorCommandHelper();
        when(client.target(stagingProsecutorCommandHelper.getStagingProsecutorMaterialUploadUrlWithPtiUrn("pti-urn123456789"))).thenReturn(webTarget);
        final JsonObject jsonObject = Json.createObjectBuilder().add("document_name", getDocumentMapper().getSinglePlea())
                .add("file_name", "abcd.pdf")
                .add("asn", "defendantId1")
                .add("case_ptiurn", "example-of-pti-urn")
                .add("scanDocumentId", "DOCUMENT_ID")
                .build();
        stagingProsecutorCommandHelper.addMaterial(jsonObject, "ENVELOPE_ID", inputStream);
        verify(builder).post(documentCaptor.capture());
        final MultipartFormDataOutput captorValue = documentCaptor.getValue().getEntity();
        assertThat(captorValue.getFormData().get("material").getMediaType(), is(MediaType.APPLICATION_OCTET_STREAM_TYPE));
        assertThat(captorValue.getFormData().get("material").getEntity(), is(inputStream));
        assertThat(captorValue.getFormData().get("materialType").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("materialType").getEntity(), is("PLEA"));
        assertThat(captorValue.getFormData().get("defendantId").getEntity(), is("defendantId1"));
        assertThat(captorValue.getFormData().get("defendantId").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("srcDocumentReference").getMediaType(), is(MediaType.TEXT_PLAIN_TYPE));
        assertThat(captorValue.getFormData().get("srcDocumentReference").getEntity(), is("ENVELOPE_ID:DOCUMENT_ID"));
    }

    private static class CustomStagingProsecutorCommandHelper extends StagingProsecutorCommandHelper {
        @Override
        public Client getClient() {
            return client;
        }

        @Override
        public String getCPPUID() {
            return randomUUID().toString();
        }

        @Override
        public String getStagingProsecutorMaterialUploadUrlWithCaseUrn(String caseUrn, String ouCode) {
            return "http://localhost:8080/stagingprosecutor/upload/case-urn";
        }

        @Override
        public String getStagingProsecutorMaterialUploadUrlWithPtiUrn(String ptiUrn) {
            return "http://localhost:8080/stagingprosecutor/upload/pti-urn";
        }
    }
}
