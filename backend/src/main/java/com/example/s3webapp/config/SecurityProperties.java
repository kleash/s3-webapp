package com.example.s3webapp.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private final Ldap ldap = new Ldap();

    public Ldap ldap() {
        return ldap;
    }

    public static class Ldap {
        private boolean enabled = true;
        private String url = "ldap://localhost:1389";
        private String baseDn = "dc=example,dc=com";
        private String bindDn = "cn=ldap-reader,ou=ServiceAccounts,dc=example,dc=com";
        private String bindPassword = "readerpass";
        private String userSearchBase = "ou=Users,dc=example,dc=com";
        private String userSearchFilter = "(sAMAccountName={0})";
        private List<String> readOnlyGroups = new ArrayList<>(List.of("cn=S3_ReadOnly,ou=Groups,dc=example,dc=com"));
        private List<String> readWriteGroups = new ArrayList<>(List.of("cn=S3_ReadWrite,ou=Groups,dc=example,dc=com"));
        private List<String> readOnlyUsers = new ArrayList<>();
        private List<String> readWriteUsers = new ArrayList<>();
        private boolean ignoreSslValidation = true;
        private NoRolePolicy noRolePolicy = NoRolePolicy.DENY;
        private final Embedded embedded = new Embedded();

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String url() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String baseDn() {
            return baseDn;
        }

        public void setBaseDn(String baseDn) {
            this.baseDn = baseDn;
        }

        public String bindDn() {
            return bindDn;
        }

        public void setBindDn(String bindDn) {
            this.bindDn = bindDn;
        }

        public String bindPassword() {
            return bindPassword;
        }

        public void setBindPassword(String bindPassword) {
            this.bindPassword = bindPassword;
        }

        public String userSearchBase() {
            return userSearchBase;
        }

        public void setUserSearchBase(String userSearchBase) {
            this.userSearchBase = userSearchBase;
        }

        public String userSearchFilter() {
            return userSearchFilter;
        }

        public void setUserSearchFilter(String userSearchFilter) {
            if (userSearchFilter != null && !userSearchFilter.isBlank()) {
                this.userSearchFilter = userSearchFilter;
            }
        }

        public List<String> readOnlyGroups() {
            return readOnlyGroups;
        }

        public void setReadOnlyGroups(List<String> readOnlyGroups) {
            this.readOnlyGroups = copy(readOnlyGroups);
        }

        public List<String> readWriteGroups() {
            return readWriteGroups;
        }

        public void setReadWriteGroups(List<String> readWriteGroups) {
            this.readWriteGroups = copy(readWriteGroups);
        }

        public List<String> readOnlyUsers() {
            return readOnlyUsers;
        }

        public void setReadOnlyUsers(List<String> readOnlyUsers) {
            this.readOnlyUsers = copy(readOnlyUsers);
        }

        public List<String> readWriteUsers() {
            return readWriteUsers;
        }

        public void setReadWriteUsers(List<String> readWriteUsers) {
            this.readWriteUsers = copy(readWriteUsers);
        }

        public boolean ignoreSslValidation() {
            return ignoreSslValidation;
        }

        public void setIgnoreSslValidation(boolean ignoreSslValidation) {
            this.ignoreSslValidation = ignoreSslValidation;
        }

        public NoRolePolicy noRolePolicy() {
            return noRolePolicy;
        }

        public void setNoRolePolicy(NoRolePolicy noRolePolicy) {
            if (noRolePolicy != null) {
                this.noRolePolicy = noRolePolicy;
            }
        }

        public Embedded embedded() {
            return embedded;
        }

        private List<String> copy(List<String> input) {
            return input == null ? List.of() : List.copyOf(input);
        }
    }

    public static class Embedded {
        private boolean enabled = false;
        private int port = 1389;
        private String baseDn = "dc=example,dc=com";
        private String seedLdif = "classpath:ldap/seed.ldif";

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int port() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String baseDn() {
            return baseDn;
        }

        public void setBaseDn(String baseDn) {
            this.baseDn = baseDn;
        }

        public String seedLdif() {
            return seedLdif;
        }

        public void setSeedLdif(String seedLdif) {
            this.seedLdif = seedLdif;
        }
    }

    public enum NoRolePolicy {
        DENY,
        READ_ONLY
    }
}
