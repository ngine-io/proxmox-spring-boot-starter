package io.ngine.proxmox.client;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Authenticates requests with a Proxmox ticket obtained via {@code POST /access/ticket}.
 * <p>
 * Tickets are valid for two hours; they are renewed after {@link #TICKET_LIFETIME} and
 * whenever Proxmox answers with {@code 401}. Write requests additionally carry the
 * {@code CSRFPreventionToken} header.
 */
final class TicketInterceptor implements ClientHttpRequestInterceptor {

    static final Duration TICKET_LIFETIME = Duration.ofMinutes(90);

    private final ApiSupport loginApi;

    private final ProxmoxCredentials.Password credentials;

    private final Clock clock;

    private Ticket ticket;

    TicketInterceptor(ApiSupport loginApi, ProxmoxCredentials.Password credentials, Clock clock) {
        this.loginApi = loginApi;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        Ticket current = ticket(null);
        apply(request, current);
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
            return response;
        }
        response.close();
        apply(request, ticket(current));
        return execution.execute(request, body);
    }

    private void apply(HttpRequest request, Ticket ticket) {
        HttpHeaders headers = request.getHeaders();
        headers.set(HttpHeaders.COOKIE, "PVEAuthCookie=" + ticket.ticket());
        if (!HttpMethod.GET.equals(request.getMethod()) && !HttpMethod.HEAD.equals(request.getMethod())) {
            headers.set("CSRFPreventionToken", ticket.csrfPreventionToken());
        }
    }

    /**
     * Returns a valid ticket, logging in if there is none, it expired or it equals the
     * given rejected ticket.
     */
    private synchronized Ticket ticket(Ticket rejected) {
        if (this.ticket == null || this.ticket == rejected
                || this.clock.instant().isAfter(this.ticket.obtainedAt().plus(TICKET_LIFETIME))) {
            this.ticket = login();
        }
        return this.ticket;
    }

    private Ticket login() {
        LoginResponse response = this.loginApi.post(LoginResponse.class, "/access/ticket",
                Map.of("username", this.credentials.username(), "password", this.credentials.password()));
        if (response == null || response.ticket() == null) {
            throw new ProxmoxException("Login of " + this.credentials.username() + " returned no ticket");
        }
        return new Ticket(response.ticket(), response.csrfPreventionToken(), this.clock.instant());
    }

    private record Ticket(String ticket, String csrfPreventionToken, Instant obtainedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LoginResponse(String ticket, @JsonProperty("CSRFPreventionToken") String csrfPreventionToken) {
    }
}
