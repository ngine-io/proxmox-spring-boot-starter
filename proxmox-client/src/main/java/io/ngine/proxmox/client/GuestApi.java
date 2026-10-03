package io.ngine.proxmox.client;

import java.util.List;
import java.util.Map;

import io.ngine.proxmox.client.model.GuestConfig;
import io.ngine.proxmox.client.model.GuestStatus;
import io.ngine.proxmox.client.model.GuestSummary;
import io.ngine.proxmox.client.model.Upid;

/**
 * Operations shared by QEMU VMs and LXC containers on one node.
 * <p>
 * Operations returning a {@link Upid} run asynchronously on Proxmox; wait for them with
 * {@link TasksApi#await(Upid)}.
 */
public abstract sealed class GuestApi permits QemuApi, LxcApi {

    private final ApiSupport api;

    private final String node;

    private final String basePath;

    private final String nameParameter;

    GuestApi(ApiSupport api, String node, String type, String nameParameter) {
        this.api = api;
        this.node = node;
        this.basePath = "/nodes/{node}/" + type;
        this.nameParameter = nameParameter;
    }

    public String node() {
        return this.node;
    }

    public List<GuestSummary> list() {
        return this.api.getList(GuestSummary.class, this.basePath, this.node);
    }

    public GuestStatus status(int vmid) {
        return this.api.get(GuestStatus.class, this.basePath + "/{vmid}/status/current", this.node, vmid);
    }

    public GuestConfig config(int vmid) {
        return this.api.get(GuestConfig.class, this.basePath + "/{vmid}/config", this.node, vmid);
    }

    /**
     * Updates the configuration synchronously.
     * <p>
     * Booleans are sent as {@code 0}/{@code 1}; to remove settings pass their keys as
     * {@code delete} (a collection or comma separated string); pass {@code digest} from
     * {@link GuestConfig#digest()} to guard against concurrent modifications.
     */
    public void updateConfig(int vmid, Map<String, ?> changes) {
        this.api.put(this.basePath + "/{vmid}/config", changes, this.node, vmid);
    }

    public Upid start(int vmid) {
        return action(vmid, "start");
    }

    /**
     * Hard stop, like pulling the power plug.
     */
    public Upid stop(int vmid) {
        return action(vmid, "stop");
    }

    /**
     * Graceful shutdown through ACPI (QEMU) or the init system (LXC).
     */
    public Upid shutdown(int vmid) {
        return action(vmid, "shutdown");
    }

    public Upid reboot(int vmid) {
        return action(vmid, "reboot");
    }

    public Upid clone(int vmid, CloneOptions options) {
        return this.api.post(Upid.class, this.basePath + "/{vmid}/clone", options.toParams(this.nameParameter),
                this.node, vmid);
    }

    /**
     * Destroys the guest including its disks. The guest must be stopped.
     */
    public Upid delete(int vmid) {
        return delete(vmid, false);
    }

    /**
     * Destroys the guest including its disks.
     *
     * @param purge also remove the guest from backup jobs, replication and HA
     */
    public Upid delete(int vmid, boolean purge) {
        return this.api.delete(Upid.class, this.basePath + "/{vmid}", Map.of("purge", purge), this.node, vmid);
    }

    Upid action(int vmid, String action) {
        return this.api.post(Upid.class, this.basePath + "/{vmid}/status/{action}", Map.of(), this.node, vmid,
                action);
    }
}
