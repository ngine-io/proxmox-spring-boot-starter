package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entry of {@code GET /nodes}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Node(String node, String status, Double cpu, Integer maxcpu, Long mem, Long maxmem, Long disk,
        Long maxdisk, Long uptime) {

    public boolean isOnline() {
        return "online".equals(this.status);
    }
}
