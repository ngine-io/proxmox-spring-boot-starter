package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entry of {@code GET /nodes/{node}/storage/{storage}/content}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageContent(String volid, String content, String format, Long size, Integer vmid, Long ctime) {
}
