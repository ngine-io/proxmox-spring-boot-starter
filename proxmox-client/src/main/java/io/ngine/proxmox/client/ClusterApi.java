package io.ngine.proxmox.client;

import java.util.List;
import java.util.Map;

import io.ngine.proxmox.client.model.ClusterResource;
import io.ngine.proxmox.client.model.ClusterResourceType;

/**
 * Cluster wide information ({@code /cluster}).
 */
public final class ClusterApi {

    private final ApiSupport api;

    ClusterApi(ApiSupport api) {
        this.api = api;
    }

    /**
     * All resources (guests, nodes, storages, ...) of the cluster.
     */
    public List<ClusterResource> resources() {
        return this.api.getList(ClusterResource.class, "/cluster/resources");
    }

    public List<ClusterResource> resources(ClusterResourceType type) {
        return this.api.getList(ClusterResource.class, "/cluster/resources", Map.of("type", type.value()));
    }

    /**
     * The next free VMID.
     */
    public int nextId() {
        return this.api.get(Integer.class, "/cluster/nextid");
    }

    /**
     * Whether the given VMID is free.
     */
    public boolean isFreeId(int vmid) {
        try {
            this.api.get(Integer.class, "/cluster/nextid", Map.of("vmid", vmid));
            return true;
        }
        catch (ProxmoxApiException ex) {
            if (ex.getStatusCode().value() == 400) {
                return false;
            }
            throw ex;
        }
    }
}
