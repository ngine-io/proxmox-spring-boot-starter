package io.ngine.proxmox.client;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import io.ngine.proxmox.client.model.Version;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Entry point to the Proxmox VE API.
 * <pre class="code">
 * ProxmoxClient proxmox = ProxmoxClient.builder()
 *     .baseUrl("https://pve.example.com:8006")
 *     .credentials(new ProxmoxCredentials.ApiToken("automation@pve!ci", secret))
 *     .build();
 *
 * Upid upid = proxmox.qemu("pve1").start(100);
 * proxmox.tasks().await(upid);
 * </pre>
 * Instances are thread-safe.
 */
public final class ProxmoxClient {

    private final ApiSupport api;

    private final NodesApi nodes;

    private final ClusterApi cluster;

    private final TasksApi tasks;

    private ProxmoxClient(ApiSupport api, Duration taskPollInterval, Duration taskTimeout) {
        this.api = api;
        this.nodes = new NodesApi(api);
        this.cluster = new ClusterApi(api);
        this.tasks = new TasksApi(api, taskPollInterval, taskTimeout);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Version version() {
        return this.api.get(Version.class, "/version");
    }

    public NodesApi nodes() {
        return this.nodes;
    }

    public ClusterApi cluster() {
        return this.cluster;
    }

    public QemuApi qemu(String node) {
        return new QemuApi(this.api, node);
    }

    public LxcApi lxc(String node) {
        return new LxcApi(this.api, node);
    }

    public StorageApi storage(String node) {
        return new StorageApi(this.api, node);
    }

    public TasksApi tasks() {
        return this.tasks;
    }

    public static final class Builder {

        private static final String API_PATH = "/api2/json";

        private String baseUrl;

        private ProxmoxCredentials credentials;

        private RestClient.Builder restClientBuilder;

        private Duration taskPollInterval = Duration.ofSeconds(1);

        private Duration taskTimeout = Duration.ofMinutes(10);

        private Builder() {
        }

        /**
         * URL of a Proxmox node or cluster endpoint, e.g. {@code https://pve.example.com:8006}.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder credentials(ProxmoxCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * The builder to derive the HTTP client from, e.g. to configure TLS, timeouts or
         * observability. It is copied, not modified.
         */
        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public Builder taskPollInterval(Duration taskPollInterval) {
            this.taskPollInterval = taskPollInterval;
            return this;
        }

        /**
         * Default timeout of {@link TasksApi#await(io.ngine.proxmox.client.model.Upid)}.
         */
        public Builder taskTimeout(Duration taskTimeout) {
            this.taskTimeout = taskTimeout;
            return this;
        }

        public ProxmoxClient build() {
            Objects.requireNonNull(this.baseUrl, "baseUrl must not be null");
            Objects.requireNonNull(this.credentials, "credentials must not be null");
            RestClient.Builder builder = (this.restClientBuilder != null ? this.restClientBuilder.clone()
                    : RestClient.builder())
                .baseUrl(apiUrl(this.baseUrl))
                .defaultStatusHandler(HttpStatusCode::isError, ProxmoxApiException::raise);
            ClientHttpRequestInterceptor authentication = switch (this.credentials) {
                case ProxmoxCredentials.ApiToken token -> new ApiTokenInterceptor(token);
                case ProxmoxCredentials.Password password ->
                    new TicketInterceptor(new ApiSupport(builder.clone().build()), password, Clock.systemUTC());
            };
            RestClient restClient = builder.requestInterceptor(authentication).build();
            return new ProxmoxClient(new ApiSupport(restClient), this.taskPollInterval, this.taskTimeout);
        }

        static String apiUrl(String baseUrl) {
            String url = baseUrl.strip();
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            return url.endsWith(API_PATH) ? url : url + API_PATH;
        }
    }
}
