package io.ngine.proxmox.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

/**
 * Thrown when the Proxmox API answers with an error status.
 * <p>
 * Proxmox puts the error message into the HTTP reason phrase (and, on newer versions,
 * into a {@code message} field) and reports parameter validation problems per field
 * in an {@code errors} object.
 */
public class ProxmoxApiException extends ProxmoxException {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpStatusCode statusCode;

    private final String reason;

    private final Map<String, String> errors;

    public ProxmoxApiException(HttpStatusCode statusCode, String reason, Map<String, String> errors, String message) {
        super(message);
        this.statusCode = statusCode;
        this.reason = reason;
        this.errors = Map.copyOf(errors);
    }

    public HttpStatusCode getStatusCode() {
        return this.statusCode;
    }

    /**
     * The error message reported by Proxmox, e.g. {@code "Parameter verification failed."}.
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Per-parameter validation errors, empty if Proxmox did not report any.
     */
    public Map<String, String> getErrors() {
        return this.errors;
    }

    static void raise(HttpRequest request, ClientHttpResponse response) throws IOException {
        throw from(request, response);
    }

    static ProxmoxApiException from(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String reason = response.getStatusText();
        Map<String, String> errors = new LinkedHashMap<>();
        JsonNode body = readBody(response);
        if (body != null) {
            if (StringUtils.hasText(body.path("message").asText(null))) {
                reason = body.path("message").asText().strip();
            }
            body.path("errors").properties()
                    .forEach(entry -> errors.put(entry.getKey(), entry.getValue().asText().strip()));
        }
        StringBuilder message = new StringBuilder()
                .append(request.getMethod()).append(' ').append(request.getURI().getPath())
                .append(" failed with ").append(status.value());
        if (StringUtils.hasText(reason)) {
            message.append(": ").append(reason);
        }
        if (!errors.isEmpty()) {
            message.append(' ').append(errors);
        }
        return new ProxmoxApiException(status, reason, errors, message.toString());
    }

    private static JsonNode readBody(ClientHttpResponse response) {
        try (InputStream body = response.getBody()) {
            byte[] bytes = body.readAllBytes();
            return bytes.length > 0 ? MAPPER.readTree(bytes) : null;
        }
        catch (IOException ex) {
            return null;
        }
    }
}
