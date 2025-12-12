package com.example.s3webapp.security;

import com.example.s3webapp.config.SecurityProperties;
import com.example.s3webapp.config.SecurityProperties.NoRolePolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.springframework.ldap.query.LdapQueryBuilder.query;
import org.springframework.ldap.query.SearchScope;

@Service
public class LdapUserService {

    private static final Logger log = LoggerFactory.getLogger(LdapUserService.class);

    private final LdapTemplate ldapTemplate;
    private final LdapContextSource contextSource;
    private final SecurityProperties securityProperties;

    public LdapUserService(LdapTemplate ldapTemplate, LdapContextSource contextSource, SecurityProperties securityProperties) {
        this.ldapTemplate = ldapTemplate;
        this.contextSource = contextSource;
        this.securityProperties = securityProperties;
    }

    public LdapUserDetails authenticate(String username, String password) throws AuthenticationException {
        if (password == null || password.isBlank()) {
            throw new BadCredentialsException("Password is required");
        }
        LdapUserDetails user = loadUser(username);
        try {
            contextSource.getContext(user.dn(), password).close();
            return user;
        } catch (Exception ex) {
            log.debug("LDAP bind failed for {}", user.dn(), ex);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    public LdapUserDetails loadUser(String username) {
        var ldapProps = securityProperties.ldap();
        if (!ldapProps.enabled()) {
            throw new InsufficientAuthenticationException("LDAP is disabled");
        }
        validateConfig(ldapProps);
        List<LdapUserDetails> matches = ldapTemplate.search(
                query().base(ldapProps.userSearchBase()).searchScope(SearchScope.SUBTREE).filter(ldapProps.userSearchFilter(), username),
                mapUser());
        if (matches.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        LdapUserDetails found = matches.get(0);
        AccessLevel accessLevel = resolveAccess(found.username(), found.memberOf(), ldapProps);
        return new LdapUserDetails(found.username(), found.dn(), found.memberOf(), accessLevel);
    }

    private ContextMapper<LdapUserDetails> mapUser() {
        return ctx -> {
            DirContextAdapter adapter = (DirContextAdapter) ctx;
            var attrs = adapter.getAttributes();
            String dn = adapter.getDn() != null ? adapter.getDn().toString() : "";

            List<String> memberOf = new ArrayList<>();
            if (attrs.get("memberOf") != null) {
                var values = attrs.get("memberOf").getAll();
                while (values.hasMore()) {
                    Object next = values.next();
                    if (next != null) {
                        memberOf.add(next.toString());
                    }
                }
            }
            String sam = attrs.get("sAMAccountName") != null ? attrs.get("sAMAccountName").get().toString() : null;
            String uid = sam != null ? sam : (attrs.get("uid") != null ? attrs.get("uid").get().toString() : null);
            return new LdapUserDetails(uid, dn, memberOf, null);
        };
    }

    private AccessLevel resolveAccess(String username, List<String> memberOf, SecurityProperties.Ldap ldapProps) {
        Set<String> normalizedGroups = new HashSet<>();
        if (memberOf != null) {
            memberOf.forEach(g -> normalizedGroups.add(g.toLowerCase()));
        }
        boolean readWrite = intersects(normalizedGroups, ldapProps.readWriteGroups())
                || containsIgnoreCase(ldapProps.readWriteUsers(), username);
        boolean readOnly = intersects(normalizedGroups, ldapProps.readOnlyGroups())
                || containsIgnoreCase(ldapProps.readOnlyUsers(), username);

        if (readWrite) {
            return AccessLevel.READ_WRITE;
        }
        if (readOnly) {
            return AccessLevel.READ_ONLY;
        }
        if (ldapProps.noRolePolicy() == NoRolePolicy.READ_ONLY) {
            return AccessLevel.READ_ONLY;
        }
        throw new InsufficientAuthenticationException("User is not authorized for this application");
    }

    private boolean intersects(Set<String> normalizedGroups, List<String> configuredGroups) {
        if (configuredGroups == null || configuredGroups.isEmpty()) return false;
        for (String group : configuredGroups) {
            if (normalizedGroups.contains(group.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(List<String> candidates, String value) {
        if (value == null || candidates == null) return false;
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void validateConfig(SecurityProperties.Ldap ldapProps) {
        if (ldapProps.url() == null || ldapProps.url().isBlank()) {
            throw new IllegalStateException("security.ldap.url must be configured");
        }
        if (ldapProps.userSearchBase() == null || ldapProps.userSearchBase().isBlank()) {
            throw new IllegalStateException("security.ldap.userSearchBase must be configured");
        }
        if (ldapProps.bindDn() == null || ldapProps.bindDn().isBlank()) {
            throw new IllegalStateException("security.ldap.bindDn must be configured");
        }
        if (ldapProps.bindPassword() == null) {
            throw new IllegalStateException("security.ldap.bindPassword must be configured");
        }
    }
}
