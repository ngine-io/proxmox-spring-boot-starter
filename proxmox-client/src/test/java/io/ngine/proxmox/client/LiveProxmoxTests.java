package io.ngine.proxmox.client;

import java.time.Duration;

import io.ngine.proxmox.client.model.GuestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real Proxmox VE. Configure with:
 * <ul>
 * <li>{@code PVE_URL}, {@code PVE_TOKEN_ID}, {@code PVE_TOKEN_SECRET}: required</li>
 * <li>{@code PVE_INSECURE=true}: accept self-signed certificates</li>
 * <li>{@code PVE_NODE} and {@code PVE_TEMPLATE_VMID}: enable the VM lifecycle test,
 * which clones the template, starts, stops and deletes the clone</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "PVE_URL", matches = ".+")
class LiveProxmoxTests {

    private ProxmoxClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        if (Boolean.parseBoolean(System.getenv("PVE_INSECURE"))) {
            builder.requestFactory(new InsecureClientHttpRequestFactory());
        }
        this.client = ProxmoxClient.builder()
            .baseUrl(System.getenv("PVE_URL"))
            .credentials(new ProxmoxCredentials.ApiToken(System.getenv("PVE_TOKEN_ID"), System.getenv("PVE_TOKEN_SECRET")))
            .restClientBuilder(builder)
            .taskTimeout(Duration.ofMinutes(5))
            .build();
    }

    @Test
    void readsClusterState() {
        assertThat(this.client.version().version()).isNotBlank();
        assertThat(this.client.nodes().list()).isNotEmpty();
        assertThat(this.client.cluster().resources()).isNotEmpty();
        String node = this.client.nodes().list().get(0).node();
        assertThat(this.client.nodes().status(node).pveversion()).startsWith("pve-manager");
        assertThat(this.client.storage(node).list()).isNotEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PVE_TEMPLATE_VMID", matches = "\\d+")
    void runsVmLifecycle() {
        QemuApi qemu = this.client.qemu(System.getenv("PVE_NODE"));
        TasksApi tasks = this.client.tasks();
        int templateId = Integer.parseInt(System.getenv("PVE_TEMPLATE_VMID"));
        int vmid = this.client.cluster().nextId();

        tasks.await(qemu.clone(templateId, CloneOptions.newId(vmid).name("pve-starter-it-" + vmid)));
        try {
            tasks.await(qemu.start(vmid));
            GuestStatus status = qemu.status(vmid);
            assertThat(status.isRunning()).isTrue();
            assertThat(qemu.config(vmid).name()).contains("pve-starter-it-" + vmid);
        }
        finally {
            if (qemu.status(vmid).isRunning()) {
                tasks.await(qemu.stop(vmid));
            }
            tasks.await(qemu.delete(vmid, true));
        }
    }
}
