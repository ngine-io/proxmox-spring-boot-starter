package io.ngine.proxmox.client;

import java.util.List;
import java.util.Map;

import io.ngine.proxmox.client.model.NetworkInterface;
import io.ngine.proxmox.client.model.Node;
import io.ngine.proxmox.client.model.NodeStatus;

/**
 * Cluster nodes ({@code /nodes}).
 */
public final class NodesApi {

    private final ApiSupport api;

    NodesApi(ApiSupport api) {
        this.api = api;
    }

    public List<Node> list() {
        return this.api.getList(Node.class, "/nodes");
    }

    public NodeStatus status(String node) {
        return this.api.get(NodeStatus.class, "/nodes/{node}/status", node);
    }

    /**
     * Network interfaces configured on the node ({@code /nodes/{node}/network}).
     */
    public List<NetworkInterface> network(String node) {
        return this.api.getList(NetworkInterface.class, "/nodes/{node}/network", node);
    }

    /**
     * Network interfaces of the given type only, e.g. {@code bridge}, {@code any_bridge} (Linux and Open vSwitch
     * bridges), {@code bond}, {@code eth} or {@code vlan}.
     */
    public List<NetworkInterface> network(String node, String type) {
        return this.api.getList(NetworkInterface.class, "/nodes/{node}/network", Map.of("type", type), node);
    }
}
