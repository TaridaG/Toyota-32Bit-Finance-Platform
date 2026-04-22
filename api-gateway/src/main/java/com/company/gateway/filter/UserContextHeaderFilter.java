package com.company.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -900;

    public static final String HDR_USER_ID = "X-USER-ID";
    public static final String HDR_USERNAME = "X-USERNAME";
    public static final String HDR_USER_ROLES = "X-USER-ROLES";
    public static final String HDR_USER_EMAIL = "X-USER-EMAIL";

    private final String userIdClaim;
    private final String usernameClaim;
    private final String rolesClaim;
    private final String rolesPath;
    private final String emailClaim;

    public UserContextHeaderFilter(
            @Value("${gateway.user-context.user-id-claim:sub}") String userIdClaim,
            @Value("${gateway.user-context.roles-claim:roles}") String rolesClaim,
            @Value("${gateway.user-context.username-claim:preferred_username}") String usernameClaim,
            @Value("${gateway.user-context.roles-path:realm_access.roles}") String rolesPath,
            @Value("${gateway.user-context.email-claim:email}") String emailClaim
    ) {
        this.userIdClaim = userIdClaim;
        this.usernameClaim = usernameClaim;
        this.rolesClaim = rolesClaim;
        this.rolesPath = rolesPath;
        this.emailClaim = emailClaim;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest stripped = stripIncomingTrustHeaders(exchange.getRequest());

        return exchange.getPrincipal()
                .flatMap(principal -> {
                    if (!(principal instanceof JwtAuthenticationToken jwtAuth)) {
                        return Mono.just(stripped);
                    }
                    return Mono.just(applyTrustedJwtHeaders(stripped, jwtAuth));
                })
                .switchIfEmpty(Mono.just(stripped))
                .flatMap(req -> chain.filter(exchange.mutate().request(req).build()));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static ServerHttpRequest stripIncomingTrustHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(h -> {
                    h.remove(HDR_USER_ID);
                    h.remove(HDR_USERNAME);
                    h.remove(HDR_USER_ROLES);
                    h.remove(HDR_USER_EMAIL);
                })
                .build();
    }

    private ServerHttpRequest applyTrustedJwtHeaders(ServerHttpRequest stripped, JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();
        String userId = readStringClaim(jwt, userIdClaim);
        String usernameRaw = readStringClaim(jwt, usernameClaim);
        final String username = (usernameRaw == null || usernameRaw.isBlank()) ? userId : usernameRaw;
        if (userId == null || userId.isBlank()) {
            return stripped;
        }

        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(readRolesFromSimpleClaim(jwt, rolesClaim));
        roles.addAll(readRolesFromPath(jwt, rolesPath));

        final String rolesHeader = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(","));

        final String email = readStringClaim(jwt, emailClaim);
        final String userIdFinal = userId;

        return stripped.mutate()
                .headers(h -> {
                    h.set(HDR_USER_ID, userIdFinal);
                    h.set(HDR_USERNAME, username != null ? username : "");
                    if (rolesHeader != null && !rolesHeader.isBlank()) {
                        h.set(HDR_USER_ROLES, rolesHeader);
                    } else {
                        h.remove(HDR_USER_ROLES);
                    }
                    if (email != null && !email.isBlank()) {
                        h.set(HDR_USER_EMAIL, email);
                    } else {
                        h.remove(HDR_USER_EMAIL);
                    }
                })
                .build();
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
            if (!(current instanceof Map<?, ?> map)) {
                return Collections.emptySet();
            }
            current = map.get(p);
            if (current == null) {
                return Collections.emptySet();
            }
        }
        if (current instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Collections.emptySet();
    }
}
