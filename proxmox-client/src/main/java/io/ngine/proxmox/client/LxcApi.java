package io.ngine.proxmox.client;

/**
 * LXC containers on one node ({@code /nodes/{node}/lxc}).
 */
public final class LxcApi extends GuestApi {

    LxcApi(ApiSupport api, String node) {
        super(api, node, "lxc", "hostname");
    }
}
