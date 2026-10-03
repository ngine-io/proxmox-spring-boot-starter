package io.ngine.proxmox.autoconfigure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Proxmox VE client.
 */
@ConfigurationProperties("proxmox")
public class ProxmoxProperties {

    /**
     * URL of a Proxmox node or cluster endpoint, e.g. https://pve.example.com:8006.
     */
    private String url;

    /**
     * API token id including user and realm, e.g. automation@pve!ci. Takes precedence over
     * username and password.
     */
    private String tokenId;

    /**
     * API token secret.
     */
    private String tokenSecret;

    /**
     * User for ticket authentication, with or without realm.
     */
    private String username;

    /**
     * Password for ticket authentication.
     */
    private String password;

    /**
     * Realm appended to the username if it does not contain one.
     */
    private String realm = "pam";

    /**
     * Connect timeout.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Read timeout.
     */
    private Duration readTimeout = Duration.ofSeconds(60);

    private final Ssl ssl = new Ssl();

    private final Task task = new Task();

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTokenId() {
        return this.tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenSecret() {
        return this.tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealm() {
        return this.realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Duration getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Ssl getSsl() {
        return this.ssl;
    }

    public Task getTask() {
        return this.task;
    }

    public static class Ssl {

        /**
         * Name of the SSL bundle (spring.ssl.bundle.*) used to trust the Proxmox
         * certificate, e.g. one containing /etc/pve/pve-root-ca.pem.
         */
        private String bundle;

        /**
         * Accept any certificate and host name. For development only.
         */
        private boolean insecure;

        public String getBundle() {
            return this.bundle;
        }

        public void setBundle(String bundle) {
            this.bundle = bundle;
        }

        public boolean isInsecure() {
            return this.insecure;
        }

        public void setInsecure(boolean insecure) {
            this.insecure = insecure;
        }
    }

    public static class Task {

        /**
         * Interval between task status polls.
         */
        private Duration pollInterval = Duration.ofSeconds(1);

        /**
         * Default timeout when waiting for a task.
         */
        private Duration timeout = Duration.ofMinutes(10);

        public Duration getPollInterval() {
            return this.pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Duration getTimeout() {
            return this.timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
