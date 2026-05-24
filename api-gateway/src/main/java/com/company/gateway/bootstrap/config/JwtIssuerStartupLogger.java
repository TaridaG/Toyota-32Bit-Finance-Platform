package com.company.gateway.bootstrap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup sırasında yapılandırılmış JWT issuer URI'sini loglar (operasyonel doğrulama).
 */
@Component
public class JwtIssuerStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(JwtIssuerStartupLogger.class);

    private final String issuerUri;

    /** {@code app.security.jwt.issuer-uri} property'sini enjekte eder. */
    public JwtIssuerStartupLogger(@Value("${app.security.jwt.issuer-uri}") String issuerUri) {
        this.issuerUri = issuerUri;
    }

    /** Application ready olduğunda aktif JWT issuer bilgisini INFO seviyesinde yazar. */
    @EventListener(ApplicationReadyEvent.class)
    public void logIssuer() {
        log.info("Using JWT issuer: {}", issuerUri);
    }
}
