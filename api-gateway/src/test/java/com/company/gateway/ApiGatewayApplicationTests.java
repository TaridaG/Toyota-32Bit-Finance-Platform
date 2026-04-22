package com.company.gateway;

import com.company.gateway.support.GatewayOidcIssuerStub;
import com.company.gateway.support.NoopRedisRateLimiterTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(NoopRedisRateLimiterTestConfig.class)
class ApiGatewayApplicationTests {

    @DynamicPropertySource
    static void oidcStub(DynamicPropertyRegistry r) throws Exception {
        GatewayOidcIssuerStub.registerIssuerUri(r);
    }

    @Test
    void contextLoads() {
    }

}
