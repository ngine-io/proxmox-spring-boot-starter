package io.ngine.proxmox.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Request factory that accepts any TLS certificate and host name.
 * <p>
 * <strong>For development only.</strong> Prefer trusting the Proxmox CA
 * ({@code /etc/pve/pve-root-ca.pem}) through an SSL bundle.
 */
public class InsecureClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

    private static final SSLSocketFactory SOCKET_FACTORY = trustAllSocketFactory();

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        if (connection instanceof HttpsURLConnection https) {
            https.setSSLSocketFactory(SOCKET_FACTORY);
            https.setHostnameVerifier((hostname, session) -> true);
        }
        super.prepareConnection(connection, httpMethod);
    }

    private static SSLSocketFactory trustAllSocketFactory() {
        TrustManager trustAll = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { trustAll }, null);
            return context.getSocketFactory();
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to create trust-all SSL context", ex);
        }
    }
}
