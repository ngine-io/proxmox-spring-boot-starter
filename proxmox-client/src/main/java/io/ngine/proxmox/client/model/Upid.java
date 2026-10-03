package io.ngine.proxmox.client.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Unique process id of a Proxmox task, e.g.
 * {@code UPID:pve1:0001A2B3:00C4D5E6:65F0A1B2:qmstart:100:root@pam:}.
 */
public record Upid(String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Upid {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.startsWith("UPID:") || value.split(":").length < 8) {
            throw new IllegalArgumentException("Not a UPID: " + value);
        }
    }

    public static Upid of(String value) {
        return new Upid(value);
    }

    /**
     * The node the task runs on.
     */
    public String node() {
        return part(1);
    }

    /**
     * The task type, e.g. {@code qmstart} or {@code qmclone}.
     */
    public String type() {
        return part(5);
    }

    /**
     * The task subject, usually the VMID; may be empty.
     */
    public String id() {
        return part(6);
    }

    public String user() {
        return part(7);
    }

    private String part(int index) {
        return this.value.split(":")[index];
    }

    @JsonValue
    @Override
    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
