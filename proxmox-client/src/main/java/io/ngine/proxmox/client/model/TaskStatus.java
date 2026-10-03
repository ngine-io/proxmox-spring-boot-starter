package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of {@code GET /nodes/{node}/tasks/{upid}/status}.
 *
 * @param status {@code running} or {@code stopped}
 * @param exitstatus {@code OK}, {@code WARNINGS: n} or an error message; only set once stopped
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatus(String upid, String node, String type, String id, String user, String status,
        String exitstatus, Long starttime) {

    public boolean isRunning() {
        return "running".equals(this.status);
    }

    /**
     * Whether the task finished without errors (warnings count as success).
     */
    public boolean isSuccessful() {
        return !isRunning() && this.exitstatus != null
                && (this.exitstatus.equals("OK") || this.exitstatus.startsWith("WARNINGS"));
    }
}
