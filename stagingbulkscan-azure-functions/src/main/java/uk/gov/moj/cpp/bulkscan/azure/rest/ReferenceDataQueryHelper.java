package uk.gov.moj.cpp.bulkscan.azure.rest;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static java.lang.System.getenv;

public class ReferenceDataQueryHelper {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final ClientWrapper clientWrapper = new ClientWrapper();

    private static final String EMPTY = "";
    public static final String ACCEPT = "Accept";
    public static final String CJSCPPUID = "CJSCPPUID";
    public static final String EMAIL_DOMAIN = "emailDomain";

    public JsonObject getOuCodeByPtiUrn(final String ptiUrn) {
        final Client client = getClient();
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(ACCEPT, "application/vnd.referencedata.query.prosecutor.by.ptiurn+json");
        headers.add(CJSCPPUID, getCPPUID());
        final Response response = client.target(getReferenceDataPtiApiUrl()).queryParam("ptiurn", ptiUrn).request().headers(headers).get();
        if (response.getStatus() == 404) {
            return Json.createObjectBuilder().add("oucode", EMPTY).build();
        }
        final String responseStr = response.readEntity(String.class);
        return EMPTY.equals(responseStr) ? Json.createObjectBuilder().add("oucode", EMPTY).build() : stringToJsonObjectConverter.convert(responseStr);
    }

    public JsonObject getProsecutorsByOuCode(final String oucode) {
        final Client client = getClient();
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(ACCEPT, "application/vnd.referencedata.query.get.prosecutor+json");
        headers.add(CJSCPPUID, getCPPUID());
        final Response response = client.target(getReferenceDataProsecutorApiUrl()).queryParam("oucode", oucode).request().headers(headers).get();
        if (response.getStatus() == 404) {
            return Json.createObjectBuilder().add("oucode", EMPTY).build();
        }
        final String responseStr = response.readEntity(String.class);
        return EMPTY.equals(responseStr) ? Json.createObjectBuilder().add("shortName", EMPTY).build() : stringToJsonObjectConverter.convert(responseStr);
    }

    public JsonArray getProsecutorByEmailDomain(final String email) {
        final Client client = getClient();
        final String emailDomain = email.substring(email.indexOf('@') + 1);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(ACCEPT, "application/vnd.referencedata.query.get.prosecutorEmailDomain+json");
        headers.add(CJSCPPUID, getCPPUID());
        final Response response = client.target(getReferenceDataProsecutorApiUrl()).queryParam(EMAIL_DOMAIN, emailDomain).request().headers(headers).get();
        if (response.getStatus() == 404) {
            return Json.createArrayBuilder().build();
        }
        final String responseStr = response.readEntity(String.class);
        final JsonObject responseJsonObject = stringToJsonObjectConverter.convert(responseStr);
        final JsonArray prosecutors = responseJsonObject.getJsonArray("prosecutors");
        if (!prosecutors.isEmpty() && !EMPTY.equals(responseStr)) {
            return responseJsonObject.getJsonArray("prosecutors");
        } else {
            final JsonObject jsonObject = Json.createObjectBuilder().add(EMAIL_DOMAIN, EMPTY).build();
            return Json.createArrayBuilder().add(jsonObject).build();
        }
    }

    public Client getClient() {
        return clientWrapper.getClient();
    }

    private String getReferenceDataPtiApiUrl() {
        return getenv("reference_data_query_api_pti_urn_url");
    }

    private String getReferenceDataProsecutorApiUrl() {
        return getenv("reference_data_query_api_prosecutor_url");
    }

    private String getCPPUID() {
        return getenv("BS-CPPUID");
    }
}
