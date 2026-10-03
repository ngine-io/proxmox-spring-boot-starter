package io.ngine.proxmox.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options for cloning a VM or container.
 */
public final class CloneOptions {

    private final int newId;

    private String name;

    private Boolean full;

    private String target;

    private String storage;

    private String pool;

    private String description;

    private String snapname;

    private CloneOptions(int newId) {
        this.newId = newId;
    }

    /**
     * Clone to the given VMID, see {@link ClusterApi#nextId()}.
     */
    public static CloneOptions newId(int newId) {
        return new CloneOptions(newId);
    }

    /**
     * Name of the new VM, or host name of the new container.
     */
    public CloneOptions name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Create a full copy instead of a linked clone (linked clones require a template).
     */
    public CloneOptions full(boolean full) {
        this.full = full;
        return this;
    }

    /**
     * Target node; only allowed if the source is on shared storage.
     */
    public CloneOptions target(String target) {
        this.target = target;
        return this;
    }

    /**
     * Target storage for full clones.
     */
    public CloneOptions storage(String storage) {
        this.storage = storage;
        return this;
    }

    public CloneOptions pool(String pool) {
        this.pool = pool;
        return this;
    }

    public CloneOptions description(String description) {
        this.description = description;
        return this;
    }

    public CloneOptions snapname(String snapname) {
        this.snapname = snapname;
        return this;
    }

    public int getNewId() {
        return this.newId;
    }

    Map<String, Object> toParams(String nameParameter) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("newid", this.newId);
        params.put(nameParameter, this.name);
        params.put("full", this.full);
        params.put("target", this.target);
        params.put("storage", this.storage);
        params.put("pool", this.pool);
        params.put("description", this.description);
        params.put("snapname", this.snapname);
        return params;
    }
}
