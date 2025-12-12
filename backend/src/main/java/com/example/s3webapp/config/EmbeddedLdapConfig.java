package com.example.s3webapp.config;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "security.ldap.embedded", name = "enabled", havingValue = "true")
public class EmbeddedLdapConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedLdapConfig.class);

    private final SecurityProperties securityProperties;
    private final ResourceLoader resourceLoader;
    private InMemoryDirectoryServer server;

    public EmbeddedLdapConfig(SecurityProperties securityProperties, ResourceLoader resourceLoader) {
        this.securityProperties = securityProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void start() throws Exception {
        var ldap = securityProperties.ldap();
        var embedded = ldap.embedded();
        String baseDn = embedded.baseDn() != null ? embedded.baseDn() : ldap.baseDn();
        if (ldap.bindDn() == null || ldap.bindPassword() == null) {
            throw new IllegalStateException("security.ldap.bindDn and bindPassword must be configured for embedded LDAP");
        }
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDn);
        config.setSchema(null);
        config.addAdditionalBindCredentials(ldap.bindDn(), ldap.bindPassword());
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", embedded.port()));
        server = new InMemoryDirectoryServer(config);
        String seedPath = resolve(embedded.seedLdif());
        server.importFromLDIF(true, seedPath);
        server.startListening();
        log.info("Started embedded LDAP on ldap://localhost:{} with base {}", embedded.port(), baseDn);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutDown(true);
        }
    }

    private String resolve(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (resource.isFile()) {
            return resource.getFile().getAbsolutePath();
        }
        java.io.File temp = java.io.File.createTempFile("ldap-seed", ".ldif");
        try (var in = resource.getInputStream(); var out = new java.io.FileOutputStream(temp)) {
            in.transferTo(out);
        }
        temp.deleteOnExit();
        return temp.getAbsolutePath();
    }
}
