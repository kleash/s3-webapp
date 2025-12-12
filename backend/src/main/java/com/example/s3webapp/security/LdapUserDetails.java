package com.example.s3webapp.security;

import java.util.List;

public record LdapUserDetails(String username, String dn, List<String> memberOf, AccessLevel accessLevel) {
    public LdapUserDetails {
        memberOf = memberOf == null ? List.of() : List.copyOf(memberOf);
    }
}
