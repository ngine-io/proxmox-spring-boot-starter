package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Entry of {@code GET /cluster/resources}. Which fields are set depends on {@link #type()}
 * ({@code qemu}, {@code lxc}, {@code node}, {@code storage}, ...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClusterResource(String id, String type, String node, Integer vmid, String name, String status,
        Double cpu, Integer maxcpu, Long mem, Long maxmem, Long disk, Long maxdisk, Long uptime,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean template, String storage, String pool,
        String tags, String hastate) {

    public boolean isTemplate() {
        return Boolean.TRUE.equals(this.template);
    }
}
