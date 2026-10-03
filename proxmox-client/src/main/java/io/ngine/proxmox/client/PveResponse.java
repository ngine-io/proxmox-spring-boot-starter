package io.ngine.proxmox.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The {@code {"data": ...}} envelope every Proxmox API response is wrapped in.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PveResponse<T>(T data) {
}
