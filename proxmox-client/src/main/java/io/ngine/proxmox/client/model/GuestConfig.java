package io.ngine.proxmox.client.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Configuration of a QEMU VM or LXC container.
 * <p>
 * The set of keys is open-ended ({@code net0}, {@code scsi0}, {@code mp1}, ...), so the
 * configuration is kept as a map with typed accessors for common keys.
 */
public final class GuestConfig {

    private final Map<String, Object> values;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public GuestConfig(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    @JsonValue
    public Map<String, Object> values() {
        return this.values;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(this.values.get(key)).map(Object::toString);
    }

    public Optional<Integer> getInt(String key) {
        return get(key).map(Integer::valueOf);
    }

    public boolean getBoolean(String key) {
        return get(key).map(value -> value.equals("1") || value.equals("true")).orElse(false);
    }

    /**
     * The VM name (QEMU) or host name (LXC).
     */
    public Optional<String> name() {
        return get("name").or(() -> get("hostname"));
    }

    public Optional<Integer> cores() {
        return getInt("cores");
    }

    /**
     * Memory in MiB.
     */
    public Optional<Integer> memory() {
        return getInt("memory");
    }

    public Optional<String> description() {
        return get("description");
    }

    public boolean isTemplate() {
        return getBoolean("template");
    }

    /**
     * The config digest; pass it as {@code digest} when updating to detect concurrent changes.
     */
    public Optional<String> digest() {
        return get("digest");
    }

    @Override
    public String toString() {
        return "GuestConfig" + this.values;
    }
}
