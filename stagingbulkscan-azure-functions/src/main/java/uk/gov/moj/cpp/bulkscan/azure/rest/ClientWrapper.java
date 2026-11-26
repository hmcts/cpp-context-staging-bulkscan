package uk.gov.moj.cpp.bulkscan.azure.rest;

import uk.gov.moj.cpp.bulkscan.azure.exception.SecureConnectionException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

public class ClientWrapper {

    private SSLContext getSslConnectionSocketFactory() {
        SSLContext sslContext;
        try {
            final KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);
            final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            sslContext = SSLContexts.custom().loadTrustMaterial(ks, acceptingTrustStrategy).build();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException ex) {
            throw new SecureConnectionException("Error reading certificate : , %s", ex);
        }
        return sslContext;
    }

    public Client getClient() {
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.sslContext(getSslConnectionSocketFactory());
        clientBuilder.hostnameVerifier(NoopHostnameVerifier.INSTANCE);
        return clientBuilder.build();
    }
}
