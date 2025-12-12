package com.example.s3webapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.s3webapp.config.SecurityProperties;
import com.example.s3webapp.config.SecurityProperties.NoRolePolicy;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class LdapUserServiceTest {

    private static InMemoryDirectoryServer server;
    private static final int LDAP_PORT = 1390;

    private SecurityProperties securityProperties;
    private LdapUserService service;

    @BeforeAll
    static void startLdap() throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.setSchema(null);
        config.addAdditionalBindCredentials("cn=ldap-reader,ou=ServiceAccounts,dc=example,dc=com", "readerpass");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", LDAP_PORT));
        server = new InMemoryDirectoryServer(config);
        Path temp = Files.createTempFile("seed", ".ldif");
        try (var in = new ClassPathResource("ldap/seed.ldif").getInputStream()) {
            Files.write(temp, in.readAllBytes());
        }
        server.importFromLDIF(true, temp.toAbsolutePath().toString());
        server.startListening();
    }

    @AfterAll
    static void stopLdap() {
        if (server != null) {
            server.shutDown(true);
        }
    }

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.ldap().setUrl("ldap://localhost:" + LDAP_PORT);
        securityProperties.ldap().setBaseDn("dc=example,dc=com");
        securityProperties.ldap().setBindDn("cn=ldap-reader,ou=ServiceAccounts,dc=example,dc=com");
        securityProperties.ldap().setBindPassword("readerpass");
        securityProperties.ldap().setUserSearchBase("ou=Users,dc=example,dc=com");
        securityProperties.ldap().setUserSearchFilter("(sAMAccountName={0})");
        securityProperties.ldap().setReadOnlyGroups(
                java.util.List.of("cn=S3_ReadOnly,ou=Groups,dc=example,dc=com"));
        securityProperties.ldap().setReadWriteGroups(
                java.util.List.of("cn=S3_ReadWrite,ou=Groups,dc=example,dc=com"));
        securityProperties.ldap().setReadWriteUsers(java.util.List.of("carol"));

        LdapContextSource ctx = new LdapContextSource();
        ctx.setUrl(securityProperties.ldap().url());
        ctx.setUserDn(securityProperties.ldap().bindDn());
        ctx.setPassword(securityProperties.ldap().bindPassword());
        ctx.afterPropertiesSet();
        LdapTemplate template = new LdapTemplate(ctx);
        service = new LdapUserService(template, ctx, securityProperties);
    }

    @Test
    void authenticatesUserWithReadOnlyRole() {
        LdapUserDetails user = service.authenticate("alice", "password1");
        assertThat(user.accessLevel()).isEqualTo(AccessLevel.READ_ONLY);
        assertThat(user.memberOf()).anySatisfy(group -> assertThat(group).contains("S3_ReadOnly"));
    }

    @Test
    void failsAuthenticationWithBadPassword() {
        assertThatThrownBy(() -> service.authenticate("alice", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void resolvesReadWriteRoleFromGroup() {
        LdapUserDetails user = service.authenticate("bob", "password1");
        assertThat(user.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
    }

    @Test
    void readWriteSupersedesReadOnly() {
        LdapUserDetails user = service.authenticate("both", "password1");
        assertThat(user.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
    }

    @Test
    void usernameOverrideGrantsAccess() {
        LdapUserDetails user = service.authenticate("carol", "password1");
        assertThat(user.accessLevel()).isEqualTo(AccessLevel.READ_WRITE);
    }

    @Test
    void deniesWhenNoMatchingRoleAndPolicyIsDeny() {
        assertThatThrownBy(() -> service.authenticate("guest", "password1"))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void canFallbackToReadOnlyWhenConfigured() {
        securityProperties.ldap().setNoRolePolicy(NoRolePolicy.READ_ONLY);
        LdapUserDetails user = service.authenticate("guest", "password1");
        assertThat(user.accessLevel()).isEqualTo(AccessLevel.READ_ONLY);
    }
}
