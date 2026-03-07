package com.company.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {

    public static final String HDR_USER_ID = "X-USER-ID";
    public static final String HDR_USERNAME = "X-USERNAME";
    public static final String HDR_USER_ROLES = "X-USER-ROLES";

    private final String userIdClaim;
    private final String usernameClaim;
    private final String rolesClaim;
    private final String rolesPath; // optional: "realm_access.roles" like structure

    public UserContextHeaderFilter(
            @Value("${gateway.user-context.user-id-claim:sub}") String userIdClaim,
            @Value("${gateway.user-context.roles-claim:roles}") String rolesClaim,
            @Value("${gateway.user-context.username-claim:preferred_username}") String usernameClaim,
            @Value("${gateway.user-context.roles-path:realm_access.roles}") String rolesPath
    ) {
        this.userIdClaim = userIdClaim;
        this.usernameClaim = usernameClaim;
        this.rolesClaim = rolesClaim;
        this.rolesPath = rolesPath;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {


        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(HDR_USER_ID);
                    h.remove(HDR_USERNAME);
                    h.remove(HDR_USER_ROLES);
                })
                .build();

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> buildEnrichedOrSanitized(auth, sanitized, exchange, chain))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange.mutate().request(sanitized).build())));
    }

    private Mono<Void> buildEnrichedOrSanitized(
            Authentication auth,
            ServerHttpRequest sanitized,
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        Jwt jwt = jwtAuth.getToken();
        String userId = readStringClaim(jwt, userIdClaim);
        String username = readStringClaim(jwt, usernameClaim);
        if (username == null || username.isBlank()) {
            username = userId; // fallback
        }
        if (userId == null || userId.isBlank()) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(readRolesFromSimpleClaim(jwt, rolesClaim));
        roles.addAll(readRolesFromPath(jwt, rolesPath));

        String rolesHeader = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(","));

        ServerHttpRequest enriched = sanitized.mutate()
                .header(HDR_USER_ID, userId)
                .header(HDR_USERNAME, username)
                .headers(h -> {
                    if (!rolesHeader.isBlank()) {
                        h.set(HDR_USER_ROLES, rolesHeader);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(enriched).build());
    }

    private String readStringClaim(Jwt jwt, String claim) {
        Object v = jwt.getClaims().get(claim);
        return v == null ? null : String.valueOf(v);
    }

    private Set<String> readRolesFromSimpleClaim(Jwt jwt, String claim) {
        Object v = jwt.getClaims().get(claim);
        if (v instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (v instanceof String s && s.contains(",")) {
            return Arrays.stream(s.split(",")).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (v instanceof String s && !s.isBlank()) {
            return new LinkedHashSet<>(List.of(s.trim()));
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private Set<String> readRolesFromPath(Jwt jwt, String path) {
        String[] parts = path.split("\\.");
        Object current = jwt.getClaims();
        for (String p : parts) {
            if (!(current instanceof Map<?, ?> map)) return Collections.emptySet();
            current = map.get(p);
            if (current == null) return Collections.emptySet();
        }
        if (current instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Collections.emptySet();
    }

    @Override
    public int getOrder() {
        return -900;
    }
}