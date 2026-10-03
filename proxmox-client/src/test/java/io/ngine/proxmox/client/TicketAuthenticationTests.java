package io.ngine.proxmox.client;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class TicketAuthenticationTests {

    private static final String API = "https://pve.test:8006/api2/json";

    private static final String UPID = "UPID:pve1:0001A2B3:00C4D5E6:65F0A1B2:qmstart:100:root@pam:";

    private MockRestServiceServer server;

    private ProxmoxClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = ProxmoxClient.builder()
            .baseUrl("https://pve.test:8006")
            .credentials(new ProxmoxCredentials.Password("root@pam", "p@ss word"))
            .restClientBuilder(builder)
            .build();
    }

    @Test
    void logsInOnceAndSendsTicketAndCsrfToken() {
        expectLogin("ticket-1", "csrf-1");
        this.server.expect(requestTo(API + "/version"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Cookie", "PVEAuthCookie=ticket-1"))
            .andExpect(headerDoesNotExist("CSRFPreventionToken"))
            .andRespond(withSuccess("{\"data\":{\"version\":\"9.0.3\",\"release\":\"9.0\"}}", MediaType.APPLICATION_JSON));
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/100/status/start"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Cookie", "PVEAuthCookie=ticket-1"))
            .andExpect(header("CSRFPreventionToken", "csrf-1"))
            .andRespond(withSuccess("{\"data\":\"" + UPID + "\"}", MediaType.APPLICATION_JSON));

        assertThat(this.client.version().version()).isEqualTo("9.0.3");
        this.client.qemu("pve1").start(100);

        this.server.verify();
    }

    @Test
    void renewsTicketWhenRejected() {
        expectLogin("ticket-1", "csrf-1");
        this.server.expect(requestTo(API + "/version"))
            .andExpect(header("Cookie", "PVEAuthCookie=ticket-1"))
            .andRespond(withUnauthorizedRequest());
        expectLogin("ticket-2", "csrf-2");
        this.server.expect(requestTo(API + "/version"))
            .andExpect(header("Cookie", "PVEAuthCookie=ticket-2"))
            .andRespond(withSuccess("{\"data\":{\"version\":\"9.0.3\"}}", MediaType.APPLICATION_JSON));

        assertThat(this.client.version().version()).isEqualTo("9.0.3");

        this.server.verify();
    }

    @Test
    void failsOnInvalidCredentials() {
        this.server.expect(requestTo(API + "/access/ticket"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"data\":null,\"message\":\"authentication failure\\n\"}"));

        assertThatExceptionOfType(ProxmoxApiException.class)
            .isThrownBy(() -> this.client.version())
            .satisfies(ex -> {
                assertThat(ex.getStatusCode().value()).isEqualTo(401);
                assertThat(ex.getReason()).isEqualTo("authentication failure");
            });
    }

    private void expectLogin(String ticket, String csrf) {
        this.server.expect(requestTo(API + "/access/ticket"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(headerDoesNotExist("Cookie"))
            .andExpect(content().formData(MultiValueMap.fromSingleValue(Map.of("username", "root@pam", "password", "p@ss word"))))
            .andRespond(withSuccess("{\"data\":{\"ticket\":\"" + ticket + "\",\"CSRFPreventionToken\":\"" + csrf
                    + "\",\"username\":\"root@pam\"}}", MediaType.APPLICATION_JSON));
    }
}
