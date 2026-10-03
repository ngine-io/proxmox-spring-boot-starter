package io.ngine.proxmox.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of {@code GET /nodes/{node}/status}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NodeStatus(String pveversion, String kversion, Long uptime, Double cpu, List<String> loadavg,
        Usage memory, Usage swap, Usage rootfs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(Long total, Long used, Long free) {
    }
}
