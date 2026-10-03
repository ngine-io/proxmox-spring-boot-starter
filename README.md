# Proxmox Spring Boot Starter

Typed [Proxmox VE](https://pve.proxmox.com/pve-docs/api-viewer/) API client built on Spring's `RestClient`, with Spring Boot auto-configuration. Licensed under Apache-2.0.

## Usage

```xml
<dependency>
    <groupId>io.ngine.proxmox</groupId>
    <artifactId>proxmox-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
proxmox:
  url: https://pve.example.com:8006
  token-id: automation@pve!ci        # or username/password (+ realm, default pam)
  token-secret: ${PVE_TOKEN_SECRET}
  ssl:
    bundle: pve                      # trust /etc/pve/pve-root-ca.pem via spring.ssl.bundle.pem.pve
spring:
  ssl:
    bundle:
      pem:
        pve:
          truststore:
            certificate: classpath:pve-root-ca.pem
```

```java
@Service
class Provisioner {

    private final ProxmoxClient proxmox;

    Provisioner(ProxmoxClient proxmox) {
        this.proxmox = proxmox;
    }

    int provision(String node, int templateId, String name) {
        int vmid = proxmox.cluster().nextId();
        QemuApi qemu = proxmox.qemu(node);
        proxmox.tasks().await(qemu.clone(templateId, CloneOptions.newId(vmid).name(name).full(true)));
        qemu.updateConfig(vmid, Map.of("cores", 4, "memory", 8192, "onboot", true));
        proxmox.tasks().await(qemu.start(vmid));
        return vmid;
    }
}
```

The client also works without Spring Boot: `ProxmoxClient.builder()`.

## Coverage

| Area | Operations |
|---|---|
| `nodes()` | list, status |
| `cluster()` | resources (optionally by type), next free VMID, check VMID |
| `qemu(node)` / `lxc(node)` | list, status, config get/update, start, stop, shutdown, reboot, clone, delete (+ `reset` for QEMU) |
| `storage(node)` | list, content |
| `tasks()` | status, await (polls until finished, fails on error or timeout) |

API errors raise `ProxmoxApiException` with Proxmox's message and per-parameter `errors`; failed tasks raise `ProxmoxTaskException`.

## Configuration

| Property | Default | |
|---|---|---|
| `proxmox.url` | | enables the auto-configuration |
| `proxmox.token-id` / `proxmox.token-secret` | | API token auth (preferred) |
| `proxmox.username` / `proxmox.password` / `proxmox.realm` | `pam` | ticket auth with automatic renewal |
| `proxmox.ssl.bundle` | | SSL bundle trusting the PVE certificate |
| `proxmox.ssl.insecure` | `false` | trust any certificate (development only) |
| `proxmox.connect-timeout` / `proxmox.read-timeout` | `5s` / `60s` | |
| `proxmox.task.poll-interval` / `proxmox.task.timeout` | `1s` / `10m` | used by `tasks().await(...)` |

With actuator on the classpath, a `proxmox` health indicator calls `GET /version` (disable with `management.health.proxmox.enabled=false`).

## Build

```sh
./mvnw verify
```

`LiveProxmoxTests` runs against a real cluster when `PVE_URL`, `PVE_TOKEN_ID` and `PVE_TOKEN_SECRET` are set (`PVE_INSECURE=true` for self-signed certificates). Setting `PVE_NODE` and `PVE_TEMPLATE_VMID` also enables a test that clones, starts, stops and deletes a VM.
