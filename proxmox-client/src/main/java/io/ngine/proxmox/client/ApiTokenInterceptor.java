package io.ngine.proxmox.client;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Authenticates requests with a Proxmox API token.
 */
final class ApiTokenInterceptor implements ClientHttpRequestInterceptor {

    private final String authorization;

    ApiTokenInterceptor(ProxmoxCredentials.ApiToken token) {
        this.authorization = "PVEAPIToken=" + token.tokenId() + "=" + token.secret();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, this.authorization);
        return execution.execute(request, body);
    }
}
