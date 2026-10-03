package io.ngine.proxmox.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Entry of {@code GET /nodes/{node}/storage}.
 *
 * @param content comma separated content types, e.g. {@code images,rootdir}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageInfo(String storage, String type, String content,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean active,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean enabled,
        @JsonDeserialize(using = PveBooleanDeserializer.class) Boolean shared, Long total, Long used, Long avail) {
}
