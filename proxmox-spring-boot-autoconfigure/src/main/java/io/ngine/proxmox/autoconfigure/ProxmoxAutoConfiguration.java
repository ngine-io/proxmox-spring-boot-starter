package io.ngine.proxmox.autoconfigure;

import io.ngine.proxmox.client.InsecureClientHttpRequestFactory;
import io.ngine.proxmox.client.ProxmoxClient;
import io.ngine.proxmox.client.ProxmoxCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures a {@link ProxmoxClient} when {@code proxmox.url} is set.
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SslAutoConfiguration.class })
@ConditionalOnClass(ProxmoxClient.class)
@ConditionalOnProperty(prefix = "proxmox", name = "url")
@EnableConfigurationProperties(ProxmoxProperties.class)
public class ProxmoxAutoConfiguration {

    private static final Log logger = LogFactory.getLog(ProxmoxAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    ProxmoxClient proxmoxClient(ProxmoxProperties properties, ObjectProvider<RestClient.Builder> restClientBuilder,
            ObjectProvider<SslBundles> sslBundles) {
        RestClient.Builder builder = restClientBuilder.getIfAvailable(RestClient::builder)
            .requestFactory(requestFactory(properties, sslBundles));
        return ProxmoxClient.builder()
            .baseUrl(properties.getUrl())
            .credentials(credentials(properties))
            .restClientBuilder(builder)
            .taskPollInterval(properties.getTask().getPollInterval())
            .taskTimeout(properties.getTask().getTimeout())
            .build();
    }

    static ProxmoxCredentials credentials(ProxmoxProperties properties) {
        if (StringUtils.hasText(properties.getTokenId())) {
            requireText(properties.getTokenSecret(), "proxmox.token-secret");
            return new ProxmoxCredentials.ApiToken(properties.getTokenId(), properties.getTokenSecret());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            requireText(properties.getPassword(), "proxmox.password");
            String username = properties.getUsername().contains("@") ? properties.getUsername()
                    : properties.getUsername() + "@" + properties.getRealm();
            return new ProxmoxCredentials.Password(username, properties.getPassword());
        }
        throw new IllegalStateException(
                "No Proxmox credentials configured: set proxmox.token-id and proxmox.token-secret "
                        + "or proxmox.username and proxmox.password");
    }

    private static void requireText(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(property + " must be set");
        }
    }

    private static ClientHttpRequestFactory requestFactory(ProxmoxProperties properties,
            ObjectProvider<SslBundles> sslBundles) {
        if (properties.getSsl().isInsecure()) {
            logger.warn("TLS certificate and host name verification is disabled for Proxmox at "
                    + properties.getUrl() + " (proxmox.ssl.insecure=true); do not use this in production");
            InsecureClientHttpRequestFactory factory = new InsecureClientHttpRequestFactory();
            factory.setConnectTimeout(properties.getConnectTimeout());
            factory.setReadTimeout(properties.getReadTimeout());
            return factory;
        }
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
            .withTimeouts(properties.getConnectTimeout(), properties.getReadTimeout());
        String bundle = properties.getSsl().getBundle();
        if (StringUtils.hasText(bundle)) {
            settings = settings.withSslBundle(sslBundles.getObject().getBundle(bundle));
        }
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }
}
