package io.ngine.proxmox.autoconfigure;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import io.ngine.proxmox.client.ProxmoxClient;
import io.ngine.proxmox.client.ProxmoxCredentials;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProxmoxAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ProxmoxAutoConfiguration.class,
                ProxmoxHealthContributorAutoConfiguration.class, RestClientAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
                SslAutoConfiguration.class));

    @Test
    void backsOffWithoutUrl() {
        this.contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ProxmoxClient.class);
            assertThat(context).doesNotHaveBean(ProxmoxHealthIndicator.class);
        });
    }

    @Test
    void createsClientWithApiToken() {
        this.contextRunner
            .withPropertyValues("proxmox.url=https://pve.test:8006", "proxmox.token-id=automation@pve!ci",
                    "proxmox.token-secret=s3cret")
            .run(context -> {
                assertThat(context).hasSingleBean(ProxmoxClient.class);
                assertThat(context).hasSingleBean(ProxmoxHealthIndicator.class);
            });
    }

    @Test
    void failsWithoutCredentials() {
        this.contextRunner.withPropertyValues("proxmox.url=https://pve.test:8006")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("No Proxmox credentials configured"));
    }

    @Test
    void failsWithoutTokenSecret() {
        this.contextRunner.withPropertyValues("proxmox.url=https://pve.test:8006", "proxmox.token-id=a@pve!b")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("proxmox.token-secret must be set"));
    }

    @Test
    void appendsDefaultRealmToUsername() {
        ProxmoxProperties properties = new ProxmoxProperties();
        properties.setUsername("automation");
        properties.setPassword("secret");
        properties.setRealm("pve");

        assertThat(ProxmoxAutoConfiguration.credentials(properties))
            .isEqualTo(new ProxmoxCredentials.Password("automation@pve", "secret"));

        properties.setUsername("root@pam");
        assertThat(ProxmoxAutoConfiguration.credentials(properties))
            .isEqualTo(new ProxmoxCredentials.Password("root@pam", "secret"));
    }

    @Test
    void backsOffForUserDefinedClient() {
        ProxmoxClient custom = ProxmoxClient.builder()
            .baseUrl("https://other:8006")
            .credentials(new ProxmoxCredentials.ApiToken("a@pve!b", "c"))
            .build();
        this.contextRunner.withPropertyValues("proxmox.url=https://pve.test:8006")
            .withBean(ProxmoxClient.class, () -> custom)
            .run(context -> assertThat(context).getBean(ProxmoxClient.class).isSameAs(custom));
    }

    @Test
    void healthIndicatorCanBeDisabled() {
        this.contextRunner
            .withPropertyValues("proxmox.url=https://pve.test:8006", "proxmox.token-id=a@pve!b",
                    "proxmox.token-secret=c", "management.health.proxmox.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(ProxmoxClient.class);
                assertThat(context).doesNotHaveBean(ProxmoxHealthIndicator.class);
            });
    }

    @Test
    void configuredClientTalksToProxmox() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api2/json/version", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"data\":{\"version\":\"9.0.3\",\"release\":\"9.0\",\"repoid\":\"abc\"}}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            this.contextRunner
                .withPropertyValues("proxmox.url=http://localhost:" + server.getAddress().getPort(),
                        "proxmox.token-id=automation@pve!ci", "proxmox.token-secret=s3cret")
                .run(context -> {
                    assertThat(context.getBean(ProxmoxClient.class).version().version()).isEqualTo("9.0.3");
                    assertThat(authorization).hasValue("PVEAPIToken=automation@pve!ci=s3cret");
                    assertThat(context.getBean(ProxmoxHealthIndicator.class).health().getStatus())
                        .isEqualTo(Status.UP);
                });
        }
        finally {
            server.stop(0);
        }
    }
}
