package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Response of {@code GET /nodes/{node}/{qemu|lxc}/{vmid}/status/current}.
 *
 * @param qmpstatus the QEMU monitor status (QEMU only), e.g. {@code paused}
 * @param lock the active lock, e.g. {@code backup} or {@code clone}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuestStatus(Integer vmid, String name, String status, String qmpstatus, String lock, Double cpu,
        Integer cpus, Long mem, Long maxmem, Long disk, Long maxdisk, Long uptime,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean template, String tags) {

    public boolean isRunning() {
        return "running".equals(this.status);
    }

    public boolean isTemplate() {
        return Boolean.TRUE.equals(this.template);
    }
}
