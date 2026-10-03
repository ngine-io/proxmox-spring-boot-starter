package io.ngine.proxmox.client.model;

/**
 * Filter for {@code GET /cluster/resources}.
 */
public enum ClusterResourceType {

    VM("vm"), STORAGE("storage"), NODE("node"), SDN("sdn");

    private final String value;

    ClusterResourceType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
