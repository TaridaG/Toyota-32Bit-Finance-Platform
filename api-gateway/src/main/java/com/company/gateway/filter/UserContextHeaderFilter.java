package com.company.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> mutateRequestWithUser(auth, exchange, chain));
    }

    private Mono<Void> mutateRequestWithUser(Authentication auth,
                                             org.springframework.web.server.ServerWebExchange exchange,
                                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return chain.filter(exchange);
        }

        String userId = jwtAuth.getToken().getSubject(); // sub
        String roles = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        var mutatedRequest = exchange.getRequest().mutate()
                .header("X-USER-ID", userId)
                .header("X-ROLES", roles)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -800; // correlation(-1000), logging(-900) sonra
    }
}