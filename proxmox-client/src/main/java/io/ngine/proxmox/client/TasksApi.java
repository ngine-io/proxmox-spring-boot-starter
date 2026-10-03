package io.ngine.proxmox.client;

import java.time.Duration;

import io.ngine.proxmox.client.model.TaskStatus;
import io.ngine.proxmox.client.model.Upid;

/**
 * Asynchronous Proxmox tasks ({@code /nodes/{node}/tasks}).
 */
public final class TasksApi {

    private final ApiSupport api;

    private final Duration pollInterval;

    private final Duration defaultTimeout;

    TasksApi(ApiSupport api, Duration pollInterval, Duration defaultTimeout) {
        this.api = api;
        this.pollInterval = pollInterval;
        this.defaultTimeout = defaultTimeout;
    }

    public TaskStatus status(Upid upid) {
        return this.api.get(TaskStatus.class, "/nodes/{node}/tasks/{upid}/status", upid.node(), upid.value());
    }

    /**
     * Waits for the task to finish using the configured default timeout.
     *
     * @throws ProxmoxTaskException if the task fails or does not finish in time
     */
    public TaskStatus await(Upid upid) {
        return await(upid, this.defaultTimeout);
    }

    /**
     * Waits for the task to finish.
     *
     * @throws ProxmoxTaskException if the task fails or does not finish in time
     */
    public TaskStatus await(Upid upid, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            TaskStatus status = status(upid);
            if (!status.isRunning()) {
                if (status.isSuccessful()) {
                    return status;
                }
                throw new ProxmoxTaskException(upid, status, "Task " + upid + " failed: " + status.exitstatus());
            }
            if (System.nanoTime() - deadline >= 0) {
                throw new ProxmoxTaskException(upid, status, "Task " + upid + " still running after " + timeout);
            }
            sleep(upid);
        }
    }

    private void sleep(Upid upid) {
        try {
            Thread.sleep(this.pollInterval);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProxmoxTaskException(upid, "Interrupted while waiting for task " + upid, ex);
        }
    }
}
