package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Entry of {@code GET /nodes/{node}/network}.
 *
 * @param type interface type, e.g. {@code bridge}, {@code OVSBridge}, {@code bond}, {@code eth} or {@code vlan}
 * @param bridgePorts space separated ports of a bridge, e.g. {@code eno1}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkInterface(String iface, String type,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean active,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean autostart,
        @JsonProperty("bridge_ports") String bridgePorts, String cidr, String address, String netmask,
        String gateway, String comments) {

    /**
     * Whether guests can attach to this interface: a Linux or Open vSwitch bridge.
     */
    public boolean isBridge() {
        return "bridge".equals(this.type) || "OVSBridge".equals(this.type);
    }
}
