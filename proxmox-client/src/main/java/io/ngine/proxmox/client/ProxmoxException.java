package io.ngine.proxmox.client;

/**
 * Base class for all exceptions thrown by the Proxmox client.
 */
public class ProxmoxException extends RuntimeException {

    public ProxmoxException(String message) {
        super(message);
    }

    public ProxmoxException(String message, Throwable cause) {
        super(message, cause);
    }
}
