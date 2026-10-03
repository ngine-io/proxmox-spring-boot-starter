package io.ngine.proxmox.client;

import java.util.List;
import java.util.Map;

import io.ngine.proxmox.client.model.StorageContent;
import io.ngine.proxmox.client.model.StorageInfo;

/**
 * Storages as seen from one node ({@code /nodes/{node}/storage}).
 */
public final class StorageApi {

    private final ApiSupport api;

    private final String node;

    StorageApi(ApiSupport api, String node) {
        this.api = api;
        this.node = node;
    }

    public List<StorageInfo> list() {
        return this.api.getList(StorageInfo.class, "/nodes/{node}/storage", this.node);
    }

    public List<StorageContent> content(String storage) {
        return this.api.getList(StorageContent.class, "/nodes/{node}/storage/{storage}/content", this.node, storage);
    }

    /**
     * Content of the given type only, e.g. {@code iso}, {@code vztmpl}, {@code images} or {@code backup}.
     */
    public List<StorageContent> content(String storage, String contentType) {
        return this.api.getList(StorageContent.class, "/nodes/{node}/storage/{storage}/content",
                Map.of("content", contentType), this.node, storage);
    }
}
