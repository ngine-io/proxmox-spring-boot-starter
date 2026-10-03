package io.ngine.proxmox.autoconfigure;

import io.ngine.proxmox.client.ProxmoxClient;
import io.ngine.proxmox.client.model.Version;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Reports Proxmox as up if {@code GET /version} succeeds.
 */
public class ProxmoxHealthIndicator extends AbstractHealthIndicator {

    private final ProxmoxClient client;

    public ProxmoxHealthIndicator(ProxmoxClient client) {
        super("Proxmox health check failed");
        this.client = client;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Version version = this.client.version();
        builder.up().withDetail("version", version.version()).withDetail("release", version.release());
    }
}
