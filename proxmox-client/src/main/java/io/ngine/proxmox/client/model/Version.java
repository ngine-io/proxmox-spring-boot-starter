package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of {@code GET /version}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Version(String version, String release, String repoid) {
}
