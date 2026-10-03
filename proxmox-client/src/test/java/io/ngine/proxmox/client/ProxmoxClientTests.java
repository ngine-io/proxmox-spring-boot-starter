package io.ngine.proxmox.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.ngine.proxmox.client.model.ClusterResource;
import io.ngine.proxmox.client.model.ClusterResourceType;
import io.ngine.proxmox.client.model.GuestConfig;
import io.ngine.proxmox.client.model.GuestStatus;
import io.ngine.proxmox.client.model.Node;
import io.ngine.proxmox.client.model.StorageInfo;
import io.ngine.proxmox.client.model.TaskStatus;
import io.ngine.proxmox.client.model.Upid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProxmoxClientTests {

    private static final String API = "https://pve.test:8006/api2/json";

    private static final String UPID = "UPID:pve1:0001A2B3:00C4D5E6:65F0A1B2:qmstart:100:automation@pve!ci:";

    private MockRestServiceServer server;

    private ProxmoxClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = ProxmoxClient.builder()
            .baseUrl("https://pve.test:8006/")
            .credentials(new ProxmoxCredentials.ApiToken("automation@pve!ci", "s3cret"))
            .restClientBuilder(builder)
            .taskPollInterval(Duration.ofMillis(1))
            .build();
    }

    @Test
    void sendsApiTokenAndUnwrapsEnvelope() {
        this.server.expect(requestTo(API + "/nodes"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "PVEAPIToken=automation@pve!ci=s3cret"))
            .andRespond(json("""
                    {"data":[{"node":"pve1","status":"online","cpu":0.05,"maxcpu":16,"mem":1024,"maxmem":4096,"uptime":42}]}
                    """));

        List<Node> nodes = this.client.nodes().list();

        assertThat(nodes).singleElement().satisfies(node -> {
            assertThat(node.node()).isEqualTo("pve1");
            assertThat(node.isOnline()).isTrue();
            assertThat(node.maxcpu()).isEqualTo(16);
        });
        this.server.verify();
    }

    @Test
    void filtersClusterResourcesAndParsesNumericBooleans() {
        this.server.expect(requestTo(API + "/cluster/resources?type=vm"))
            .andRespond(json("""
                    {"data":[
                      {"id":"qemu/100","type":"qemu","node":"pve1","vmid":100,"name":"tpl","template":1,"status":"stopped"},
                      {"id":"lxc/101","type":"lxc","node":"pve1","vmid":101,"name":"ct","template":"0","status":"running"},
                      {"id":"qemu/102","type":"qemu","node":"pve1","vmid":"102","name":"vm","status":"running"}
                    ]}
                    """));

        List<ClusterResource> resources = this.client.cluster().resources(ClusterResourceType.VM);

        assertThat(resources).extracting(ClusterResource::isTemplate).containsExactly(true, false, false);
        assertThat(resources).extracting(ClusterResource::vmid).containsExactly(100, 101, 102);
    }

    @Test
    void parsesNextIdReturnedAsString() {
        this.server.expect(requestTo(API + "/cluster/nextid")).andRespond(json("{\"data\":\"105\"}"));

        assertThat(this.client.cluster().nextId()).isEqualTo(105);
    }

    @Test
    void checksWhetherIdIsFree() {
        this.server.expect(requestTo(API + "/cluster/nextid?vmid=100"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"data\":null,\"errors\":{\"vmid\":\"VM 100 already exists\"}}"));
        this.server.expect(requestTo(API + "/cluster/nextid?vmid=200")).andRespond(json("{\"data\":\"200\"}"));

        assertThat(this.client.cluster().isFreeId(100)).isFalse();
        assertThat(this.client.cluster().isFreeId(200)).isTrue();
    }

    @Test
    void mapsErrorsToProxmoxApiException() {
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/100/config"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"data\":null,\"errors\":{\"memory\":\"value must have a minimum value of 16\\n\"}}"));

        assertThatExceptionOfType(ProxmoxApiException.class)
            .isThrownBy(() -> this.client.qemu("pve1").updateConfig(100, Map.of("memory", 1)))
            .satisfies(ex -> {
                assertThat(ex.getStatusCode().value()).isEqualTo(400);
                assertThat(ex.getErrors()).containsEntry("memory", "value must have a minimum value of 16");
                assertThat(ex.getMessage()).startsWith("PUT /api2/json/nodes/pve1/qemu/100/config failed with 400");
            });
    }

    @Test
    void usesMessageFieldAsReason() {
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/999/status/current"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                .body("{\"data\":null,\"message\":\"Configuration file 'nodes/pve1/qemu-server/999.conf' does not exist\\n\"}"));

        assertThatExceptionOfType(ProxmoxApiException.class)
            .isThrownBy(() -> this.client.qemu("pve1").status(999))
            .satisfies(ex -> assertThat(ex.getReason())
                .isEqualTo("Configuration file 'nodes/pve1/qemu-server/999.conf' does not exist"));
    }

    @Test
    void readsGuestStatusAndConfig() {
        this.server.expect(requestTo(API + "/nodes/pve1/lxc/101/status/current"))
            .andRespond(json("""
                    {"data":{"vmid":101,"name":"ct","status":"running","cpus":2,"maxmem":536870912,"uptime":10}}
                    """));
        this.server.expect(requestTo(API + "/nodes/pve1/lxc/101/config"))
            .andRespond(json("""
                    {"data":{"hostname":"ct","cores":2,"memory":"512","onboot":1,"net0":"name=eth0,bridge=vmbr0","digest":"abc"}}
                    """));

        GuestStatus status = this.client.lxc("pve1").status(101);
        GuestConfig config = this.client.lxc("pve1").config(101);

        assertThat(status.isRunning()).isTrue();
        assertThat(config.name()).contains("ct");
        assertThat(config.cores()).contains(2);
        assertThat(config.memory()).contains(512);
        assertThat(config.getBoolean("onboot")).isTrue();
        assertThat(config.get("net0")).contains("name=eth0,bridge=vmbr0");
        assertThat(config.digest()).contains("abc");
    }

    @Test
    void encodesWriteParametersAsForm() {
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/9000/clone"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string("newid=201&name=web-1&full=1&storage=local-lvm"))
            .andRespond(json("{\"data\":\"" + UPID + "\"}"));
        this.server.expect(requestTo(API + "/nodes/pve1/lxc/9001/clone"))
            .andExpect(content().string("newid=202&hostname=ct-1"))
            .andRespond(json("{\"data\":\"" + UPID + "\"}"));
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/201/config"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().formDataContains(Map.of("onboot", "0", "delete", "ide2,net1")))
            .andRespond(json("{\"data\":null}"));

        Upid upid = this.client.qemu("pve1")
            .clone(9000, CloneOptions.newId(201).name("web-1").full(true).storage("local-lvm"));
        this.client.lxc("pve1").clone(9001, CloneOptions.newId(202).name("ct-1"));
        this.client.qemu("pve1").updateConfig(201, Map.of("onboot", false, "delete", List.of("ide2", "net1")));

        assertThat(upid.node()).isEqualTo("pve1");
        this.server.verify();
    }

    @Test
    void deletesWithQueryParameters() {
        this.server.expect(requestTo(API + "/nodes/pve1/qemu/201?purge=1"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(json("{\"data\":\"" + UPID + "\"}"));

        assertThat(this.client.qemu("pve1").delete(201, true)).isEqualTo(Upid.of(UPID));
    }

    @Test
    void readsStorages() {
        this.server.expect(requestTo(API + "/nodes/pve1/storage"))
            .andRespond(json("""
                    {"data":[{"storage":"local","type":"dir","content":"iso,vztmpl","active":1,"enabled":1,"shared":0,"total":100,"used":40,"avail":60}]}
                    """));
        this.server.expect(requestTo(API + "/nodes/pve1/storage/local/content?content=iso"))
            .andRespond(json("""
                    {"data":[{"volid":"local:iso/debian.iso","content":"iso","format":"iso","size":123}]}
                    """));

        List<StorageInfo> storages = this.client.storage("pve1").list();

        assertThat(storages).singleElement().satisfies(storage -> {
            assertThat(storage.active()).isTrue();
            assertThat(storage.shared()).isFalse();
        });
        assertThat(this.client.storage("pve1").content("local", "iso")).singleElement()
            .satisfies(content -> assertThat(content.volid()).isEqualTo("local:iso/debian.iso"));
    }

    @Test
    void awaitsTaskCompletion() {
        expectTaskStatus("{\"status\":\"running\"}");
        expectTaskStatus("{\"status\":\"stopped\",\"exitstatus\":\"OK\",\"type\":\"qmstart\"}");

        TaskStatus status = this.client.tasks().await(Upid.of(UPID));

        assertThat(status.isSuccessful()).isTrue();
        this.server.verify();
    }

    @Test
    void failsOnUnsuccessfulTask() {
        expectTaskStatus("{\"status\":\"stopped\",\"exitstatus\":\"can't lock file '/var/lock/qemu-server/lock-100.conf'\"}");

        assertThatExceptionOfType(ProxmoxTaskException.class)
            .isThrownBy(() -> this.client.tasks().await(Upid.of(UPID)))
            .withMessageContaining("can't lock file")
            .satisfies(ex -> assertThat(ex.getStatus().exitstatus()).startsWith("can't lock"));
    }

    @Test
    void treatsWarningsAsSuccess() {
        expectTaskStatus("{\"status\":\"stopped\",\"exitstatus\":\"WARNINGS: 1\"}");

        assertThat(this.client.tasks().await(Upid.of(UPID)).isSuccessful()).isTrue();
    }

    @Test
    void timesOutWaitingForTask() {
        expectTaskStatus("{\"status\":\"running\"}");

        assertThatExceptionOfType(ProxmoxTaskException.class)
            .isThrownBy(() -> this.client.tasks().await(Upid.of(UPID), Duration.ZERO))
            .withMessageContaining("still running");
    }

    @Test
    void normalizesBaseUrl() {
        assertThat(ProxmoxClient.Builder.apiUrl("https://pve:8006")).isEqualTo("https://pve:8006/api2/json");
        assertThat(ProxmoxClient.Builder.apiUrl("https://pve:8006/api2/json/")).isEqualTo("https://pve:8006/api2/json");
    }

    private void expectTaskStatus(String status) {
        String encodedUpid = UPID.replace(":", "%3A").replace("@", "%40").replace("!", "%21");
        this.server.expect(requestTo(API + "/nodes/pve1/tasks/" + encodedUpid + "/status"))
            .andRespond(json("{\"data\":" + status + "}"));
    }

    private static org.springframework.test.web.client.ResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }
}
