package com.company.gateway.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class KeycloakJwtGrantedAuthoritiesExtractor {

    private KeycloakJwtGrantedAuthoritiesExtractor() {
    }

    public static Collection<GrantedAuthority> extract(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        addRealmRoles(jwt, roles);
        addTopLevelRoles(jwt, roles);
        addResourceClientRoles(jwt, roles);
        addRolesFromScope(jwt, roles);
        return roles.stream()
                .map(KeycloakJwtGrantedAuthoritiesExtractor::toAuthorityName)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String toAuthorityName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String r = raw.trim();
        if (r.startsWith("ROLE_")) {
            String suffix = r.substring(5).trim();
            if (suffix.isEmpty()) {
                return "";
            }
            return "ROLE_" + suffix.toUpperCase(Locale.ROOT);
        }
        return "ROLE_" + r.toUpperCase(Locale.ROOT);
    }

    private static void addRealmRoles(Jwt jwt, Set<String> roles) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> ra)) {
            return;
        }
        Object list = ra.get("roles");
        if (list instanceof Collection<?> col) {
            col.stream().filter(String.class::isInstance).map(String.class::cast).forEach(roles::add);
        }
    }

    private static void addTopLevelRoles(Jwt jwt, Set<String> roles) {
        Object v = jwt.getClaims().get("roles");
        if (v instanceof Collection<?> col) {
            col.stream().map(String::valueOf).forEach(roles::add);
        } else if (v instanceof String s && !s.isBlank()) {
            if (s.contains(",")) {
                for (String p : s.split(",")) {
                    if (!p.isBlank()) {
                        roles.add(p.trim());
                    }
                }
            } else {
                roles.add(s.trim());
            }
        }
    }

    private static void addResourceClientRoles(Jwt jwt, Set<String> roles) {
        Object ra = jwt.getClaims().get("resource_access");
        if (!(ra instanceof Map<?, ?> clients)) {
            return;
        }
        for (Object clientVal : clients.values()) {
            if (!(clientVal instanceof Map<?, ?> clientMap)) {
                continue;
            }
            Object list = clientMap.get("roles");
            if (list instanceof Collection<?> col) {
                col.stream().map(String::valueOf).forEach(roles::add);
            }
        }
    }

    private static void addRolesFromScope(Jwt jwt, Set<String> roles) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            Object v = jwt.getClaims().get("scope");
            if (v instanceof Collection<?> col) {
                for (Object o : col) {
                    String p = String.valueOf(o).trim();
                    if (!p.isBlank()) {
                        roles.add(p);
                    }
                }
                return;
            }
            if (v != null) {
                scope = String.valueOf(v).trim();
            }
        }
        if (scope == null || scope.isBlank()) {
            return;
        }
        Set<String> skip = Set.of(
                "openid", "profile", "email", "address", "phone", "offline_access"
        );
        for (String part : scope.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (skip.contains(part.toLowerCase(Locale.ROOT))) {
                continue;
            }
            roles.add(normalizeScopeToken(part));
        }
    }

    private static String normalizeScopeToken(String part) {
        String p = part.trim();
        if (p.length() > 6 && p.regionMatches(true, 0, "SCOPE_", 0, 6)) {
            String rest = p.substring(6).trim();
            return rest.isEmpty() ? p : rest;
        }
        return p;
    }
}
