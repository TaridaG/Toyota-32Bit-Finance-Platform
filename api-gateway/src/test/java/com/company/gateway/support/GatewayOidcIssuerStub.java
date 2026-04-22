package com.company.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

public final class GatewayOidcIssuerStub {

    private static MockWebServer server;
    private static RSAKey jwtSigningKey;
    private static String registeredIssuer;

    private GatewayOidcIssuerStub() {
    }

    public static synchronized void registerIssuerUri(DynamicPropertyRegistry registry) throws Exception {
        if (server == null) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            RSAKey rsaJwk = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID("gateway-test-key")
                    .build();
            jwtSigningKey = rsaJwk;

            server = new MockWebServer();
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath();
                    if (path != null && path.endsWith("/.well-known/openid-configuration")) {
                        int port = server.getPort();
                        String issuer = registeredIssuer != null
                                ? registeredIssuer
                                : ("http://127.0.0.1:" + port + "/realms/finance");
                        String jwksUri = "http://127.0.0.1:" + port + "/realms/finance/protocol/openid-connect/certs";
                        String body = "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + jwksUri + "\"}";
                        return new MockResponse()
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json")
                                .setBody(body);
                    }
                    if (path != null && path.endsWith("/protocol/openid-connect/certs")) {
                        try {
                            String jwks = new JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
                            return new MockResponse()
                                    .setResponseCode(200)
                                    .addHeader("Content-Type", "application/json")
                                    .setBody(jwks);
                        } catch (Exception e) {
                            return new MockResponse().setResponseCode(500);
                        }
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
            server.start();
            registeredIssuer = canonicalIssuerForServer();
        }
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> registeredIssuer);
        registry.add("app.security.jwt.issuer-uri", () -> registeredIssuer);
    }

    public static String mintAccessToken(Consumer<JWTClaimsSet.Builder> customizer) throws JOSEException {
        if (jwtSigningKey == null || server == null) {
            throw new IllegalStateException("registerIssuerUri must run (via @DynamicPropertySource) before minting JWTs");
        }
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(registeredIssuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .jwtID(UUID.randomUUID().toString());
        customizer.accept(builder);
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwtSigningKey.getKeyID()).build(),
                builder.build());
        jwt.sign(new RSASSASigner(jwtSigningKey.toRSAPrivateKey()));
        return jwt.serialize();
    }

    public static String issuerUri() {
        if (registeredIssuer != null) {
            return registeredIssuer;
        }
        if (server == null) {
            throw new IllegalStateException("Issuer stub not started");
        }
        return canonicalIssuerForServer();
    }

    private static String canonicalIssuerForServer() {
        int port = server.getPort();
        return "http://127.0.0.1:" + port + "/realms/finance";
    }

    public static void shutdown() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
            jwtSigningKey = null;
            registeredIssuer = null;
        }
    }
}
