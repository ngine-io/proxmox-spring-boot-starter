package io.ngine.proxmox.client;

import java.util.Objects;

/**
 * Credentials used to authenticate against the Proxmox API.
 */
public sealed interface ProxmoxCredentials {

    /**
     * API token authentication (recommended).
     *
     * @param tokenId the full token id, e.g. {@code automation@pve!ci}
     * @param secret the token secret (UUID)
     */
    record ApiToken(String tokenId, String secret) implements ProxmoxCredentials {

        public ApiToken {
            Objects.requireNonNull(tokenId, "tokenId must not be null");
            Objects.requireNonNull(secret, "secret must not be null");
        }

        @Override
        public String toString() {
            return "ApiToken[tokenId=" + this.tokenId + ", secret=****]";
        }
    }

    /**
     * Ticket based authentication with username and password.
     *
     * @param username the user id including realm, e.g. {@code root@pam}
     * @param password the password
     */
    record Password(String username, String password) implements ProxmoxCredentials {

        public Password {
            Objects.requireNonNull(username, "username must not be null");
            Objects.requireNonNull(password, "password must not be null");
        }

        @Override
        public String toString() {
            return "Password[username=" + this.username + ", password=****]";
        }
    }
}
