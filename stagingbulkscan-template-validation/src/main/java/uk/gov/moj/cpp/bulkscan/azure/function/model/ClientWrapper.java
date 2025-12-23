package uk.gov.moj.cpp.bulkscan.azure.function.model;

import uk.gov.moj.cpp.bulkscan.azure.function.exception.SecureConnectionException;

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
            final KeyStore keyStore = KeyStore.getInstance("Windows-MY");
            keyStore.load(null, null);
            final TrustStrategy trustStrategy = (cert, authType) -> true;
            sslContext = SSLContexts.custom().loadTrustMaterial(keyStore, trustStrategy).build();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException ex) {
            throw new SecureConnectionException("Error reading certificate : , %s", ex);
        }
        return sslContext;
    }

    public Client getClient() {
        final ClientBuilder builder = ClientBuilder.newBuilder();
        builder.sslContext(getSslConnectionSocketFactory());
        builder.hostnameVerifier(NoopHostnameVerifier.INSTANCE);
        return builder.build();
    }
}
