package io.ngine.proxmox.client;

import java.util.List;

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
}
