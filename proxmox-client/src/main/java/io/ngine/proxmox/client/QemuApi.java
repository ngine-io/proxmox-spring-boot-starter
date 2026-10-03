package io.ngine.proxmox.client;

import io.ngine.proxmox.client.model.Upid;

/**
 * QEMU virtual machines on one node ({@code /nodes/{node}/qemu}).
 */
public final class QemuApi extends GuestApi {

    QemuApi(ApiSupport api, String node) {
        super(api, node, "qemu", "name");
    }

    /**
     * Hard reset, without shutting down the guest OS.
     */
    public Upid reset(int vmid) {
        return action(vmid, "reset");
    }
}
