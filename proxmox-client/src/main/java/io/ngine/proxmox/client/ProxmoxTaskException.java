package io.ngine.proxmox.client;

import io.ngine.proxmox.client.model.TaskStatus;
import io.ngine.proxmox.client.model.Upid;

/**
 * Thrown when a Proxmox task fails or does not finish in time.
 */
public class ProxmoxTaskException extends ProxmoxException {

    private final Upid upid;

    private final TaskStatus status;

    public ProxmoxTaskException(Upid upid, TaskStatus status, String message) {
        super(message);
        this.upid = upid;
        this.status = status;
    }

    public ProxmoxTaskException(Upid upid, String message, Throwable cause) {
        super(message, cause);
        this.upid = upid;
        this.status = null;
    }

    public Upid getUpid() {
        return this.upid;
    }

    /**
     * The last observed task status, or {@code null} if none was observed.
     */
    public TaskStatus getStatus() {
        return this.status;
    }
}
