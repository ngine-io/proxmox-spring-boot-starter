package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Entry of {@code GET /nodes/{node}/qemu} and {@code GET /nodes/{node}/lxc}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuestSummary(Integer vmid, String name, String status, Integer cpus, Long mem, Long maxmem,
        Long maxdisk, Long uptime, @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean template,
        String tags) {

    public boolean isRunning() {
        return "running".equals(this.status);
    }

    public boolean isTemplate() {
        return Boolean.TRUE.equals(this.template);
    }
}
