package io.ngine.proxmox.autoconfigure;

import io.ngine.proxmox.client.ProxmoxClient;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers a {@link ProxmoxHealthIndicator} when actuator is present.
 */
@AutoConfiguration(after = ProxmoxAutoConfiguration.class)
@ConditionalOnClass(name = { "io.ngine.proxmox.client.ProxmoxClient",
        "org.springframework.boot.actuate.health.HealthIndicator" })
@ConditionalOnBean(ProxmoxClient.class)
@ConditionalOnEnabledHealthIndicator("proxmox")
public class ProxmoxHealthContributorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "proxmoxHealthIndicator")
    ProxmoxHealthIndicator proxmoxHealthIndicator(ProxmoxClient client) {
        return new ProxmoxHealthIndicator(client);
    }
}
